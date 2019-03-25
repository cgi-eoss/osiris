package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentProcessingTemplateDataService extends OsirisEntityDataService<IncidentProcessingTemplate> {

    List<IncidentProcessingTemplate> findByOwner(User user);

}
