package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Project entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedProject", types = {Project.class})
public interface DetailedProject extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortDatabasket> getDatabaskets();
    Set<ShortOsirisService> getServices();
    Set<JobConfig> getJobConfigs();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Project), target.id)}")
    OsirisAccess getAccess();
}
