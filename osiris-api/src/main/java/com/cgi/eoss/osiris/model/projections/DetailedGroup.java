package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.Group;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Group entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedGroup", types = {Group.class})
public interface DetailedGroup extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortUser> getMembers();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Group), target.id)}")
    OsirisAccess getAccess();
}
