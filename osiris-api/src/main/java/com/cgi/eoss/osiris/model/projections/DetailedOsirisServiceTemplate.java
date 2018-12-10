package com.cgi.eoss.osiris.model.projections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.security.OsirisAccess;

/**
 * <p>Comprehensive representation of an OsirisServiceTemplate entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedOsirisServiceTemplate", types = OsirisServiceTemplate.class)
public interface DetailedOsirisServiceTemplate extends Identifiable<Long> {

    String getName();
    String getDescription();
    ShortUser getOwner();
    OsirisService.Type getType();
    OsirisServiceDescriptor getServiceDescriptor();
    @Value("#{@osirisSecurityService.getCurrentAccess(T(com.cgi.eoss.osiris.model.OsirisServiceTemplate), target.id)}")
    OsirisAccess getAccess();

}
