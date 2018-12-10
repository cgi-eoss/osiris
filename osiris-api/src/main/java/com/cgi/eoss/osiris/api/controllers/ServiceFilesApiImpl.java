package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.QOsirisServiceContextFile;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceContextFileDao;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceFilesApiImpl extends BaseRepositoryApiImpl<OsirisServiceContextFile> implements ServiceFilesApiCustom {

    private final OsirisSecurityService securityService;
    private final OsirisServiceContextFileDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QOsirisServiceContextFile.osirisServiceContextFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QOsirisServiceContextFile.osirisServiceContextFile.service.owner;
    }

    @Override
    Class<OsirisServiceContextFile> getEntityClass() {
        return OsirisServiceContextFile.class;
    }

    @Override
    public <S extends OsirisServiceContextFile> S save(S serviceFile) {
        // Transform base64 content into real content
        serviceFile.setContent(new String(BaseEncoding.base64().decode(serviceFile.getContent())));
        return getDao().save(serviceFile);
    }

}
