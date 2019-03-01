package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QGeoserverLayer.geoserverLayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.GeoserverLayer;
import com.cgi.eoss.osiris.persistence.dao.GeoserverLayerDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaGeoserverLayerDataService extends AbstractJpaDataService<GeoserverLayer> implements GeoserverLayerDataService {

    private final GeoserverLayerDao dao;

    @Autowired
    public JpaGeoserverLayerDataService(GeoserverLayerDao dao) {
        this.dao = dao;
    }

    @Override
    OsirisEntityDao<GeoserverLayer> getDao() {
        return dao;
    }

	@Override
	Predicate getUniquePredicate(GeoserverLayer entity) {
	     return geoserverLayer.workspace.eq(entity.getWorkspace()).and(geoserverLayer.layer.eq(entity.getLayer()));
	}

   

}
