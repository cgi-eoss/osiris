package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;

import java.util.List;

public interface OsirisServiceContextFileDao extends OsirisEntityDao<OsirisServiceContextFile> {
    List<OsirisServiceContextFile> findByService(OsirisService service);
}
