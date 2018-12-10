package com.cgi.eoss.osiris.persistence.dao;

import java.util.List;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserPreference;

public interface UserPreferenceDao extends OsirisEntityDao<UserPreference> {

    List<UserPreference> findByOwner(User user);

    UserPreference findOneByNameAndOwner(String name, User user);

    List<UserPreference> findByTypeAndOwner(String type, User user);

}
