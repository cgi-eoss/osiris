package com.cgi.eoss.osiris.model.projections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.security.OsirisAccess;

/**
 * <p>Default JSON projection for embedded {@link SystematicProcessing}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortSystematicProcessing", types = {SystematicProcessing.class})
public interface ShortSystematicProcessing extends Identifiable<Long> {
    ShortUser getOwner();
    SystematicProcessing.Status getStatus();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Job), target.id)}")
    OsirisAccess getAccess();
}
