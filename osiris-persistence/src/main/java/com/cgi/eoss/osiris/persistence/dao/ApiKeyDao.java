package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.ApiKey;
import com.cgi.eoss.osiris.model.User;

public interface ApiKeyDao extends OsirisEntityDao<ApiKey> {
  
	public ApiKey getByOwner(User owner);
}
