package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QDefaultServiceTemplate.defaultServiceTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.DefaultServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.persistence.dao.DefaultServiceTemplateDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaDefaultServiceTemplateDataService extends AbstractJpaDataService<DefaultServiceTemplate> implements DefaultServiceTemplateDataService {

    private final DefaultServiceTemplateDao defaultServiceTemplateDao;

    @Autowired
    public JpaDefaultServiceTemplateDataService(DefaultServiceTemplateDao defaultServiceTemplateDao) {
        this.defaultServiceTemplateDao = defaultServiceTemplateDao;
    }

    @Override
    OsirisEntityDao<DefaultServiceTemplate> getDao() {
        return defaultServiceTemplateDao;
    }

    @Override
    Predicate getUniquePredicate(DefaultServiceTemplate entity) {
        return defaultServiceTemplate.serviceType.eq(entity.getServiceType());
    }

    @Override
    public DefaultServiceTemplate getByServiceType(OsirisService.Type serviceType) {
        return defaultServiceTemplateDao.getByServiceType(serviceType);
    }

}
