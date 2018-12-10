package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface DatabasketDataService extends
        OsirisEntityDataService<Databasket>,
        SearchableDataService<Databasket> {
    Databasket getByNameAndOwner(String name, User user);

    List<Databasket> findByFile(OsirisFile file);

    List<Databasket> findByOwner(User user);
}
