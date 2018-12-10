package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Project;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface ProjectDao extends OsirisEntityDao<Project> {
    Project findOneByNameAndOwner(String name, User user);

    List<Project> findByNameContainingIgnoreCase(String term);

    List<Project> findByDatabasketsContaining(Databasket databasket);

    List<Project> findByJobConfigsContaining(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
