package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.QIncidentProcessing;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.IncidentProcessingDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class IncidentProcessingsApiImpl extends BaseRepositoryApiImpl<IncidentProcessing>  {

    private final OsirisSecurityService securityService;
    private final IncidentProcessingDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QIncidentProcessing.incidentProcessing.id;
    }

    @Override
    QUser getOwnerPath() {
        return QIncidentProcessing.incidentProcessing.owner;
    }

    @Override
    Class<IncidentProcessing> getEntityClass() {
        return IncidentProcessing.class;
    }
}
