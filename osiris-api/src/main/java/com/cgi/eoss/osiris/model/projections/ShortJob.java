package com.cgi.eoss.osiris.model.projections;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.security.OsirisAccess;

/**
 * <p>Default JSON projection for embedded {@link Job}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortJob", types = {Job.class})
public interface ShortJob extends Identifiable<Long> {
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
    @Value("#{target.config.systematicParameter}")
    String getSystematicParameter();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Job), target.id)}")
    OsirisAccess getAccess();
}
