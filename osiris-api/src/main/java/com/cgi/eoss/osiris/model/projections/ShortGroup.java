package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.Group;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Group entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortGroup", types = {Group.class})
public interface ShortGroup extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.members.size()}")
    Integer getSize();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Group), target.id)}")
    OsirisAccess getAccess();
}
