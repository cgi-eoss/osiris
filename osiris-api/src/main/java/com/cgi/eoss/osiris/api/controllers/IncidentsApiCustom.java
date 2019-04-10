package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface IncidentsApiCustom {

    Page<Incident> findByType(IncidentType type, Pageable pageable);

    Page<Incident> findByFilterOnly(String filter, IncidentType incidentType, Pageable pageable);

    Page<Incident> findByFilterAndOwner(String filter, User user, IncidentType incidentType, Pageable pageable);

    Page<Incident> findByFilterAndNotOwner(String filter, User user, IncidentType incidentType, Pageable pageable);

    Page<Incident> findByDateRange(Instant startDate, Instant endDate, Pageable pageable);

    Page<Incident> findByCollection(Collection collection, Pageable pageable);
}
