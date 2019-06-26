package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisFile.Type;
import com.cgi.eoss.osiris.model.User;
import java.util.List;
import java.util.Set;

public interface CollectionDataService extends
        OsirisEntityDataService<Collection>,
        SearchableDataService<Collection> {

    Collection getByNameAndOwner(String name, User user);

    List<Collection> findByOwner(User user);

    Collection getByIdentifier(String collectionIdentifier);
    
    Set<Collection> findByFileType(Type fileType);
}
