package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentProcessingDao extends OsirisEntityDao<IncidentProcessing> {
    List<IncidentProcessing> findByOwner(User user);

    List<IncidentProcessing> findByTemplate(IncidentProcessingTemplate incidentProcessingTemplate);

}
