package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.OsirisFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.UUID;

/**
 * <p>Abbreviated representation of an OsirisFile entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortOsirisFile", types = {OsirisFile.class})
public interface ShortOsirisFile extends Identifiable<Long> {
    UUID getRestoId();
    ShortUser getOwner();
    String getFilename();
    OsirisFile.Type getType();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisFile), target.id)}")
    OsirisAccess getAccess();
}
