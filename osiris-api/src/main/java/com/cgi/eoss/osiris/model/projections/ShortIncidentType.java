package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a IncidentType entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortIncidentType", types = {IncidentType.class})
public interface ShortIncidentType extends Identifiable<Long> {
    String getTitle();
    String getIconId();
    ShortUser getOwner();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.IncidentType), target.id)}")
    OsirisAccess getAccess();
    
}
