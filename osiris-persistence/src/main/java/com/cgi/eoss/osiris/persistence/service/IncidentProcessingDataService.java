package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentProcessingDataService extends OsirisEntityDataService<IncidentProcessing> {

    List<IncidentProcessing> findByOwner(User user);

    List<IncidentProcessing> findByTemplate(IncidentProcessingTemplate incidentProcessingTemplate);

}
