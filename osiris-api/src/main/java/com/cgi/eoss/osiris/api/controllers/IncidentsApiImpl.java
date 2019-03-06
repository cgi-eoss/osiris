package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.QIncident;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.IncidentDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
        return getFilteredResults(getFilterPredicate(null, type), pageable);
    }

    @Override
    public Page<Incident> findByFilterOnly(String filter, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, incidentType), pageable);
    }

    @Override
    public Page<Incident> findByFilterAndOwner(String filter, User user, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, incidentType)), pageable);
    }

    @Override
    public Page<Incident> findByFilterAndNotOwner(String filter, User user, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, incidentType)), pageable);
    }

    /**
     * Returns any Incidents that have their active time matched by the search date range.
     */
    @Override
    public Page<Incident> findByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return getFilteredResults(QIncident.incident
                    .startDate.between(startDate, endDate)
                .or(QIncident.incident
                    .endDate.between(startDate, endDate)
                .or(QIncident.incident
                    .startDate.before(startDate)
                    .and(QIncident.incident
                    .endDate.after(endDate))))
            , pageable);
    }

    private Predicate getFilterPredicate(String filter, IncidentType incidentType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QIncident.incident.title.containsIgnoreCase(filter).or(QIncident.incident.description.containsIgnoreCase(filter)));
        }

        if (incidentType != null) {
            builder.and(QIncident.incident.type.eq(incidentType));
        }

        return builder.getValue();
    }
}
