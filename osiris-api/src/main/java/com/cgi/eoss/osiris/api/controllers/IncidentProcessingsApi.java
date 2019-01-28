package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortIncidentProcessing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "incidentProcessings", itemResourceRel = "incidentProcessing", collectionResourceRel = "incidentProcessings", excerptProjection = ShortIncidentProcessing.class)
public interface IncidentProcessingsApi extends BaseRepositoryApi<IncidentProcessing>, PagingAndSortingRepository<IncidentProcessing, Long> {
    @Override
    @Query("select t from IncidentProcessing t where t.owner=user")
    Page<IncidentProcessing> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from IncidentProcessing t where not t.owner=user")
    Page<IncidentProcessing> findByNotOwner(@Param("owner") User user, Pageable pageable);
}
