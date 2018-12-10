package com.cgi.eoss.osiris.persistence.service;

import java.util.List;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;

public interface ServiceTemplateFileDataService extends
        OsirisEntityDataService<OsirisServiceTemplateFile> {
    List<OsirisServiceTemplateFile> findByServiceTemplate(OsirisServiceTemplate serviceTemplate);
}
