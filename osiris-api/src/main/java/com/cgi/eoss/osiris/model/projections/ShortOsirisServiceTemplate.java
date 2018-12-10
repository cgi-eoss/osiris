package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link OsirisServiceTemplate}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortOsirisServiceTemplate", types = {OsirisServiceTemplate.class})
public interface ShortOsirisServiceTemplate extends Identifiable<Long> {
    String getName();
    String getDescription();
    OsirisService.Type getType();
    ShortUser getOwner();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisServiceTemplate), target.id)}")
    OsirisAccess getAccess();
}
