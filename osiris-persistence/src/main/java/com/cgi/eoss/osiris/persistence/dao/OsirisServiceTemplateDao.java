package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface OsirisServiceTemplateDao extends OsirisEntityDao<OsirisServiceTemplate> {
    List<OsirisServiceTemplate> findByNameContainingIgnoreCase(String term);

    List<OsirisServiceTemplate> findByOwner(User user);

}
