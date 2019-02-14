package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.List;

/**
 * <p>Detailed representation of a IncidentType entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedIncidentType", types = {IncidentType.class})
public interface DetailedIncidentType extends Identifiable<Long> {
    String getTitle();
    String getDescription();
    String getIconId();
    ShortUser getOwner();
    List<ShortIncidentProcessingTemplate> getIncidentProcessingTemplates();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.IncidentType), target.id)}")
    OsirisAccess getAccess();
    
}
