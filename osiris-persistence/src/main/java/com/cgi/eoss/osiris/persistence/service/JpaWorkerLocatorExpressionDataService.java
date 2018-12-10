package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.WorkerLocatorExpression;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.WorkerLocatorExpressionDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.osiris.model.QWorkerLocatorExpression.workerLocatorExpression;

@Service
@Transactional(readOnly = true)
public class JpaWorkerLocatorExpressionDataService extends AbstractJpaDataService<WorkerLocatorExpression> implements WorkerLocatorExpressionDataService {

    private final WorkerLocatorExpressionDao workerLocatorExpressionDao;

    @Autowired
    public JpaWorkerLocatorExpressionDataService(WorkerLocatorExpressionDao workerLocatorExpressionDao) {
        this.workerLocatorExpressionDao = workerLocatorExpressionDao;
    }

    @Override
    OsirisEntityDao<WorkerLocatorExpression> getDao() {
        return workerLocatorExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(WorkerLocatorExpression entity) {
        return workerLocatorExpression.service.eq(entity.getService());
    }

    @Override
    public WorkerLocatorExpression getByService(OsirisService service) {
        return workerLocatorExpressionDao.findOneByService(service);
    }

}
