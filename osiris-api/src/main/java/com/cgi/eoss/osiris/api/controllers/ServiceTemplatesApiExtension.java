package com.cgi.eoss.osiris.api.controllers;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;
import com.cgi.eoss.osiris.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceFileDataService;
import com.cgi.eoss.osiris.security.OsirisSecurityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;

@RestController
@BasePathAwareController
@RequestMapping("/serviceTemplates")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServiceTemplatesApiExtension {

    private final ServiceDataService serviceDataService;
    private final ServiceFileDataService serviceFileDataService;
    private final DefaultServiceTemplateDataService defaultServiceTemplateDataService;
    private final OsirisSecurityService osirisSecurityService;
    
    /**
     * <p>Creates a new service from a template
     */
    
    @PostMapping("/{serviceTemplateId}/newService")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (hasRole('EXPERT_USER') and hasPermission(#serviceTemplate, 'read'))")
    public ResponseEntity<Resource<OsirisService>> createNewService(@ModelAttribute("serviceTemplateId") OsirisServiceTemplate serviceTemplate, @RequestBody OsirisService service) {
    	if (service.getId() != null) {
    		return new ResponseEntity<Resource<OsirisService>>(HttpStatus.FORBIDDEN);
    	}
        OsirisServiceDescriptor templateDescriptor = serviceTemplate.getServiceDescriptor();
        OsirisServiceDescriptor serviceDescriptor = service.getServiceDescriptor();
    	if (serviceDescriptor == null) {
    		serviceDescriptor = new OsirisServiceDescriptor();
    		serviceDescriptor.setId(service.getName());
    		serviceDescriptor.setDataInputs(Collections.<Parameter>emptyList());
    		serviceDescriptor.setDataOutputs(Collections.<Parameter>emptyList());
    		service.setServiceDescriptor(serviceDescriptor);
    	
    	}
    	
    	if (templateDescriptor.getDataInputs() != null) {
    		serviceDescriptor.setDataInputs(templateDescriptor.getDataInputs());
    	}
    	
    	if (templateDescriptor.getDataOutputs() != null) {
    		serviceDescriptor.setDataOutputs(templateDescriptor.getDataOutputs());
    	}
        
    	service.setType(serviceTemplate.getType());
    	osirisSecurityService.updateOwnerWithCurrentUser(service);
    	serviceDataService.save(service);
    	createContextFileFromTemplateFiles(service, serviceTemplate.getTemplateFiles());
    	return new ResponseEntity<Resource<OsirisService>>(new Resource<OsirisService>(service), HttpStatus.CREATED);
    	
    }
    
    


	private Set<OsirisServiceContextFile> createContextFileFromTemplateFiles(OsirisService service,
			Set<OsirisServiceTemplateFile> templateFiles) {
		return templateFiles.stream().map(templateFile -> serviceFileDataService.save(new OsirisServiceContextFile(service, templateFile.getFilename(), templateFile.isExecutable(), templateFile.getContent()))).collect(Collectors.toSet());
	}


}