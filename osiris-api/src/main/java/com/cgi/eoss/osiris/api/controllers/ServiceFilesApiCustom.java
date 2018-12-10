package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisServiceContextFile;

public interface ServiceFilesApiCustom {
    <S extends OsirisServiceContextFile> S save(S service);
}
