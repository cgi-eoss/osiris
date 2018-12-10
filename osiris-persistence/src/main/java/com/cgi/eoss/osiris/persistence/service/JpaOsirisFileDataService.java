package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.eoss.osiris.model.QOsirisFile.osirisFile;

@Service
@Transactional(readOnly = true)
public class JpaOsirisFileDataService extends AbstractJpaDataService<OsirisFile> implements OsirisFileDataService {

    private final OsirisFileDao dao;

    @Autowired
    public JpaOsirisFileDataService(OsirisFileDao osirisFileDao) {
        this.dao = osirisFileDao;
    }

    @Override
    OsirisEntityDao<OsirisFile> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(OsirisFile entity) {
        return osirisFile.uri.eq(entity.getUri()).or(osirisFile.restoId.eq(entity.getRestoId()));
    }

    @Override
    public OsirisFile getByUri(URI uri) {
        return dao.findOneByUri(uri);
    }

    @Override
    public OsirisFile getByUri(String uri) {
        return getByUri(URI.create(uri));
    }

    @Override
    public OsirisFile getByRestoId(UUID uuid) {
        return dao.findOneByRestoId(uuid);
    }

    @Override
    public List<OsirisFile> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<OsirisFile> getByType(OsirisFile.Type type) {
        return dao.findByType(type);
    }

}
