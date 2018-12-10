package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.DataSource;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface DataSourceDataService extends
        OsirisEntityDataService<DataSource>,
        SearchableDataService<DataSource> {
    DataSource getByName(String name);

    List<DataSource> findByOwner(User user);

    DataSource getForService(OsirisService service);

    DataSource getForExternalProduct(OsirisFile osirisFile);

    DataSource getForRefData(OsirisFile osirisFile);
}
