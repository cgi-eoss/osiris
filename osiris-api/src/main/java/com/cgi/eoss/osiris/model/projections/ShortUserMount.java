package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.UserMount;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>
 * Abbreviated representation of a UserMount entity, for embedding in REST responses.
 * </p>
 */
@Projection(name = "shortUserMount", types = {UserMount.class})
public interface ShortUserMount extends Identifiable<Long> {
    String getName();

    String getType();
    
    String getMountPath();

    ShortUser getOwner();

    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.UserMount), target.id)}")
    OsirisAccess getAccess();
}
