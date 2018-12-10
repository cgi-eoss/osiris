package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;

public interface ServiceTemplateFilesApiCustom {
    <S extends OsirisServiceTemplateFile> S save(S service);
}
