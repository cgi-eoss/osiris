package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QOsirisServiceTemplateFile.osirisServiceTemplateFile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.OsirisServiceTemplate;
import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceTemplateFileDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaServiceTemplateFileDataService extends AbstractJpaDataService<OsirisServiceTemplateFile> implements ServiceTemplateFileDataService {

    private final OsirisServiceTemplateFileDao osirisServiceTemplateFileDao;

    @Autowired
    public JpaServiceTemplateFileDataService(OsirisServiceTemplateFileDao osirisServiceTemplateFileDao) {
        this.osirisServiceTemplateFileDao = osirisServiceTemplateFileDao;
    }

    @Override
    OsirisEntityDao<OsirisServiceTemplateFile> getDao() {
        return osirisServiceTemplateFileDao;
    }

    @Override
    Predicate getUniquePredicate(OsirisServiceTemplateFile entity) {
        return osirisServiceTemplateFile.serviceTemplate.eq(entity.getServiceTemplate()).and(osirisServiceTemplateFile.filename.eq(entity.getFilename()));
    }

    @Override
    public List<OsirisServiceTemplateFile> findByServiceTemplate(OsirisServiceTemplate serviceTemplate) {
        return osirisServiceTemplateFileDao.findByServiceTemplate(serviceTemplate);
    }

}
