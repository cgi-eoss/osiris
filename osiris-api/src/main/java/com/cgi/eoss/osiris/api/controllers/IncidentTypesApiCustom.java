package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentTypesApiCustom {

    Page<IncidentType> findByFilterOnly(String filter, Pageable pageable);

    Page<IncidentType> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<IncidentType> findByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
