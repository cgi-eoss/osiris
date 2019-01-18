package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.QIncidentType;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.IncidentTypeDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class IncidentTypesApiImpl extends BaseRepositoryApiImpl<IncidentType> {

    private final OsirisSecurityService securityService;
    private final IncidentTypeDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QIncidentType.incidentType.id;
    }

    @Override
    QUser getOwnerPath() {
        return QIncidentType.incidentType.owner;
    }

    @Override
    Class<IncidentType> getEntityClass() {
        return IncidentType.class;
    }

}
