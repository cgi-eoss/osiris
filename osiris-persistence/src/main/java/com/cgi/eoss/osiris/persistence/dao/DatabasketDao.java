package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface DatabasketDao extends OsirisEntityDao<Databasket> {
    Databasket findOneByNameAndOwner(String name, User user);

    List<Databasket> findByNameContainingIgnoreCase(String term);

    List<Databasket> findByFilesContaining(OsirisFile file);

    List<Databasket> findByOwner(User user);
}
