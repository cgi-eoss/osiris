package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.CostingExpression;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.persistence.dao.CostingExpressionDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.osiris.model.QCostingExpression.costingExpression;

@Service
@Transactional(readOnly = true)
public class JpaCostingExpressionDataService extends AbstractJpaDataService<CostingExpression> implements CostingExpressionDataService {

    private final CostingExpressionDao costingExpressionDao;

    @Autowired
    public JpaCostingExpressionDataService(CostingExpressionDao costingExpressionDao) {
        this.costingExpressionDao = costingExpressionDao;
    }

    @Override
    OsirisEntityDao<CostingExpression> getDao() {
        return costingExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(CostingExpression entity) {
        return costingExpression.type.eq(entity.getType()).and(costingExpression.associatedId.eq(entity.getAssociatedId()));
    }

    @Override
    public CostingExpression getServiceCostingExpression(OsirisService service) {
        return costingExpressionDao.findOne(
                costingExpression.type.eq(CostingExpression.Type.SERVICE)
                        .and(costingExpression.associatedId.eq(service.getId())));
    }

    @Override
    public CostingExpression getDownloadCostingExpression(OsirisFile osirisFile) {
        if (osirisFile.getDataSource() != null) {
            return costingExpressionDao.findOne(
                    costingExpression.type.eq(CostingExpression.Type.DOWNLOAD)
                            .and(costingExpression.associatedId.eq(osirisFile.getDataSource().getId())));
        }
        return null;
    }

}
