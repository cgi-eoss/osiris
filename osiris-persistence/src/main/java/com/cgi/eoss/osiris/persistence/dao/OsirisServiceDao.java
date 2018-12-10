package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface OsirisServiceDao extends OsirisEntityDao<OsirisService> {
    List<OsirisService> findByNameContainingIgnoreCase(String term);

    List<OsirisService> findByOwner(User user);

    List<OsirisService> findByStatus(OsirisService.Status status);
}
