package com.cgi.eoss.osiris.model.projections;

import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.security.OsirisAccess;
import com.google.common.collect.Multimap;

/**
 * <p>Comprehensive representation of a Job entity, including outputs and jobConfig, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedJob", types = Job.class)
public interface DetailedJob extends Identifiable<Long> {
    String getExtId();
    ShortUser getOwner();
    Job.Status getStatus();
    String getGuiUrl();
    String getStage();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    boolean isParent();
    @Value("#{target.config.service.name}")
    String getServiceName();
    Multimap<String, String> getOutputs();
    Set<ShortOsirisFile> getOutputFiles();
    JobConfig getConfig();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Job), target.id)}")
    OsirisAccess getAccess();
}
