package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.User;

public interface UserDataService extends
        OsirisEntityDataService<User>,
        SearchableDataService<User> {
    User getByName(String name);

    /**
     * <p>Return a persisted user with the given name, creating a new entity if none already exists.</p>
     * <p>This method is intended for use where automatic user creation is necessary.</p>
     *
     * @param name The desired username.
     * @return A persisted user entity with the given name.
     */
    User getOrSave(String name);

    User getDefaultUser();
}
