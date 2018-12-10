package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.QJobConfig;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.JobConfigDao;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.osiris.model.QJobConfig.jobConfig;

@Service
@Transactional(readOnly = true)
public class JpaJobConfigDataService extends AbstractJpaDataService<JobConfig> implements JobConfigDataService {

    private final JobConfigDao dao;

    @Autowired
    public JpaJobConfigDataService(JobConfigDao jobConfigDao, UserDataService userDataService, ServiceDataService serviceDataService) {
        this.dao = jobConfigDao;
    }

    @Override
    OsirisEntityDao<JobConfig> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(JobConfig entity) {
        BooleanExpression equalityExpr = jobConfig.owner.eq(entity.getOwner())
                .and(jobConfig.service.eq(entity.getService()))
                .and(jobConfig.inputs.eq(entity.getInputs()));
        if (entity.getParent() == null) {
            equalityExpr = equalityExpr.and(QJobConfig.jobConfig.parent.isNull());
        }
        else {
            equalityExpr = equalityExpr.and(QJobConfig.jobConfig.parent.id.eq(entity.getParent().getId()));
        }
        if (entity.getSystematicParameter() == null) {
            equalityExpr = equalityExpr.and(QJobConfig.jobConfig.systematicParameter.isNull());
        }
        else {
            equalityExpr = equalityExpr.and(QJobConfig.jobConfig.systematicParameter.eq(entity.getSystematicParameter()));
        }
        return equalityExpr;
    }

    @Override
    public List<JobConfig> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<JobConfig> findByService(OsirisService service) {
        return dao.findByService(service);
    }

    @Override
    public List<JobConfig> findByOwnerAndService(User user, OsirisService service) {
        return dao.findByOwnerAndService(user, service);
    }

}
