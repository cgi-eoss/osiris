package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.QIncident;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.IncidentDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class IncidentsApiImpl extends BaseRepositoryApiImpl<Incident> implements IncidentsApiCustom {

    private final OsirisSecurityService securityService;
    private final IncidentDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QIncident.incident.id;
    }

    @Override
    QUser getOwnerPath() {
        return QIncident.incident.owner;
    }

    @Override
    Class<Incident> getEntityClass() {
        return Incident.class;
    }

    @Override
    public Page<Incident> findByType(IncidentType type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(type), pageable);
    }

    private Predicate getFilterPredicate(IncidentType type) {
        return QIncident.incident.type.eq(type);
    }
}
