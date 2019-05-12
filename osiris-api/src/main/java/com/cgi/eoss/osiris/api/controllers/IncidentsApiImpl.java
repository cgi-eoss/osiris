package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.QIncident;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.SystematicProcessing;
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
    @Deprecated
    public Page<Incident> findByType(IncidentType type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, type), pageable);
    }

    @Override
    @Deprecated
    public Page<Incident> findByFilterOnly(String filter, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, incidentType), pageable);
    }

    @Override
    @Deprecated
    public Page<Incident> findByFilterAndOwner(String filter, User user, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, incidentType)), pageable);
    }

    @Override
    @Deprecated
    public Page<Incident> findByFilterAndNotOwner(String filter, User user, IncidentType incidentType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, incidentType)), pageable);
    }

    /**
     * Returns any Incidents that have their active time matched by the search date range.
     */
    @Override
    @Deprecated
    public Page<Incident> findByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, null, null, null,
                startDate, endDate, null, null), pageable);
    }

    /**
     * Returns any Incidents that belong to a collection via an incident process.
     */
    @Override
    @Deprecated
    public Page<Incident> findByCollection(Collection collection, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, null, null, null, null, null,
                collection, null), pageable);
    }

    @Override
    @Deprecated
    public Page<Incident> findBySystematicProcessing(SystematicProcessing systematicProcessing, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, null, null, null, null, null,null,
                systematicProcessing), pageable);
    }

    /**
     * Method that takes all potential search parameters, and returns the appropirate Page of Incidents that match.
     * It uses a Filtered Predicate to minimise the amount of queries sent to the database.
     * @param owner
     * @param notOwner
     * @param incidentType
     * @param filter
     * @param startDate
     * @param endDate
     * @param collection
     * @param systematicProcessing
     * @param pageable
     * @return
     */
    @Override
    public Page<Incident> parametricFind(User owner, User notOwner,
                          IncidentType incidentType,
                          String filter,
                          Instant startDate, Instant endDate,
                          Collection collection,
                          SystematicProcessing systematicProcessing,
                                 Pageable pageable) {
        return getFilteredResults(getFilterPredicate(owner, notOwner, incidentType, filter, startDate, endDate, collection, systematicProcessing), pageable);
    };

    @Deprecated
    private Predicate getFilterPredicate(String filter, IncidentType incidentType) {
        return getFilterPredicate(null, null, incidentType, filter, null, null, null, null);
    }

    private Predicate getFilterPredicate(User owner, User notOwner,
                                         IncidentType incidentType,
                                         String filter,
                                         Instant startDate, Instant endDate,
                                         Collection collection,
                                         SystematicProcessing systematicProcessing) {
    	BooleanBuilder builder = new BooleanBuilder();
    	if (owner != null) {
			builder.and(getOwnerPath().eq(owner));
		}
		if (notOwner !=null) {
			builder.and(getOwnerPath().ne(notOwner));
		}
		if (incidentType != null) {
        	builder.and(QIncident.incident.type.eq(incidentType));
        }
        if (!Strings.isNullOrEmpty(filter)) { 
        	builder.and(QIncident.incident.title.containsIgnoreCase(filter).or(QIncident.incident.description.containsIgnoreCase(filter)));
        }
        if (startDate != null) {  
        	builder.andNot(QIncident.incident.endDate.before(startDate));
        }
        if (endDate != null) {
        	builder.andNot(QIncident.incident.startDate.after(endDate));
        }
        if (collection != null) { 
        	builder.and(QIncident.incident.incidentProcessings.any().collection.eq(collection));
        }
        if (systematicProcessing != null) {
        	builder.and(QIncident.incident.incidentProcessings.any().systematicProcessing.eq(systematicProcessing));
        }
        return builder.getValue();
    }
}
