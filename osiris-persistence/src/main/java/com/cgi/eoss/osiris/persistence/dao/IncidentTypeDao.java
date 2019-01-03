package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentTypeDao extends OsirisEntityDao<IncidentType> {
    List<IncidentType> findByOwner(User user);
}
