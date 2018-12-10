package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.PublishingRequest;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface PublishingRequestDataService extends
        OsirisEntityDataService<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);

    List<PublishingRequest> findRequestsForPublishing(OsirisService service);
    
    List<PublishingRequest> findRequestsForPublishingServiceTemplate(OsirisServiceTemplate serviceTemplate);
    
    List<PublishingRequest> findRequestsForPublishingCollection(Collection collection);

    List<PublishingRequest> findOpenByAssociated(Class<?> objectClass, Long identifier);
}
