package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface GroupDao extends OsirisEntityDao<Group> {
    Group findOneByName(String name);

    List<Group> findByNameContainingIgnoreCase(String term);

    List<Group> findByMembersContaining(User member);

    List<Group> findByOwner(User user);
}
