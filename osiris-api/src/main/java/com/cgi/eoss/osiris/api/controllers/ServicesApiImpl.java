package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.QOsirisService;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceDao;
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

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServicesApiImpl extends BaseRepositoryApiImpl<OsirisService> implements ServicesApiCustom {

    private final OsirisSecurityService securityService;
    private final OsirisServiceDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QOsirisService.osirisService.id;
    }

    @Override
    QUser getOwnerPath() {
        return QOsirisService.osirisService.owner;
    }

    @Override
    Class<OsirisService> getEntityClass() {
        return OsirisService.class;
    }

    @Override
    public Page<OsirisService> findByFilterOnly(String filter, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, serviceType), pageable);
    }

    @Override
    public Page<OsirisService> findByFilterAndOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    @Override
    public Page<OsirisService> findByFilterAndNotOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    private Predicate getFilterPredicate(String filter, OsirisService.Type serviceType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QOsirisService.osirisService.name.containsIgnoreCase(filter).or(QOsirisService.osirisService.description.containsIgnoreCase(filter)));
        }

        if (serviceType != null) {
            builder.and(QOsirisService.osirisService.type.eq(serviceType));
        }

        return builder.getValue();
    }

}
