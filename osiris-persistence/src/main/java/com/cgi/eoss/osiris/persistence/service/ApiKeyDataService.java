package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.ApiKey;
import com.cgi.eoss.osiris.model.User;

public interface ApiKeyDataService extends
        OsirisEntityDataService<ApiKey> {

	ApiKey getByOwner(User user);
}
