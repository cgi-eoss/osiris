package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
import java.util.List;

public interface SystematicProcessingDataService extends OsirisEntityDataService<SystematicProcessing> {

    List<SystematicProcessing> findByStatus(Status s);


}
