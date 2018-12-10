package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.User;
import java.util.List;

public interface CollectionDao extends OsirisEntityDao<Collection> {
    Collection findOneByNameAndOwner(String name, User user);

    List<Collection> findByNameContainingIgnoreCase(String term);

    List<Collection> findByOwner(User user);
    
    Collection findOneByIdentifier(String identifier);
}
