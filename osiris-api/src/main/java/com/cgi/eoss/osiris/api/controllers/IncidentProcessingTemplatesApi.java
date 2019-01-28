package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortIncidentProcessingTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "incidentProcessingTemplates", itemResourceRel = "incidentProcessingTemplate", collectionResourceRel = "incidentProcessingTemplates", excerptProjection = ShortIncidentProcessingTemplate.class)
public interface IncidentProcessingTemplatesApi extends BaseRepositoryApi<IncidentProcessingTemplate>, PagingAndSortingRepository<IncidentProcessingTemplate, Long> {
    @Override
    @Query("select t from IncidentProcessingTemplate t where t.owner=user")
    Page<IncidentProcessingTemplate> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from IncidentProcessingTemplate t where not t.owner=user")
    Page<IncidentProcessingTemplate> findByNotOwner(@Param("owner") User user, Pageable pageable);
}
