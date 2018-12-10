package com.cgi.eoss.osiris.persistence.service;

import java.util.List;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.User;

public interface ServiceTemplateDataService extends
        OsirisEntityDataService<OsirisServiceTemplate>,
        SearchableDataService<OsirisServiceTemplate> {
    List<OsirisServiceTemplate> findByOwner(User user);

    OsirisServiceTemplate getByName(String serviceTemplateName);

}
