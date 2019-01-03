package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentsApiCustom {
    Page<Incident> findByType(IncidentType type, Pageable pageable);
}
