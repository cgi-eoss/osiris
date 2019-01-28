package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.User;

import java.util.List;
import java.util.Optional;

public interface SystematicProcessingDao extends OsirisEntityDao<SystematicProcessing> {

    List<SystematicProcessing> findByOwner(User user);

    Optional<SystematicProcessing> findByParentJob(Job parentJob);

}
