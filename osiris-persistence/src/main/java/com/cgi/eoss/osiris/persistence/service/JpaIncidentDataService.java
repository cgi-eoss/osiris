package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.IncidentDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.osiris.model.QIncident.incident;

@Service
@Transactional(readOnly = true)
public class JpaIncidentDataService extends AbstractJpaDataService<Incident> implements IncidentDataService {

    private final IncidentDao dao;

    @Autowired
    public JpaIncidentDataService(IncidentDao dao) {
        this.dao = dao;
    }

    @Override
    OsirisEntityDao<Incident> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Incident entity) {
        return incident.owner.eq(entity.getOwner())
                .and(incident.aoi.eq(entity.getAoi()))
                .and(incident.startDate.eq(entity.getStartDate()))
                .and(incident.endDate.eq(entity.getEndDate()))
                .and(incident.type.eq(entity.getType()));
    }

    @Override
    public List<Incident> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<Incident> findByType(IncidentType incidentType) {
        return dao.findByType(incidentType);
    }
}
