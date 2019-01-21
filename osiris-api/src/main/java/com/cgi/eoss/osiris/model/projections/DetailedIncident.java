package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.time.Instant;

/**
 * <p>Detailed representation of a Incident entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedIncident", types = {Incident.class})
public interface DetailedIncident extends Identifiable<Long> {
    String getTitle();
    String getDescription();
    ShortIncidentType getType();
    ShortUser getOwner();
    String getAoi();
    Instant getStartDate();
    Instant getEndDate();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Incident), target.id)}")
    OsirisAccess getAccess();

}
