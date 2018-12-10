package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceDockerBuildInfo;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;
import com.cgi.eoss.osiris.model.OsirisServiceDockerBuildInfo.Status;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceTemplateDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceTemplateFileDataService;
import com.cgi.eoss.osiris.rpc.BuildServiceParams;
import com.cgi.eoss.osiris.rpc.BuildServiceResponse;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.services.DefaultOsirisServices;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@BasePathAwareController
@RequestMapping("/services")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServicesApiExtension {

    @Data
    public class BuildStatus {
        private final Boolean needsBuild;
        private final OsirisServiceDockerBuildInfo.Status status;
    }


    private final ServiceDataService serviceDataService;
    private final OsirisSecurityService osirisSecurityService;
    private final ServiceTemplateDataService serviceTemplateDataService;
    private final ServiceTemplateFileDataService serviceTemplateFileDataService;
    private final LocalServiceLauncher localServiceLauncher;
    
    @GetMapping("/defaults")
    public Resources<OsirisService> getDefaultServices() {
        // Use the default service list, but retrieve updated objects from the database
        return new Resources<>(DefaultOsirisServices.getDefaultServices().stream()
                .map(s -> serviceDataService.getByName(s.getName()))
                .collect(Collectors.toList()));
    }
    
    /**
     * <p>Provides information on the status of the service Docker build</p>
     */
    @GetMapping("/{serviceId}/buildStatus")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity<BuildStatus> buildStatus(@ModelAttribute("serviceId") OsirisService service) {
        String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
        boolean needsBuild = needsBuild(service, currentServiceFingerprint);
        OsirisServiceDockerBuildInfo.Status status;
        if (service.getDockerBuildInfo() == null) {
            status = OsirisServiceDockerBuildInfo.Status.NOT_STARTED;
        }
        else {
            status = service.getDockerBuildInfo().getDockerBuildStatus();
        }
        BuildStatus buildStatus = new BuildStatus(needsBuild, status);
        return new ResponseEntity<BuildStatus>(buildStatus, HttpStatus.OK);
    }
    
    private boolean needsBuild(OsirisService osirisService, String currentServiceFingerprint) {
        if (osirisService.getDockerBuildInfo() == null) {
            return true;
        }
        if (osirisService.getDockerBuildInfo().getDockerBuildStatus() == OsirisServiceDockerBuildInfo.Status.ONGOING) {
            return false;
        }
        if (osirisService.getDockerBuildInfo().getLastBuiltFingerprint() == null) {
            return true;
        }
        return !currentServiceFingerprint.equals(osirisService.getDockerBuildInfo().getLastBuiltFingerprint());
    }
    
    /**
     * <p>Creates a new service from a template
     */
    
    @PostMapping("/{serviceId}/newTemplate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (hasRole('EXPERT_USER') and hasPermission(#service, 'administration'))")
    public ResponseEntity<Resource<OsirisServiceTemplate>> createNewServiceTemplate(@ModelAttribute("serviceId") OsirisService service, @RequestBody OsirisServiceTemplate serviceTemplate) {
    	if (serviceTemplate.getId() != null) {
    		return new ResponseEntity<Resource<OsirisServiceTemplate>>(HttpStatus.FORBIDDEN);
    	}
        OsirisServiceDescriptor serviceDescriptor = service.getServiceDescriptor();
        OsirisServiceDescriptor templateDescriptor = serviceTemplate.getServiceDescriptor();
        
        if (templateDescriptor == null) {
        	templateDescriptor = new OsirisServiceDescriptor();
    		templateDescriptor.setId(serviceTemplate.getName());
    		templateDescriptor.setDescription(serviceTemplate.getDescription());
    		serviceTemplate.setServiceDescriptor(templateDescriptor);
    	}
    	populateTemplateDescriptorFromServiceDescriptor(serviceDescriptor, templateDescriptor);
        
    	serviceTemplate.setType(service.getType());
    	serviceTemplate.setRequiredResources(service.getRequiredResources());
    	osirisSecurityService.updateOwnerWithCurrentUser(serviceTemplate);
    	serviceTemplateDataService.save(serviceTemplate);
    	createTemplateFilesFromContextFile(serviceTemplate, service.getContextFiles());
    	return new ResponseEntity<Resource<OsirisServiceTemplate>>(new Resource<OsirisServiceTemplate>(serviceTemplate), HttpStatus.CREATED);
    	
    }

	private void populateTemplateDescriptorFromServiceDescriptor(OsirisServiceDescriptor serviceDescriptor,
			OsirisServiceDescriptor templateDescriptor) {
		if (serviceDescriptor.getDataInputs() != null) {
			templateDescriptor.setDataInputs(serviceDescriptor.getDataInputs());
		}
		if (serviceDescriptor.getDataOutputs() != null) {
			templateDescriptor.setDataOutputs(serviceDescriptor.getDataOutputs());
		}
        templateDescriptor.setStatusSupported(serviceDescriptor.isStatusSupported());
        templateDescriptor.setStoreSupported(serviceDescriptor.isStoreSupported());
        templateDescriptor.setServiceProvider(serviceDescriptor.getServiceProvider());
        templateDescriptor.setServiceType(serviceDescriptor.getServiceType());
	}
    
    private Set<OsirisServiceTemplateFile> createTemplateFilesFromContextFile(OsirisServiceTemplate serviceTemplate,
			Set<OsirisServiceContextFile> contextFiles) {
		return contextFiles.stream().map(contextFile -> serviceTemplateFileDataService.save(new OsirisServiceTemplateFile(serviceTemplate, contextFile.getFilename(), contextFile.isExecutable(), contextFile.getContent()))).collect(Collectors.toSet());
	}
    
    /**
     * <p>Builds the service docker image
     * <p>Build is launched asynchronously</p>
     */
    @PostMapping("/{serviceId}/build")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity build(@ModelAttribute("serviceId") OsirisService service) {
        OsirisServiceDockerBuildInfo dockerBuildInfo = service.getDockerBuildInfo();
        
        if (dockerBuildInfo != null && dockerBuildInfo.getDockerBuildStatus().equals(OsirisServiceDockerBuildInfo.Status.ONGOING)) {
            return new ResponseEntity<>("A build is already ongoing",  HttpStatus.CONFLICT);
        }
        else {
            String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
            if (needsBuild(service, currentServiceFingerprint)) {
                LOG.info("Building service via REST API: {}", service.getName());
                if (dockerBuildInfo == null) {
                    dockerBuildInfo = new OsirisServiceDockerBuildInfo();
                    service.setDockerBuildInfo(dockerBuildInfo);
                }
                dockerBuildInfo.setDockerBuildStatus(Status.ONGOING);
                serviceDataService.save(service);
                BuildServiceParams.Builder buildServiceParamsBuilder = BuildServiceParams.newBuilder()
                        .setUserId(osirisSecurityService.getCurrentUser().getName())
                        .setServiceId(String.valueOf(service.getId()))
                        .setBuildFingerprint(currentServiceFingerprint);
                BuildServiceParams buildServiceParams = buildServiceParamsBuilder.build();
                buildService(service, buildServiceParams);
                return new ResponseEntity<>( HttpStatus.ACCEPTED);
            }
            else {
                return new ResponseEntity<>( HttpStatus.OK);
            }
        }
    }

    private void buildService(OsirisService osirisService, BuildServiceParams buildServiceParams) {
        serviceDataService.save(osirisService);
        BuildServiceObserver responseObserver = new BuildServiceObserver();
        localServiceLauncher.asyncBuildService(buildServiceParams, responseObserver);
    }
    

    public class BuildServiceObserver implements StreamObserver<BuildServiceResponse> {
        
        public BuildServiceObserver() {
        }

        @Override
        public void onNext(BuildServiceResponse value) {
        }

        @Override
        public void onError(Throwable t) {
           
            
        }

        @Override
        public void onCompleted() {
            
        }

    }


}