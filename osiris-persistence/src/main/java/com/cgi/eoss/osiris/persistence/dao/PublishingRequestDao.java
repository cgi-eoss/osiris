package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.PublishingRequest;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface PublishingRequestDao extends OsirisEntityDao<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);
}
