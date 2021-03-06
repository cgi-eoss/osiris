package com.cgi.eoss.osiris.api.mappings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.projections.DetailedJob;
import com.cgi.eoss.osiris.model.projections.ShortJob;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFileDataService;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;

/**
 * <p>HATEOAS resource processor for {@link Job}s. Adds extra _link entries for client use, e.g. job container logs.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JobResourceProcessor extends BaseResourceProcessor<Job> {


    private final RepositoryEntityLinks entityLinks;
    private final OsirisFileDataService osirisFileDataService;
    private final JobDataService jobDataService;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<Job> getTargetClass() {
        return Job.class;
    }

    private void addGuiLink(Resource resource, Job.Status status, String guiUrl) {
        if (status == Job.Status.RUNNING && !Strings.isNullOrEmpty(guiUrl)) {
            resource.add(new Link(guiUrl).withRel("gui"));
        }
    }

    private void addLogsLink(Resource resource) {
        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() + "/logs").withRel("logs"));
    }

    private void addOutputLinks(Resource resource, Multimap<String, String> outputs) {
        // Transform any "osiris://" URIs into relation links
        if (outputs != null && !outputs.isEmpty()) {
            outputs.entries().stream()
                    .filter(e -> e.getValue().startsWith("osiris://"))
                    .forEach(e -> {
                        OsirisFile osirisFile = osirisFileDataService.getByUri(e.getValue());
                        resource.add(entityLinks.linkToSingleResource(osirisFile).withRel("output-" + e.getKey()).expand());
                    });
        }
    }

    private void addTerminateLink(Resource resource, Long jobId) {
        Job job = jobDataService.getById(jobId);
        addTerminateLink(resource, job.getStatus(), job.getStage(), job.getConfig().getService().getType());
     }
     
     private void addTerminateLink(Resource resource, Status status, String stage, OsirisService.Type serviceType) {
     	//FTP Service can be stopped during the processing stage, even if in error
     	if (serviceType.equals(OsirisService.Type.FTP_SERVICE) && JobStep.PROCESSING.getText().equals(stage)) {
     		resource.add(new Link(resource.getLink("self").getHref() + "/terminate").withRel("terminate"));
     	}
     	//Other services can be stopped while their status is running
     	if (!serviceType.equals(OsirisService.Type.FTP_SERVICE) && status == Job.Status.RUNNING && JobStep.PROCESSING.getText().equals(stage)) {
     	    resource.add(new Link(resource.getLink("self").getHref() + "/terminate").withRel("terminate"));
     	}

     }
    
    private void addCancelLink(Resource resource, Job.Status status) {
        if (status == Job.Status.CREATED) {
            // TODO Do this properly with a method reference
            resource.add(new Link(resource.getLink("self").getHref() + "/cancel").withRel("cancel"));
        }
    }
    
    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<Job>> {
        @Override
        public Resource<Job> process(Resource<Job> resource) {
            Job entity = resource.getContent();

            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);
            addOutputLinks(resource, entity.getOutputs());
            addTerminateLink(resource, entity.getStatus(), entity.getStage(), entity.getConfig().getService().getType());
            addCancelLink(resource, entity.getStatus());
            
            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedJob>> {
        @Override
        public Resource<DetailedJob> process(Resource<DetailedJob> resource) {
            DetailedJob entity = resource.getContent();
            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);
            addOutputLinks(resource, entity.getOutputs());
            addTerminateLink(resource, entity.getStatus(), entity.getStage(), entity.getConfig().getService().getType());
            addCancelLink(resource, entity.getStatus());
            
            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortJob>> {
        @Override
        public Resource<ShortJob> process(Resource<ShortJob> resource) {
            ShortJob entity = resource.getContent();
            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);
            addTerminateLink(resource, entity.getId());
            addCancelLink(resource, entity.getStatus());
            return resource;
        }
    }

}
