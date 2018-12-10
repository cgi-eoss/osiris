package com.cgi.eoss.osiris.api.controllers;

import java.util.List;

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

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortOsirisServiceTemplateFile;

@RepositoryRestResource(
        path = "serviceTemplateFiles",
        itemResourceRel = "serviceTemplateFile",
        collectionResourceRel = "serviceTemplateFiles",
        excerptProjection = ShortOsirisServiceTemplateFile.class
)
public interface ServiceTemplateFilesApi extends BaseRepositoryApi<OsirisServiceTemplateFile>, ServiceFilesApiCustom, PagingAndSortingRepository<OsirisServiceTemplateFile, Long> {

    @Override
    @RestResource(exported = false)
    List<OsirisServiceTemplateFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.serviceTemplate, 'read')")
    OsirisServiceTemplateFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplateFile.serviceTemplate, 'write')")
    <S extends OsirisServiceTemplateFile> S save(@P("serviceTemplateFile") S serviceTemplateFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends OsirisServiceTemplateFile> Iterable<S> save(@P("serviceTemplateFiles") Iterable<S> serviceTemplateFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.serviceTemplate, 'administration')")
    void delete(@P("serviceFile") OsirisServiceTemplateFile serviceFile);

    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceTemplate, 'read')")
    Page<OsirisServiceTemplateFile> findByServiceTemplate(@Param("serviceTemplate") OsirisServiceTemplate serviceTemplate, Pageable pageable);

    @Override
    @Query("select t from OsirisServiceTemplateFile t where t.serviceTemplate.owner=:owner")
    Page<OsirisServiceTemplateFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from OsirisServiceTemplateFile t where not t.serviceTemplate.owner=:owner")
    Page<OsirisServiceTemplateFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
