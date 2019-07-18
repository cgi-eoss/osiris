package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerType;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.OsirisFile.Type;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;
import com.cgi.eoss.osiris.model.internal.InternalModelUtils;
import com.cgi.eoss.osiris.persistence.service.CollectionDataService;
import com.cgi.eoss.osiris.persistence.service.IncidentProcessingDataService;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.OsirisJobResponse;
import com.cgi.eoss.osiris.rpc.OsirisServiceParams;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link Incident}s. Offers additional functionality over
 * the standard CRUD-style {@link IncidentsApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/incidents")
@Log4j2
public class IncidentsApiExtension {

    private static final String COLLECTION_INPUT = "collection";
    private static final String GEOSERVER_INPUT = "geoServerSpec";

    private final OsirisSecurityService osirisSecurityService;
    private final LocalServiceLauncher localServiceLauncher;
    private final SystematicProcessingDataService systematicProcessingDataService;
    private final CollectionDataService collectionDataService;
    private final JobDataService jobDataService;
    private final IncidentProcessingDataService incidentProcessingDataService;
    private final CatalogueService catalogueService;
    private final GeoserverService geoserverService; 
    private final ObjectMapper objectMapper;

    @Autowired
    public IncidentsApiExtension(OsirisSecurityService osirisSecurityService, LocalServiceLauncher localServiceLauncher, SystematicProcessingDataService systematicProcessingDataService, 
    		CollectionDataService collectionDataService, JobDataService jobDataService, IncidentProcessingDataService incidentProcessingDataService, ObjectMapper objectMapper, 
    		GeoserverService geoserverService, CatalogueService catalogueService) {
        this.osirisSecurityService = osirisSecurityService;
        this.localServiceLauncher = localServiceLauncher;
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.collectionDataService = collectionDataService;
        this.jobDataService = jobDataService;
        this.incidentProcessingDataService = incidentProcessingDataService;
        this.geoserverService = geoserverService;
        this.catalogueService = catalogueService;
        this.objectMapper = objectMapper;
    }

    /**
     * <p>Creates and launches Systematic Processing activities for any unprocessed
     * {@link com.cgi.eoss.osiris.model.IncidentProcessing} associated with this incident.</p>
     */
    @PostMapping("/{incidentId}/process")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#incident, 'write')")
    public ResponseEntity<Resource<Job>> process(@ModelAttribute("incidentId") Incident incident) {
        try {
            for (IncidentProcessing incidentProcessing : incident.getIncidentProcessings()) {
                launchProcessing(incidentProcessing);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOG.error("Exception thrown during processing of Incident {}", incident.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void launchProcessing(IncidentProcessing incidentProcessing) throws JsonProcessingException, InterruptedException {
        LOG.debug("Launching Systematic Processing for Incident Processing {}", incidentProcessing.getId());
        Multimap<String, String> inputs = mergeReplace(incidentProcessing.getTemplate().getFixedInputs(), incidentProcessing.getInputs());

        inputs = substitutePlaceholders(incidentProcessing.getIncident(), inputs); 
        
        Collection collection = new Collection(incidentProcessing.getIncident().getTitle() + "-" + incidentProcessing.getId(),
                incidentProcessing.getOwner());
        collection.setIdentifier("osiris" + UUID.randomUUID().toString().replaceAll("-", ""));
        collection.setFileType(Type.OUTPUT_PRODUCT);
        try {
        	catalogueService.createCollection(collection);
        }
        catch( IOException e) {
            LOG.error("Failed to create underlying output collection {} for incident processing {}", collection, incidentProcessing.getId());
            throw new RuntimeException("Failed to create underlying output collection", e);
        }
        collectionDataService.save(collection);
        incidentProcessing.setCollection(collection);
        
        // Associate all outputs of this processing with the newly-created collection
        Map<String, String> outputCollectionsMap = incidentProcessing.getTemplate().getService().getServiceDescriptor().getDataOutputs().stream()
                .map(OsirisServiceDescriptor.Parameter::getId)
                .collect(Collectors.toMap(Function.identity(), s -> collection.getIdentifier()));

        inputs.replaceValues(COLLECTION_INPUT, Collections.singletonList(objectMapper.writeValueAsString(outputCollectionsMap)));

        Map<String, Object> geoserverSpecMap = incidentProcessing.getTemplate().getService().getServiceDescriptor().getDataOutputs().stream()
        		.filter(this::isGeoserverIngestible)
                .collect(Collectors.toMap(OsirisServiceDescriptor.Parameter::getId, p-> prepareGeoserverForOutput(incidentProcessing, p)));

        inputs.replaceValues(GEOSERVER_INPUT, Collections.singletonList(objectMapper.writeValueAsString(geoserverSpecMap)));
        
        //Here we launch a job or a systematic processing 
        if (!Strings.isNullOrEmpty(incidentProcessing.getTemplate().getCronExpression()) || !incidentProcessing.getTemplate().getSearchParameters().isEmpty()) {
        	launchSystematicIncidentProcessing(incidentProcessing, inputs);
        }
        else {
        	launchSingleJobIncidentProcessing(incidentProcessing, inputs);
        }
        incidentProcessingDataService.save(incidentProcessing);
    }
	
	private void launchSingleJobIncidentProcessing(IncidentProcessing incidentProcessing,
			Multimap<String, String> inputs) throws InterruptedException {
		OsirisServiceParams.Builder serviceParamsBuilder = OsirisServiceParams.newBuilder()
                .setJobId(UUID.randomUUID().toString())
                .setUserId(osirisSecurityService.getCurrentUser().getName())
                .setServiceId(incidentProcessing.getTemplate().getService().getName())
                .addAllInputs(GrpcUtil.mapToParams(inputs));

        OsirisServiceParams serviceParams = serviceParamsBuilder.build();

        LOG.info("Launching service via REST API: {}", serviceParams);

        final CountDownLatch latch = new CountDownLatch(1);
        JobLaunchObserver responseObserver = new JobLaunchObserver(latch);
        localServiceLauncher.asyncSubmitJob(serviceParams, responseObserver);

        // Block until the latch counts down (i.e. one message from the server
        try {
			if (!latch.await(1, TimeUnit.MINUTES)) {
				throw new RuntimeException("Error waiting for job submission"); 
			}
		} catch (InterruptedException e) {
			LOG.error("Launch of incident processing interrupted");
			throw e;
		}
        Job job = jobDataService.getById(responseObserver.getIntJobId());
        incidentProcessing.setJob(job);
	}

	private void launchSystematicIncidentProcessing(IncidentProcessing incidentProcessing,
			Multimap<String, String> inputs) {
		SystematicProcessingRequest.Builder grpcRequestBuilder = SystematicProcessingRequest.newBuilder()
                .setUserId(osirisSecurityService.getCurrentUser().getName())
                .setServiceId(incidentProcessing.getTemplate().getService().getName())
                .addAllInput(GrpcUtil.mapToParams(inputs))
                .setSystematicParameter(incidentProcessing.getTemplate().getSystematicInput());
        if (!Strings.isNullOrEmpty(incidentProcessing.getTemplate().getCronExpression())) {
            grpcRequestBuilder.setCronExpression(incidentProcessing.getTemplate().getCronExpression());
        }
        else {
            ListMultimap<String, String> searchParameters = (ListMultimap<String, String>) mergeReplace(incidentProcessing.getTemplate().getSearchParameters(),
                            incidentProcessing.getSearchParameters());
                    // TODO: Allow the AOI and date range to be modified in some way here?
                    searchParameters.put("aoi", incidentProcessing.getIncident().getAoi());
                    searchParameters.put("productDateStart", incidentProcessing.getIncident().getStartDate().toString());
                    searchParameters.put("productDateEnd", incidentProcessing.getIncident().getEndDate().toString());
            grpcRequestBuilder.addAllSearchParameter(GrpcUtil.mapToParams(searchParameters));
        }  
        
        
        long systematicProcessingId = localServiceLauncher.launchSystematicProcessing(grpcRequestBuilder.build()).getSystematicProcessingId();

        LOG.info("Associated Systematic Processing {} with Incident Processing {}", systematicProcessingId, incidentProcessing.getId());
        incidentProcessing.setSystematicProcessing(systematicProcessingDataService.getById(systematicProcessingId));
        incidentProcessingDataService.save(incidentProcessing);
    }

   

	private boolean isGeoserverIngestible(Parameter p) {
		if (p.getPlatformMetadata() != null && p.getPlatformMetadata().containsKey("format")) {
				switch(p.getPlatformMetadata().get("format")) {
				case "GEOTIFF": return true;
				case "SHAPEFILE" : return true;
				default: return false;
			}
		}
		return false;
	}

	private GeoServerSpec prepareGeoserverForOutput(IncidentProcessing incidentProcessing, Parameter p) {
		switch(p.getPlatformMetadata().get("format")) {
		case "GEOTIFF": 
			return prepareGeoserverMosaic(incidentProcessing, p);
		case "SHAPEFILE" : 
			return getShapefileSpec(incidentProcessing);
		default:
			throw new IllegalArgumentException("Unrecognized format for parameter " + p.getTitle());
		}
		
	}

	private GeoServerSpec getShapefileSpec(IncidentProcessing incidentProcessing) {
		return GeoServerSpec.builder()
				.geoserverType(GeoServerType.SHAPEFILE_POSTGIS_IMPORT)
				.layerName("incident_"+ incidentProcessing.getIncident().getId() + "_processing_" + incidentProcessing.getId())
			    .build();
	}

	private GeoServerSpec prepareGeoserverMosaic(IncidentProcessing incidentProcessing, Parameter p) {
		String workspace = "incident" + incidentProcessing.getIncident().getId();
		String storeName = "processing" + incidentProcessing.getId();
		geoserverService.createEmptyMosaic(workspace, storeName, p.getId(), InternalModelUtils.platformTimeRegexpToGeoserverTimeRegexp(p.getTimeRegexp()));
		return GeoServerSpec.builder()
				.geoserverType(GeoServerType.MOSAIC)
				.workspace(workspace)
			    .datastoreName(storeName)
			    .coverageName(p.getId())
			    .build();
	}
		

	private Multimap<String, String> mergeReplace(Multimap<String, String> initialMap, Multimap<String, String> replacingMap) {
        ListMultimap<String, String> replacedMap = ArrayListMultimap.create(initialMap);
        for (String key : replacingMap.keys()) {
            replacedMap.replaceValues(key, replacingMap.get(key));
        }
        return replacedMap;
    }
	
	private Multimap<String, String> substitutePlaceholders(Incident incident, Multimap<String, String> inputs) {
		ListMultimap<String, String> replacedMap = ArrayListMultimap.create(inputs);
		for (String key : inputs.keySet()) {
			replacedMap.replaceValues(key,
					inputs.get(key).stream().map(v -> replacePlaceholder(incident, v)).collect(Collectors.toList()));
		}
		return replacedMap;
	}

	private String replacePlaceholder(Incident incident, String value) {
		switch (value) {
		case "incident.aoi":
			return incident.getAoi();
		case "incident.start":
			return incident.getStartDate().toString();
		case "incident.end":
			return incident.getEndDate().toString();
		default:
			return value;
		}
	}
	
	
	private static final class JobLaunchObserver implements StreamObserver<OsirisJobResponse> {

        private final CountDownLatch latch;
        @Getter
        private long intJobId;

        JobLaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(OsirisJobResponse value) {
            this.intJobId = Long.parseLong(value.getJob().getIntJobId());
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }

}
