package com.cgi.eoss.osiris.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisService.Type;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.User;

public interface ServiceTemplatesApiCustom {
	
    Page<OsirisServiceTemplate> findByFilterOnly(String filter, OsirisService.Type serviceType, Pageable pageable);

    Page<OsirisServiceTemplate> findByFilterAndOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable);

    Page<OsirisServiceTemplate> findByFilterAndNotOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable);

	OsirisServiceTemplate getDefaultByType(Type serviceType);

}
