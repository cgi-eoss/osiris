package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface OsirisFileDao extends OsirisEntityDao<OsirisFile> {
    OsirisFile findOneByUri(URI uri);

    OsirisFile findOneByRestoId(UUID uuid);

    List<OsirisFile> findByOwner(User user);

    List<OsirisFile> findByType(OsirisFile.Type type);
}
