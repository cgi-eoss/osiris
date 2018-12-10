package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface GroupDataService extends
        OsirisEntityDataService<Group>,
        SearchableDataService<Group> {
    Group getByName(String name);

    List<Group> findGroupMemberships(User user);

    List<Group> findByOwner(User user);
}
