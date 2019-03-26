package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystematicProcessingDataService extends OsirisEntityDataService<SystematicProcessing> {

    List<SystematicProcessing> findByStatus(Status s);

    Optional<SystematicProcessing> findByParentJob(Job parentJob);

    SystematicProcessing buildNew(String extId, String userId, String serviceId, String jobConfigLabel, String systematicParameter,
                                  Multimap<String, String> inputs, ListMultimap<String, String> searchParameters, String cronExpression, LocalDateTime lastUpdated);
}
