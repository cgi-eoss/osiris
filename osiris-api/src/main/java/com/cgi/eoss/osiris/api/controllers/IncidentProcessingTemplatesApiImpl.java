package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.QIncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.QUser;
import com.cgi.eoss.osiris.persistence.dao.IncidentProcessingTemplateDao;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class IncidentProcessingTemplatesApiImpl extends BaseRepositoryApiImpl<IncidentProcessingTemplate>  {

    private final OsirisSecurityService securityService;
    private final IncidentProcessingTemplateDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QIncidentProcessingTemplate.incidentProcessingTemplate.id;
    }

    @Override
    QUser getOwnerPath() {
        return QIncidentProcessingTemplate.incidentProcessingTemplate.owner;
    }

    @Override
    Class<IncidentProcessingTemplate> getEntityClass() {
        return IncidentProcessingTemplate.class;
    }
}
