package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentDao extends OsirisEntityDao<Incident> {
    List<Incident> findByOwner(User user);

    List<Incident> findByType(IncidentType incidentType);
}
