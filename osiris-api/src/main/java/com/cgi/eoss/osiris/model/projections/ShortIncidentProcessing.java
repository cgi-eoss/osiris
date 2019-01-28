package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.security.OsirisAccess;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Short representation of a IncidentProcessing entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortIncidentProcessing", types = {IncidentProcessing.class})
public interface ShortIncidentProcessing extends Identifiable<Long> {
    ShortIncidentProcessingTemplate getTemplate();
    ShortSystematicProcessing getSystematicProcessing();
    ShortCollection getCollection();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.IncidentProcessing), target.id)}")
    OsirisAccess getAccess();
    
}
