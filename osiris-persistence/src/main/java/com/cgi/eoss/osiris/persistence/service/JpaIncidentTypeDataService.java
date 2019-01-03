package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.IncidentTypeDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.osiris.model.QIncidentType.incidentType;

@Service
@Transactional(readOnly = true)
public class JpaIncidentTypeDataService extends AbstractJpaDataService<IncidentType> implements IncidentTypeDataService {

    private final IncidentTypeDao dao;

    @Autowired
    public JpaIncidentTypeDataService(IncidentTypeDao dao) {
        this.dao = dao;
    }

    @Override
    OsirisEntityDao<IncidentType> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(IncidentType entity) {
        return incidentType.owner.eq(entity.getOwner())
                .and(incidentType.title.eq(entity.getTitle()));
    }

    @Override
    public List<IncidentType> findByOwner(User user) {
        return dao.findByOwner(user);
    }
}
