package com.cgi.eoss.osiris.api.controllers;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisService.Type;
import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.QOsirisServiceTemplate;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceTemplateDao;
import com.cgi.eoss.osiris.persistence.service.DefaultServiceTemplateDataService;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceTemplatesApiImpl extends BaseRepositoryApiImpl<OsirisServiceTemplate> implements ServiceTemplatesApiCustom {

    private final OsirisSecurityService securityService;
    private final OsirisServiceTemplateDao dao;
    private final DefaultServiceTemplateDataService defaultTemplateDataService;
    

    @Override
    NumberPath<Long> getIdPath() {
        return QOsirisServiceTemplate.osirisServiceTemplate.id;
    }
    
    @Override
    QUser getOwnerPath() {
        return QOsirisServiceTemplate.osirisServiceTemplate.owner;
    }

    @Override
    Class<OsirisServiceTemplate> getEntityClass() {
        return OsirisServiceTemplate.class;
    }

    @Override
    public Page<OsirisServiceTemplate> findByFilterOnly(String filter, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, serviceType), pageable);
    }

    @Override
    public Page<OsirisServiceTemplate> findByFilterAndOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    @Override
    public Page<OsirisServiceTemplate> findByFilterAndNotOwner(String filter, User user, OsirisService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    private Predicate getFilterPredicate(String filter, OsirisService.Type serviceType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QOsirisServiceTemplate.osirisServiceTemplate.name.containsIgnoreCase(filter).or(QOsirisServiceTemplate.osirisServiceTemplate.description.containsIgnoreCase(filter)));
        }

        if (serviceType != null) {
            builder.and(QOsirisServiceTemplate.osirisServiceTemplate.type.eq(serviceType));
        }

        return builder.getValue();
    }

	@Override
	public OsirisServiceTemplate getDefaultByType(Type serviceType) {
		DefaultServiceTemplate defaultOsirisServiceTemplate = defaultTemplateDataService.getByServiceType(serviceType);
		if (defaultOsirisServiceTemplate != null) {
			return defaultOsirisServiceTemplate.getServiceTemplate();
		}   
		
		Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
        BooleanExpression isVisibleAndOfType = getIdPath().in(visibleIds).and(QOsirisServiceTemplate.osirisServiceTemplate.type.eq(serviceType));
        return getDao().findAll(isVisibleAndOfType).stream().findFirst().orElse(null);
	}
}
