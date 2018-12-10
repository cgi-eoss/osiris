package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface OsirisFileDataService extends
        OsirisEntityDataService<OsirisFile> {
    OsirisFile getByUri(URI uri);

    OsirisFile getByUri(String uri);

    OsirisFile getByRestoId(UUID uuid);

    List<OsirisFile> findByOwner(User user);

    List<OsirisFile> getByType(OsirisFile.Type type);
}
