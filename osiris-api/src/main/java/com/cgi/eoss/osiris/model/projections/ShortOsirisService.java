package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.OsirisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link OsirisService}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortOsirisService", types = {OsirisService.class})
public interface ShortOsirisService extends Identifiable<Long> {
    String getName();
    String getDescription();
    OsirisService.Type getType();
    ShortUser getOwner();
    String getDockerTag();
    OsirisService.Licence getLicence();
    OsirisService.Status getStatus();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisService), target.id)}")
    OsirisAccess getAccess();
}
