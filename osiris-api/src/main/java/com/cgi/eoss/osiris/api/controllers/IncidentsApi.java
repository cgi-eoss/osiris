package com.cgi.eoss.osiris.api.controllers;

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
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

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
    
}
