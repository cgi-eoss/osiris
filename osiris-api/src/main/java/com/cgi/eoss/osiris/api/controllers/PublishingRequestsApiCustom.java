package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.PublishingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface PublishingRequestsApiCustom {

    Page<PublishingRequest> findByStatus(Collection<PublishingRequest.Status> statuses, Pageable pageable);

}
