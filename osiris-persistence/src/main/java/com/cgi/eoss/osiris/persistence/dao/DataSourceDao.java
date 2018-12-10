package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.DataSource;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface DataSourceDao extends OsirisEntityDao<DataSource> {
    List<DataSource> findByNameContainingIgnoreCase(String term);
    DataSource findOneByName(String name);
    List<DataSource> findByOwner(User user);
}
