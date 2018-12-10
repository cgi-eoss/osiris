package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.User;

import java.util.List;

public interface JobDao extends OsirisEntityDao<Job> {
    List<Job> findByOwner(User user);

    List<Job> findByConfig_Service(OsirisService service);
    
    List<Job> findByStatusAndGuiUrlNotNull(Status status);

    List<Job> findByOwnerAndConfig_Service(User user, OsirisService service);
}
