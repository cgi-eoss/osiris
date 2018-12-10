package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.UserEndpoint;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>
 * Abbreviated representation of a UserEndpoint entity, for embedding in REST responses.
 * </p>
 */
@Projection(name = "shortUserEndpoint", types = {UserEndpoint.class})
public interface ShortUserEndpoint extends Identifiable<Long> {
    String getName();
    
    String getUrl();

    ShortUser getOwner();

    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.UserEndpoint), target.id)}")
    OsirisAccess getAccess();
}
