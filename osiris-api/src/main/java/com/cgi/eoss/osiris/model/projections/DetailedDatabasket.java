package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Databasket entity, including all OsirisFile catalogue metadata, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedDatabasket", types = Databasket.class)
public interface DetailedDatabasket extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortOsirisFile> getFiles();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Databasket), target.id)}")
    OsirisAccess getAccess();
}
