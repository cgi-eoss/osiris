package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDataService extends
        OsirisEntityDataService<WorkerLocatorExpression> {
    WorkerLocatorExpression getByService(OsirisService service);
}
