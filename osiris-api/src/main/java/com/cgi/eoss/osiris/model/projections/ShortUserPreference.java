package com.cgi.eoss.osiris.model.projections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;
import com.cgi.eoss.osiris.model.UserPreference;
import com.cgi.eoss.osiris.security.OsirisAccess;

/**
 * <p>
 * Abbreviated representation of a UserPreference entity, for embedding in REST responses.
 * </p>
 */
@Projection(name = "shortUserPreference", types = {UserPreference.class})
public interface ShortUserPreference extends Identifiable<Long> {
    String getName();

    String getType();

    ShortUser getOwner();

    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.UserPreference), target.id)}")
    OsirisAccess getAccess();
}
