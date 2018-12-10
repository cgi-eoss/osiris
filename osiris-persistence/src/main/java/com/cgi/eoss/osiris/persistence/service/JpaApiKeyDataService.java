package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.ApiKey;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.ApiKeyDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.cgi.eoss.osiris.model.QApiKey.apiKey;

@Service
@Transactional(readOnly = true)
public class JpaApiKeyDataService extends AbstractJpaDataService<ApiKey> implements ApiKeyDataService {

    private final ApiKeyDao dao;

    @Autowired
    public JpaApiKeyDataService(ApiKeyDao apiKeyDao) {
        this.dao = apiKeyDao;
    }

    @Override
    OsirisEntityDao<ApiKey> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(ApiKey entity) {
        return apiKey.owner.eq(entity.getOwner());
    }

    @Override
    public ApiKey getByOwner(User user) {
        return dao.getByOwner(user);
    }

}
