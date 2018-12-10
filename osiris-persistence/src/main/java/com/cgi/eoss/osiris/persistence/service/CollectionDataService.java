package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.User;
import java.util.List;

public interface CollectionDataService extends
        OsirisEntityDataService<Collection>,
        SearchableDataService<Collection> {

    Collection getByNameAndOwner(String name, User user);

    List<Collection> findByOwner(User user);

    Collection getByIdentifier(String collectionIdentifier);
}
