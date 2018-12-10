package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;

public interface DefaultServiceTemplateDao extends OsirisEntityDao<DefaultServiceTemplate> {
	
	public DefaultServiceTemplate getByServiceType(OsirisService.Type type);

}
