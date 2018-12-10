package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.CostingExpression;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;

public interface CostingExpressionDataService extends
        OsirisEntityDataService<CostingExpression> {
    CostingExpression getServiceCostingExpression(OsirisService service);
    CostingExpression getDownloadCostingExpression(OsirisFile osirisFile);
}
