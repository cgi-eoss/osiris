package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.DownloaderCredentials;
import com.cgi.eoss.osiris.persistence.dao.DownloaderCredentialsDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.osiris.model.QDownloaderCredentials.downloaderCredentials;

@Service
@Transactional(readOnly = true)
public class JpaDownloaderCredentialsDataService extends AbstractJpaDataService<DownloaderCredentials> implements DownloaderCredentialsDataService {

    private final DownloaderCredentialsDao downloaderCredentialsDao;

    @Autowired
    public JpaDownloaderCredentialsDataService(DownloaderCredentialsDao downloaderCredentialsDao) {
        this.downloaderCredentialsDao = downloaderCredentialsDao;
    }

    @Override
    OsirisEntityDao<DownloaderCredentials> getDao() {
        return downloaderCredentialsDao;
    }

    @Override
    Predicate getUniquePredicate(DownloaderCredentials entity) {
        return downloaderCredentials.host.eq(entity.getHost());
    }

    @Override
    public DownloaderCredentials getByHost(String host) {
        return downloaderCredentialsDao.findOne(downloaderCredentials.host.eq(host));
    }

}
