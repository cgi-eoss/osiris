package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.PublishingRequest;
import com.cgi.eoss.osiris.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.osiris.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.osiris.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.services.DefaultOsirisServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Functionality for users with the CONTENT_AUTHORITY Role.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/contentAuthority")
@Log4j2
public class ContentAuthorityApi {

    private final OsirisSecurityService osirisSecurityService;
    private final ZooManagerClient zooManagerClient;
    private final PublishingRequestDataService publishingRequestsDataService;
    private final ServiceDataService serviceDataService;
    private final DefaultServiceTemplateDataService defaultServiceTemplateDataService;

    @Autowired
    public ContentAuthorityApi(OsirisSecurityService osirisSecurityService, ZooManagerClient zooManagerClient, PublishingRequestDataService publishingRequestsDataService, ServiceDataService serviceDataService, DefaultServiceTemplateDataService defaultServiceTemplateDataService) {
        this.osirisSecurityService = osirisSecurityService;
        this.zooManagerClient = zooManagerClient;
        this.publishingRequestsDataService = publishingRequestsDataService;
        this.serviceDataService = serviceDataService;
        this.defaultServiceTemplateDataService = defaultServiceTemplateDataService;
    }

    @PostMapping("/services/restoreDefaults")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void restoreDefaultServices() {
        Set<OsirisService> defaultServices = DefaultOsirisServices.getDefaultServices();

        for (OsirisService service : defaultServices) {
            LOG.info("Restoring default service: {}", service.getName());

            // If the service already exists, synchronise the IDs (and associated file IDs) to avoid constraint errors
            Optional.ofNullable(serviceDataService.getByName(service.getName())).ifPresent((OsirisService existing) -> {
                service.setId(existing.getId());
                service.getContextFiles().forEach(newFile -> {
                    existing.getContextFiles().stream()
                            .filter(existingFile -> existingFile.getFilename().equals(newFile.getFilename()))
                            .findFirst()
                            .ifPresent(existingFile -> newFile.setId(existingFile.getId()));
                });
            });

            service.setOwner(osirisSecurityService.refreshPersistentUser(service.getOwner()));
            serviceDataService.save(service);
            publishService(service);
        }
    }

    @PostMapping("/services/wps/syncAllPublic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void wpsSyncAllPublic() {
        // Find all Status.AVAILABLE, then filter for those visible to PUBLIC
        List<OsirisService> publicServices = serviceDataService.findAllAvailable().stream()
                .filter(s -> osirisSecurityService.isPublic(OsirisService.class, s.getId()))
                .collect(Collectors.toList());
        zooManagerClient.updateActiveZooServices(publicServices);
    }

    @PostMapping("/services/publish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishService(@ModelAttribute("serviceId") OsirisService service) {
        service.setStatus(OsirisService.Status.AVAILABLE);
        serviceDataService.save(service);

        osirisSecurityService.publish(OsirisService.class, service.getId());
        publishingRequestsDataService.findRequestsForPublishing(service).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/services/unpublish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishService(@ModelAttribute("serviceId") OsirisService service) {
        osirisSecurityService.unpublish(OsirisService.class, service.getId());
    }
    
    @PostMapping("/serviceTemplates/publish/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishServiceTemplate(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate) {
        osirisSecurityService.publish(OsirisServiceTemplate.class, serviceTemplate.getId());
        publishingRequestsDataService.findRequestsForPublishingServiceTemplate(serviceTemplate).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/serviceTemplates/unpublish/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishServiceTemplate(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate) {
        osirisSecurityService.unpublish(OsirisServiceTemplate.class, serviceTemplate.getId());
    }
    
    /**
     * <p>Makes the template default for its type
     */
    
    @PostMapping("/serviceTemplates/makeDefault/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public ResponseEntity<Void> makeTemplateDefault(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate) {
    	DefaultServiceTemplate defaultServiceTemplate = defaultServiceTemplateDataService.getByServiceType(serviceTemplate.getType());
    	if (defaultServiceTemplate != null) {
    		if (defaultServiceTemplate.getServiceTemplate().getId() != serviceTemplate.getId()) {
    			defaultServiceTemplate.setServiceTemplate(serviceTemplate);
    			defaultServiceTemplateDataService.save(defaultServiceTemplate);
        	}
    		return new ResponseEntity<Void>(HttpStatus.OK);
    	}
		defaultServiceTemplate = new DefaultServiceTemplate();
		defaultServiceTemplate.setServiceType(serviceTemplate.getType());
		defaultServiceTemplate.setServiceTemplate(serviceTemplate);
		defaultServiceTemplateDataService.save(defaultServiceTemplate);
    	return new ResponseEntity<Void>(HttpStatus.OK);
    }
    
    @PostMapping("/collections/publish/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishServiceTemplate(@ModelAttribute("collectionId") Collection collection) {
        osirisSecurityService.publish(Collection.class, collection.getId());
        publishingRequestsDataService.findRequestsForPublishingCollection(collection).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }
    
    @PostMapping("/collections/unpublish/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishCollection(@ModelAttribute("collectionId") Collection collection) {
        osirisSecurityService.unpublish(Collection.class, collection.getId());
    }

}
