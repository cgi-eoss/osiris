package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.security.OsirisAccess;
import com.cgi.eoss.osiris.model.OsirisFile;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an OsirisFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedOsirisFile", types = OsirisFile.class)
public interface DetailedOsirisFile extends Identifiable<Long> {
    URI getUri();
    UUID getRestoId();
    OsirisFile.Type getType();
    ShortUser getOwner();
    String getFilename();
    @Value("#{@restoServiceImpl.getGeoJsonSafe(target)}")
    GeoJsonObject getMetadata();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisFile), target.id)}")
    OsirisAccess getAccess();
}
