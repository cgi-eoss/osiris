package com.cgi.eoss.osiris.persistence.dao;

import java.util.List;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;

public interface OsirisServiceTemplateFileDao extends OsirisEntityDao<OsirisServiceTemplateFile> {
    List<OsirisServiceTemplateFile> findByServiceTemplate(OsirisServiceTemplate serviceTemplate);
}
