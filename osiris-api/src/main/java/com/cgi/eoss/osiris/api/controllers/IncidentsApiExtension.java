package com.cgi.eoss.osiris.api.controllers;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerType;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;
import com.cgi.eoss.osiris.model.internal.InternalModelUtils;
import com.cgi.eoss.osiris.persistence.service.CollectionDataService;
import com.cgi.eoss.osiris.persistence.service.IncidentProcessingDataService;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import lombok.extern.log4j.Log4j2;

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
    private final IncidentProcessingDataService incidentProcessingDataService;
    private final CatalogueService catalogueService;
    private final GeoserverService geoserverService; 
    private final ObjectMapper objectMapper;

    @Autowired
    public IncidentsApiExtension(OsirisSecurityService osirisSecurityService, LocalServiceLauncher localServiceLauncher, SystematicProcessingDataService systematicProcessingDataService, CollectionDataService collectionDataService, IncidentProcessingDataService incidentProcessingDataService, ObjectMapper objectMapper, GeoserverService geoserverService, CatalogueService catalogueService) {
        this.osirisSecurityService = osirisSecurityService;
        this.localServiceLauncher = localServiceLauncher;
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.collectionDataService = collectionDataService;
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

    private void launchProcessing(IncidentProcessing incidentProcessing) throws JsonProcessingException {
        LOG.debug("Launching Systematic Processing for Incident Processing {}", incidentProcessing.getId());
        Multimap<String, String> inputs = mergeReplace(incidentProcessing.getTemplate().getFixedInputs(), incidentProcessing.getInputs());

        Collection collection = new Collection(incidentProcessing.getIncident().getTitle() + "-" + incidentProcessing.getId(),
                incidentProcessing.getOwner());
        collection.setIdentifier("osiris" + UUID.randomUUID().toString().replaceAll("-", ""));
        if (!catalogueService.createOutputCollection(collection)) {
            LOG.error("Failed to create underlying output collection {} for incident processing {}", collection, incidentProcessing.getId());
            throw new RuntimeException("Failed to create underlying output collection");
        }
        collectionDataService.save(collection);
        incidentProcessing.setCollection(collection);
        
        // Associate all outputs of this processing with the newly-created collection
        Map<String, String> outputCollectionsMap = incidentProcessing.getTemplate().getService().getServiceDescriptor().getDataOutputs().stream()
                .map(OsirisServiceDescriptor.Parameter::getId)
                .collect(Collectors.toMap(Function.identity(), s -> collection.getIdentifier()));

        inputs.replaceValues(COLLECTION_INPUT, Collections.singletonList(objectMapper.writeValueAsString(outputCollectionsMap)));

        Map<String, Object> geoserverSpecMap = incidentProcessing.getTemplate().getService().getServiceDescriptor().getDataOutputs().stream()
        		.filter(p-> isGeoserverIngestible(p))
                .collect(Collectors.toMap(OsirisServiceDescriptor.Parameter::getId, p-> prepareGeoserverForOutput(incidentProcessing, incidentProcessing.getTemplate().getService(), p)));

        inputs.replaceValues(GEOSERVER_INPUT, Collections.singletonList(objectMapper.writeValueAsString(geoserverSpecMap)));
        
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

	private GeoServerSpec prepareGeoserverForOutput(IncidentProcessing incidentProcessing, OsirisService s, Parameter p) {
		switch(p.getPlatformMetadata().get("format")) {
		case "GEOTIFF": 
			return prepareGeoserverMosaic(incidentProcessing, s, p);
		case "SHAPEFILE" : 
			return getShapefileSpec(incidentProcessing, s, p);
		default:
			throw new IllegalArgumentException("Unrecognized format for parameter " + p.getTitle());
		}
		
	}

	private GeoServerSpec getShapefileSpec(IncidentProcessing incidentProcessing, OsirisService s, Parameter p) {
		return GeoServerSpec.builder()
				.geoserverType(GeoServerType.SHAPEFILE_POSTGIS_IMPORT)
				.layerName("incident1"+ incidentProcessing.getIncident().getId() + "_processing" + incidentProcessing.getId())
			    .build();
	}

	private GeoServerSpec prepareGeoserverMosaic(IncidentProcessing incidentProcessing, OsirisService s, Parameter p) {
		String workspace = "incident" + incidentProcessing.getIncident().getId();
		String storeName = "processing" + incidentProcessing.getId();
		geoserverService.createEmptyMosaic(workspace, storeName, p.getTitle(), InternalModelUtils.platformTimeRegexpToGeoserverTimeRegexp(p.getTimeRegexp()));
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

}
