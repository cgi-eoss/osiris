package com.cgi.eoss.osiris.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;
import com.cgi.eoss.osiris.model.QOsirisServiceTemplateFile;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceTemplateFileDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceTemplateFilesApiImpl extends BaseRepositoryApiImpl<OsirisServiceTemplateFile> implements ServiceTemplateFilesApiCustom {

    private final OsirisSecurityService securityService;
    private final OsirisServiceTemplateFileDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QOsirisServiceTemplateFile.osirisServiceTemplateFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QOsirisServiceTemplateFile.osirisServiceTemplateFile.serviceTemplate.owner;
    }

    @Override
    Class<OsirisServiceTemplateFile> getEntityClass() {
        return OsirisServiceTemplateFile.class;
    }

    @Override
    public <S extends OsirisServiceTemplateFile> S save(S serviceTemplateFile) {
        // Transform base64 content into real content
    	serviceTemplateFile.setContent(new String(BaseEncoding.base64().decode(serviceTemplateFile.getContent())));
        return getDao().save(serviceTemplateFile);
    }

}
