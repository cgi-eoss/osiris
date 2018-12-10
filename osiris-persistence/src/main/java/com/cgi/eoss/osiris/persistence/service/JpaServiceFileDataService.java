package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceContextFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.osiris.model.QOsirisServiceContextFile.osirisServiceContextFile;

@Service
@Transactional(readOnly = true)
public class JpaServiceFileDataService extends AbstractJpaDataService<OsirisServiceContextFile> implements ServiceFileDataService {

    private final OsirisServiceContextFileDao osirisServiceContextFileDao;

    @Autowired
    public JpaServiceFileDataService(OsirisServiceContextFileDao osirisServiceContextFileDao) {
        this.osirisServiceContextFileDao = osirisServiceContextFileDao;
    }

    @Override
    OsirisEntityDao<OsirisServiceContextFile> getDao() {
        return osirisServiceContextFileDao;
    }

    @Override
    Predicate getUniquePredicate(OsirisServiceContextFile entity) {
        return osirisServiceContextFile.service.eq(entity.getService()).and(osirisServiceContextFile.filename.eq(entity.getFilename()));
    }

    @Override
    public List<OsirisServiceContextFile> findByService(OsirisService service) {
        return osirisServiceContextFileDao.findByService(service);
    }

}
