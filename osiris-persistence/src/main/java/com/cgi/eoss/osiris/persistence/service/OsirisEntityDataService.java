package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisEntity;

/**
 * A directory to store, load and delete OsirisEntity objects. Provides data integrity and constraint checks
 * before passing to the DAO.
 *
 * @param <T> The data type to be provided.
 */
public interface OsirisEntityDataService<T extends OsirisEntity<T>> extends DataService<T, Long> {

}
