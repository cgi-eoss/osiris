package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Collection entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortCollection", types = {Collection.class})
public interface ShortCollection extends Identifiable<Long> {
    String getName();
    String getIdentifier();
    String getDescription();
    String getProductsType();
    ShortUser getOwner();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Collection), target.id)}")
    OsirisAccess getAccess();
    
}
