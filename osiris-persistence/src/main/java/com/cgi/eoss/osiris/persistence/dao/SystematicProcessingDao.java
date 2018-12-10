package com.cgi.eoss.osiris.persistence.dao;

import java.util.List;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.User;

public interface SystematicProcessingDao extends OsirisEntityDao<SystematicProcessing> {

    List<SystematicProcessing> findByOwner(User user);


}
