package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.QOsirisFile;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisFileDao;
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

import java.io.IOException;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class OsirisFilesApiImpl extends BaseRepositoryApiImpl<OsirisFile> implements OsirisFilesApiCustom {

    private final OsirisSecurityService securityService;
    private final OsirisFileDao dao;
    private final CatalogueService catalogueService;

    @Override
    NumberPath<Long> getIdPath() {
        return QOsirisFile.osirisFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QOsirisFile.osirisFile.owner;
    }

    @Override
    Class<OsirisFile> getEntityClass() {
        return OsirisFile.class;
    }

    @Override
    public void delete(OsirisFile osirisFile) {
        try {
            catalogueService.delete(osirisFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<OsirisFile> findByType(OsirisFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, type), pageable);
    }

    @Override
    public Page<OsirisFile> findByFilterOnly(String filter, OsirisFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type), pageable);
    }

    @Override
    public Page<OsirisFile> findByFilterAndOwner(String filter, OsirisFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, type)).and(QOsirisFile.osirisFile.type.eq(type)),
                pageable);
    }

    @Override
    public Page<OsirisFile> findByFilterAndNotOwner(String filter, OsirisFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user)
                        .and(getFilterPredicate(filter, type)).and(QOsirisFile.osirisFile.type.eq(type)),
                pageable);
    }

    private Predicate getFilterPredicate(String filter, OsirisFile.Type type) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QOsirisFile.osirisFile.filename.containsIgnoreCase(filter));
        }

        if (type != null) {
            builder.and(QOsirisFile.osirisFile.type.eq(type));
        }

        return builder.getValue();
    }

}
