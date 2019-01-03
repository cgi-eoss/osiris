package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentTypeDataService extends OsirisEntityDataService<IncidentType> {

    List<IncidentType> findByOwner(User user);

	
}
