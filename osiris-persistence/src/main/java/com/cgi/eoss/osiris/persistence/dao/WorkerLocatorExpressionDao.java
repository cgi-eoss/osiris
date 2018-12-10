package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDao extends OsirisEntityDao<WorkerLocatorExpression> {
    WorkerLocatorExpression findOneByService(OsirisService service);
}
