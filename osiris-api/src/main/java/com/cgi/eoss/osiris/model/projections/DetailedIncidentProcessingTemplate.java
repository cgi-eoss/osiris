package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.security.OsirisAccess;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Detailed representation of a IncidentProcessingTemplate entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedIncidentProcessingTemplate", types = {IncidentProcessingTemplate.class})
public interface DetailedIncidentProcessingTemplate extends Identifiable<Long> {
    String getTitle();
    String getDescription();
    ShortIncidentType getIncidentType();
    ShortUser getOwner();
    ShortOsirisService getService();
    String getCronExpression();
    String getSystematicInput();
    Multimap<String, String> getFixedInputs();
    ListMultimap<String, String> getSearchParameters();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.IncidentProcessingTemplate), target.id)}")
    OsirisAccess getAccess();
    
}
