package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentDataService extends OsirisEntityDataService<Incident> {

    List<Incident> findByOwner(User user);

    List<Incident> findByType(IncidentType incidentType);

}
