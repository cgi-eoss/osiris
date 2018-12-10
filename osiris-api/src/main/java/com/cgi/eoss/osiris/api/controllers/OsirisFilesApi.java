package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.projections.ShortOsirisFile;
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

import java.net.URI;
import java.util.UUID;

@RepositoryRestResource(path = "osirisFiles", itemResourceRel = "osirisFile", collectionResourceRel = "osirisFiles", excerptProjection = ShortOsirisFile.class)
public interface OsirisFilesApi extends BaseRepositoryApi<OsirisFile>, OsirisFilesApiCustom, PagingAndSortingRepository<OsirisFile, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    OsirisFile findOne(Long id);

    @Override
    @RestResource(exported = false)
    <S extends OsirisFile> Iterable<S> save(Iterable<S> osirisFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#osirisFile.id != null and hasPermission(#osirisFile, 'write'))")
    <S extends OsirisFile> S save(@P("osirisFile") S osirisFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends OsirisFile> osirisFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#osirisFile, 'administration')")
    void delete(@P("osirisFile") OsirisFile osirisFile);

    @Override
    @Query("select f from OsirisFile f where f.type=type")
    Page<OsirisFile> findByType(@Param("type") OsirisFile.Type type, Pageable pageable);

    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    OsirisFile findOneByUri(@Param("uri") URI uri);

    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    OsirisFile findOneByRestoId(@Param("uuid") UUID uuid);

    @Override
    @Query("select t from OsirisFile t where t.owner=user")
    Page<OsirisFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from OsirisFile t where not t.owner=user")
    Page<OsirisFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    @Query("select t from OsirisFile t where t.filename like %:filter% and t.type=:type")
    Page<OsirisFile> findByFilterOnly(@Param("filter") String filter, @Param("type") OsirisFile.Type type, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from OsirisFile t where t.owner=:owner and t.filename like %:filter% and t.type=:type")
    Page<OsirisFile> findByFilterAndOwner(@Param("filter") String filter, @Param("type") OsirisFile.Type type, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from OsirisFile t where not t.owner=:owner and t.filename like %:filter% and t.type=:type")
    Page<OsirisFile> findByFilterAndNotOwner(@Param("filter") String filter, @Param("type") OsirisFile.Type type, @Param("owner") User user, Pageable pageable);
}
