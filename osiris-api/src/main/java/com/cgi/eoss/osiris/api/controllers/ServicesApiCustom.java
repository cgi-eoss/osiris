package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicesApiCustom {
    Page<OsirisService> findByFilterOnly(String filter, OsirisService.Type serviceType, Pageable pageable);

    Page<OsirisService> findByFilterAndOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable);

    Page<OsirisService> findByFilterAndNotOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable);
}
