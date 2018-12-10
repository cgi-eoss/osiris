package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QOsirisServiceTemplate.osirisServiceTemplate;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceTemplateDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaServiceTemplateDataService extends AbstractJpaDataService<OsirisServiceTemplate> implements ServiceTemplateDataService {

    private final OsirisServiceTemplateDao osirisServiceTemplateDao;

    @Autowired
    public JpaServiceTemplateDataService(OsirisServiceTemplateDao osirisServiceTemplateDao) {
        this.osirisServiceTemplateDao = osirisServiceTemplateDao;
    }

    @Override
    OsirisEntityDao<OsirisServiceTemplate> getDao() {
        return osirisServiceTemplateDao;
    }

    @Override
    Predicate getUniquePredicate(OsirisServiceTemplate entity) {
        return osirisServiceTemplate.name.eq(entity.getName());
    }

    @Override
    public List<OsirisServiceTemplate> search(String term) {
        return osirisServiceTemplateDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<OsirisServiceTemplate> findByOwner(User user) {
        return osirisServiceTemplateDao.findByOwner(user);
    }

    @Override
    public OsirisServiceTemplate getByName(String serviceTemplateName) {
        return osirisServiceTemplateDao.findOne(osirisServiceTemplate.name.eq(serviceTemplateName));
    }

}
