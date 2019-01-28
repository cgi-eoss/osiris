package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.SystematicProcessingDao;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.cgi.eoss.osiris.model.QSystematicProcessing.systematicProcessing;

@Service
@Transactional(readOnly = true)
public class JpaSystematicProcessingDataService extends AbstractJpaDataService<SystematicProcessing>
        implements SystematicProcessingDataService {

    private final SystematicProcessingDao dao;
    private final UserDataService userDataService;
    private final JobDataService jobDataService;

    @Autowired
    public JpaSystematicProcessingDataService(SystematicProcessingDao systematicProcessingDao, UserDataService userDataService, JobDataService jobDataService) {
        this.dao = systematicProcessingDao;
        this.userDataService = userDataService;
        this.jobDataService = jobDataService;
    }

    @Override
    OsirisEntityDao<SystematicProcessing> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(SystematicProcessing entity) {
    	if (entity.getId() == null)
    		return null;
        return systematicProcessing.id.eq(entity.getId());
    }

    @Override
    public List<SystematicProcessing> findByStatus(Status s) {
        return dao.findAll(systematicProcessing.status.eq(s));
    }

    @Override
    public Optional<SystematicProcessing> findByParentJob(Job parentJob) {
        return dao.findByParentJob(parentJob);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SystematicProcessing buildNew(String extId, String userId, String serviceId, String jobConfigLabel, String systematicParameter,
                                         Multimap<String, String> inputs, ListMultimap<String, String> searchParameters, LocalDateTime lastUpdated) {
        Job parentJob = jobDataService.buildNew(extId, userId, serviceId, jobConfigLabel, inputs, systematicParameter);
        return dao.save(new SystematicProcessing(userDataService.getByName(userId), parentJob, searchParameters, lastUpdated));
    }
}
