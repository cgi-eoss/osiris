package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Short representation of a IncidentProcessingTemplate entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortIncidentProcessingTemplate", types = {IncidentProcessingTemplate.class})
public interface ShortIncidentProcessingTemplate extends Identifiable<Long> {
    String getTitle();
    String getDescription();
    ShortOsirisService getService();
}
