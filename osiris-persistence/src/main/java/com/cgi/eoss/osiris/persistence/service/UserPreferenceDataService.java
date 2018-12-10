package com.cgi.eoss.osiris.persistence.service;

import java.util.List;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserPreference;

public interface UserPreferenceDataService extends OsirisEntityDataService<UserPreference> {

    UserPreference getByNameAndOwner(String name, User user);

    List<UserPreference> findByTypeAndOwner(String type, User user);

}
