package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;
import java.util.List;

public interface ServiceDataService extends
        OsirisEntityDataService<OsirisService>,
        SearchableDataService<OsirisService> {
    List<OsirisService> findByOwner(User user);

    OsirisService getByName(String serviceName);

    List<OsirisService> findAllAvailable();

    String computeServiceFingerprint(OsirisService osirisService);

}
