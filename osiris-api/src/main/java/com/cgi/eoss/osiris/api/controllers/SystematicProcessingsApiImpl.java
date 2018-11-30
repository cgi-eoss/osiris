package com.cgi.eoss.osiris.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.model.QSystematicProcessing;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.persistence.dao.SystematicProcessingDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class SystematicProcessingsApiImpl extends BaseRepositoryApiImpl<SystematicProcessing>  {

    private final OsirisSecurityService securityService;
    private final SystematicProcessingDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QSystematicProcessing.systematicProcessing.id;
    }

    @Override
    QUser getOwnerPath() {
        return QSystematicProcessing.systematicProcessing.owner;
    }

    @Override
    Class<SystematicProcessing> getEntityClass() {
        return SystematicProcessing.class;
    }

}
