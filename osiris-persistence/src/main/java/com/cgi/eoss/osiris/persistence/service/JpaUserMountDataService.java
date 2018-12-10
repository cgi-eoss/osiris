package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QUserMount.userMount;
import com.cgi.eoss.osiris.model.UserMount;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.UserMountDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaUserMountDataService extends AbstractJpaDataService<UserMount>
        implements UserMountDataService {

    private final UserMountDao dao;

    @Autowired
    public JpaUserMountDataService(UserMountDao userMountDao) {
        this.dao = userMountDao;
    }
    
    @Override
    OsirisEntityDao<UserMount> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(UserMount entity) {
        return userMount.name.eq(entity.getName())
                .and(userMount.owner.eq(entity.getOwner()));
    }
  
}
