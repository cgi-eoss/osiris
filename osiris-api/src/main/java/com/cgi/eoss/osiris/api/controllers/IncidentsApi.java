package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@RepositoryRestResource(path = "incidents", itemResourceRel = "incident", collectionResourceRel = "incidents", excerptProjection = ShortIncident.class)
public interface IncidentsApi extends BaseRepositoryApi<Incident>, IncidentsApiCustom, PagingAndSortingRepository<Incident, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    Incident findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends Incident> incidents);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'write')")
    void delete(@P("incident") Incident incident);

    @Override
    @Query("select t from Incident t where t.owner=user")
    Page<Incident> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Incident t where not t.owner=user")
    Page<Incident> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select i from Incident i where i.type=type")
    Page<Incident> findByType(@Param("type") IncidentType type, Pageable pageable);

    @Override
    @RestResource(path="findByFilterOnly", rel="findByFilterOnly")
    @Query("select t from Incident t where (t.title like %:filter% or t.description like %:filter%) and (:incidentType is null or t.type = :incidentType)")
    Page<Incident> findByFilterOnly(@Param("filter") String filter, @Param("incidentType") @RequestParam(required = false) IncidentType incidentType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from Incident t where t.owner=:owner and (t.title like %:filter% or t.description like %:filter%) and (:incidentType is null or t.type = :incidentType)")
    Page<Incident> findByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, @Param("incidentType") @RequestParam(required = false) IncidentType incidentType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from Incident t where not t.owner=:owner and (t.title like %:filter% or t.description like %:filter%) and (:incidentType is null or t.type = :incidentType)")
    Page<Incident> findByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, @Param("incidentType") @RequestParam(required = false) IncidentType incidentType, Pageable pageable);

    @Override
    @RestResource(path = "findByDateRange", rel = "findByDateRange")
    @Query("select t from Incident t where ((:startDate <= t.startDate AND t.startDate <= :endDate) OR (:startDate <= t.endDate AND t.endDate <= :endDate) OR (t.startDate < :startDate AND :endDate < t.endDate))")
    Page<Incident> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate, Pageable pageable);

    @Override
    @RestResource(path = "findByCollection", rel = "findByCollection")
    @Query("SELECT i FROM Incident i JOIN IncidentProcessing ip WHERE ip.collection = :collection")
    Page<Incident> findByCollection(@Param("collection") Collection collection, Pageable pageable);
}
