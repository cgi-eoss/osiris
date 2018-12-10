package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface JobConfigDao extends OsirisEntityDao<JobConfig> {
    List<JobConfig> findByOwner(User user);

    List<JobConfig> findByService(OsirisService service);

    List<JobConfig> findByOwnerAndService(User user, OsirisService service);
}
