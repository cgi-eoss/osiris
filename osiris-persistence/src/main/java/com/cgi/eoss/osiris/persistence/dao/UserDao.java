package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.User;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface UserDao extends OsirisEntityDao<User> {
    @EntityGraph(attributePaths = {"groups"})
    User findOneByName(String name);
    List<User> findByNameContainingIgnoreCase(String term);
}
