package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortOsirisServiceTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

@RepositoryRestResource(path = "serviceTemplates", itemResourceRel = "serviceTemplate", collectionResourceRel = "serviceTemplates", excerptProjection = ShortOsirisServiceTemplate.class)
public interface ServiceTemplatesApi extends BaseRepositoryApi<OsirisServiceTemplate>, ServiceTemplatesApiCustom, JpaRepository<OsirisServiceTemplate, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    OsirisServiceTemplate findOne(Long id);

    @Override
    Page<OsirisServiceTemplate> findAll(Pageable pageable);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#serviceTemplate.id == null and hasRole('EXPERT_USER')) or (#serviceTemplate.id != null && hasPermission(#serviceTemplate, 'write'))")
    <S extends OsirisServiceTemplate> S save(@P("serviceTemplate") S serviceTemplate);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends OsirisServiceTemplate> serviceTemplates);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (!@osirisSecurityService.isPublic(#serviceTemplate.class, #serviceTemplate.id) and hasPermission(#serviceTemplate, 'administration'))")
    void delete(@P("serviceTemplate") OsirisServiceTemplate serviceTemplate);

    @Override
    @Query("select t from OsirisServiceTemplate t where t.owner=user")
    Page<OsirisServiceTemplate> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from OsirisServiceTemplate t where not t.owner=user")
    Page<OsirisServiceTemplate> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterOnly", rel="findByFilterOnly")
    @Query("select t from OsirisServiceTemplate t where (t.name like %:filter% or t.description like %:filter%) and (:serviceType is null or t.type = :serviceType)")
    Page<OsirisServiceTemplate> findByFilterOnly(@Param("filter") String filter, @Param("serviceType") @RequestParam(required = false) OsirisService.Type serviceType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from OsirisServiceTemplate t where t.owner=:owner and (t.name like %:filter% or t.description like %:filter%) and (:serviceType is null or t.type = :serviceType)")
    Page<OsirisServiceTemplate> findByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, @Param("serviceType") @RequestParam(required = false) OsirisService.Type serviceType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from OsirisServiceTemplate t where not t.owner=:owner and (t.name like %:filter% or t.description like %:filter%) and (:serviceType is null or t.type = :serviceType)")
    Page<OsirisServiceTemplate> findByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, @Param("serviceType") @RequestParam(required = false) OsirisService.Type serviceType, Pageable pageable);

    @Override
    @RestResource(path = "getDefaultByType", rel = "getDefaultByType")
    @Query("select t.serviceTemplate from DefaultServiceTemplate t where t.serviceType = :serviceType)")
    OsirisServiceTemplate getDefaultByType(@Param("serviceType") OsirisService.Type serviceType);


}
