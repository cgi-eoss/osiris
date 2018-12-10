package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QUserPreference.userPreference;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserPreference;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.UserPreferenceDao;
import com.querydsl.core.types.Predicate;;

@Service
@Transactional(readOnly = true)
public class JpaUserPreferenceDataService extends AbstractJpaDataService<UserPreference>
        implements UserPreferenceDataService {

    private final UserPreferenceDao dao;

    @Autowired
    public JpaUserPreferenceDataService(UserPreferenceDao userPreferenceDao) {
        this.dao = userPreferenceDao;
    }

    @Override
    OsirisEntityDao<UserPreference> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(UserPreference entity) {
        return userPreference.name.eq(entity.getName())
                .and(userPreference.owner.eq(entity.getOwner()));
    }

    @Override
    public UserPreference getByNameAndOwner(String name, User user) {
        return dao.findOneByNameAndOwner(name, user);
    }

    @Override
    public List<UserPreference> findByTypeAndOwner(String type, User user) {
        return dao.findByTypeAndOwner(type, user);
    }
}
