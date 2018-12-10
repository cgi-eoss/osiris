package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.PublishingRequest;
import com.cgi.eoss.osiris.persistence.service.PublishingRequestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@BasePathAwareController
@RequestMapping("/publishingRequests")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class PublishingRequestsApiExtension {

    private final PublishingRequestDataService dataService;
    private final RepositoryEntityLinks entityLinks;
    private final OsirisSecurityService osirisSecurityService;

    @PostMapping("/requestPublishService/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity requestPublishService(@ModelAttribute("serviceId") OsirisService service) {
        PublishingRequest newRequest = PublishingRequest.builder()
                .owner(osirisSecurityService.getCurrentUser())
                .type(PublishingRequest.Type.SERVICE)
                .associatedId(service.getId())
                .status(PublishingRequest.Status.REQUESTED)
                .build();

        PublishingRequest persistent = dataService.findOneByExample(newRequest);

        if (persistent != null) {
            // Re-request if necessary
            persistent.setStatus(PublishingRequest.Status.REQUESTED);
            persistent = dataService.save(newRequest);
            return ResponseEntity.noContent().location(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        } else {
            persistent = dataService.save(newRequest);
            return ResponseEntity.created(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        }
    }
    
    @PostMapping("/requestPublishServiceTemplate/{serviceTemplateId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplate, 'administration')")
    public ResponseEntity requestPublishServiceTemplate(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate) {
        PublishingRequest newRequest = PublishingRequest.builder()
                .owner(osirisSecurityService.getCurrentUser())
                .type(PublishingRequest.Type.SERVICE_TEMPLATE)
                .associatedId(serviceTemplate.getId())
                .status(PublishingRequest.Status.REQUESTED)
                .build();

        PublishingRequest persistent = dataService.findOneByExample(newRequest);

        if (persistent != null) {
            // Re-request if necessary
            persistent.setStatus(PublishingRequest.Status.REQUESTED);
            persistent = dataService.save(newRequest);
            return ResponseEntity.noContent().location(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        } else {
            persistent = dataService.save(newRequest);
            return ResponseEntity.created(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        }
    }
    
    @PostMapping("/requestPublishCollection/{collectionId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#collection, 'administration')")
    public ResponseEntity requestPublishCollection(@ModelAttribute("collectionId") Collection collection) {
        PublishingRequest newRequest = PublishingRequest.builder()
                .owner(osirisSecurityService.getCurrentUser())
                .type(PublishingRequest.Type.COLLECTION)
                .associatedId(collection.getId())
                .status(PublishingRequest.Status.REQUESTED)
                .build();

        PublishingRequest persistent = dataService.findOneByExample(newRequest);

        if (persistent != null) {
            // Re-request if necessary
            persistent.setStatus(PublishingRequest.Status.REQUESTED);
            persistent = dataService.save(newRequest);
            return ResponseEntity.noContent().location(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        } else {
            persistent = dataService.save(newRequest);
            return ResponseEntity.created(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        }
    }

}
