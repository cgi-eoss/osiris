package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.QIncidentType;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.IncidentTypeDao;
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
public class IncidentTypesApiImpl extends BaseRepositoryApiImpl<IncidentType> implements IncidentTypesApiCustom {

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

    @Override
    public Page<IncidentType> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter), pageable);
    }

    @Override
    public Page<IncidentType> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter)), pageable);
    }

    @Override
    public Page<IncidentType> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter)), pageable);
    }

    private Predicate getFilterPredicate(String filter) {
        return QIncidentType.incidentType.title.containsIgnoreCase(filter).or(QIncidentType.incidentType.description.containsIgnoreCase(filter));
    }
}
