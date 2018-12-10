package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Databasket entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortDatabasket", types = {Databasket.class})
public interface ShortDatabasket extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.files.size()}")
    Integer getSize();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Databasket), target.id)}")
    OsirisAccess getAccess();
}
