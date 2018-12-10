package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;

import java.util.List;

public interface ServiceFileDataService extends
        OsirisEntityDataService<OsirisServiceContextFile> {
    List<OsirisServiceContextFile> findByService(OsirisService service);
}
