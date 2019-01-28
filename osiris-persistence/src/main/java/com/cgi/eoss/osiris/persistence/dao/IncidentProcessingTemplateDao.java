package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface IncidentProcessingTemplateDao extends OsirisEntityDao<IncidentProcessing> {
    List<IncidentProcessingTemplate> findByOwner(User user);

}
