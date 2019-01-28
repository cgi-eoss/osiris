package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.IncidentProcessingDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.osiris.model.QIncidentProcessing.incidentProcessing;

@Service
@Transactional(readOnly = true)
public class JpaIncidentProcessingDataService extends AbstractJpaDataService<IncidentProcessing> implements IncidentProcessingDataService {

    private final IncidentProcessingDao dao;

    @Autowired
    public JpaIncidentProcessingDataService(IncidentProcessingDao dao) {
        this.dao = dao;
    }

    @Override
    OsirisEntityDao<IncidentProcessing> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(IncidentProcessing entity) {
        return incidentProcessing.owner.eq(entity.getOwner())
                .and(incidentProcessing.incident.eq(entity.getIncident()))
                .and(incidentProcessing.inputs.eq(entity.getInputs()))
                .and(incidentProcessing.searchParameters.eq(entity.getSearchParameters()))
                .and(incidentProcessing.template.eq(entity.getTemplate()));
    }

    @Override
    public List<IncidentProcessing> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<IncidentProcessing> findByTemplate(IncidentProcessingTemplate incidentProcessingTemplate) {
        return dao.findByTemplate(incidentProcessingTemplate);
    }
}
