package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortOsirisServiceContextFile;
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

import java.util.List;

@RepositoryRestResource(
        path = "serviceFiles",
        itemResourceRel = "serviceFile",
        collectionResourceRel = "serviceFiles",
        excerptProjection = ShortOsirisServiceContextFile.class
)
public interface ServiceFilesApi extends BaseRepositoryApi<OsirisServiceContextFile>, ServiceFilesApiCustom, PagingAndSortingRepository<OsirisServiceContextFile, Long> {

    @Override
    @RestResource(exported = false)
    List<OsirisServiceContextFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.service, 'write')")
    OsirisServiceContextFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'write')")
    <S extends OsirisServiceContextFile> S save(@P("serviceFile") S serviceFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends OsirisServiceContextFile> Iterable<S> save(@P("serviceFiles") Iterable<S> serviceFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'administration')")
    void delete(@P("serviceFile") OsirisServiceContextFile serviceFile);

    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'write')")
    Page<OsirisServiceContextFile> findByService(@Param("service") OsirisService service, Pageable pageable);

    @Override
    @Query("select t from OsirisServiceContextFile t where t.service.owner=:owner")
    Page<OsirisServiceContextFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from OsirisServiceContextFile t where not t.service.owner=:owner")
    Page<OsirisServiceContextFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
