package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OsirisFilesApiCustom {
    void delete(OsirisFile osirisFile);

    Page<OsirisFile> findByType(OsirisFile.Type type, Pageable pageable);

    Page<OsirisFile> findByFilterOnly(String filter, OsirisFile.Type type, Pageable pageable);

    Page<OsirisFile> findByFilterAndOwner(String filter, OsirisFile.Type type, User user, Pageable pageable);

    Page<OsirisFile> findByFilterAndNotOwner(String filter, OsirisFile.Type type, User user, Pageable pageable);
}
