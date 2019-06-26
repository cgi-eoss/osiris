package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Collection entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedCollection", types = {Collection.class})
public interface DetailedCollection extends Identifiable<Long> {
    String getName();
    String getIdentifier();
    String getDescription();
    String getProductsType();
    OsirisFile.Type getFileType();
    ShortUser getOwner();
    @Value("#{target.osirisFiles.size()}")
    Integer getSize();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.Collection), target.id)}")
    OsirisAccess getAccess();
}
