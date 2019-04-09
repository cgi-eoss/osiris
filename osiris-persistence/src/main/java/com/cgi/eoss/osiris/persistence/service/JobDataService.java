package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;
import com.google.common.collect.Multimap;

import java.util.List;

public interface JobDataService extends
        OsirisEntityDataService<Job> {

    List<Job> findByOwner(User user);

    List<Job> findByService(OsirisService service);

    List<Job> findByOwnerAndService(User user, OsirisService service);
    
    List<Job> findByStatusAndGuiUrlNotNull(Status status);

    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs);

    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, String systematicParameter, boolean isParent);
    
    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob);

    Job reload(Long id);

	
}
