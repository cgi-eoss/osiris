package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;

public interface DefaultServiceTemplateDataService extends
        OsirisEntityDataService<DefaultServiceTemplate> {

	DefaultServiceTemplate getByServiceType(OsirisService.Type serviceType);

}
