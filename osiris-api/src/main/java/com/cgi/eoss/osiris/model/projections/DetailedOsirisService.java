package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.security.OsirisAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.Map;

/**
 * <p>Comprehensive representation of an OsirisService entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedOsirisService", types = OsirisService.class)
public interface DetailedOsirisService extends Identifiable<Long> {

    String getName();
    String getDescription();
    ShortUser getOwner();
    OsirisService.Type getType();
    String getDockerTag();
    URI getExternalServiceUri();
    OsirisService.Licence getLicence();
    OsirisService.Status getStatus();
    OsirisServiceDescriptor getServiceDescriptor();
    Map<Long, String> getAdditionalMounts();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisService), target.id)}")
    OsirisAccess getAccess();

}
