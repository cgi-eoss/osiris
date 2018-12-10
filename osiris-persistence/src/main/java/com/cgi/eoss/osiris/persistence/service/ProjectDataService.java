package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Project;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface ProjectDataService extends
        OsirisEntityDataService<Project>,
        SearchableDataService<Project> {
    Project getByNameAndOwner(String name, User user);

    List<Project> findByDatabasket(Databasket databasket);

    List<Project> findByJobConfig(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
