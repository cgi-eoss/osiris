package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QOsirisService.osirisService;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceDao;
import com.querydsl.core.types.Predicate;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<OsirisService> implements ServiceDataService {

    private final OsirisServiceDao osirisServiceDao;

    private ServiceFileDataService serviceFilesDataService;

    @Autowired
    public JpaServiceDataService(OsirisServiceDao osirisServiceDao, ServiceFileDataService serviceFilesDataService) {
        this.osirisServiceDao = osirisServiceDao;
        this.serviceFilesDataService = serviceFilesDataService;
    }

    @Override
    OsirisEntityDao<OsirisService> getDao() {
        return osirisServiceDao;
    }

    @Override
    Predicate getUniquePredicate(OsirisService entity) {
        return osirisService.name.eq(entity.getName());
    }

    @Override
    public List<OsirisService> search(String term) {
        return osirisServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<OsirisService> findByOwner(User user) {
        return osirisServiceDao.findByOwner(user);
    }

    @Override
    public OsirisService getByName(String serviceName) {
        return osirisServiceDao.findOne(osirisService.name.eq(serviceName));
    }

    @Override
    public List<OsirisService> findAllAvailable() {
        return osirisServiceDao.findByStatus(OsirisService.Status.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public String computeServiceFingerprint(OsirisService osirisService) {
        ObjectOutputStream oos;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            oos = new ObjectOutputStream(bos);
            List<OsirisServiceContextFile> serviceFiles = serviceFilesDataService.findByService(osirisService);
            for (OsirisServiceContextFile contextFile : serviceFiles) {
                oos.writeObject(contextFile.getFilename());
                oos.writeObject(contextFile.getContent());
            }
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] serviceSerialized = bos.toByteArray();
            digest.update(serviceSerialized);
            String md5 = Hex.encodeHexString(digest.digest());
            return md5;

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }

    }

}
