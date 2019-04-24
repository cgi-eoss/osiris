package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.GeoserverLayer;
import com.cgi.eoss.osiris.model.OsirisFile;

public interface GeoserverLayerDataService extends
        OsirisEntityDataService<GeoserverLayer> {

    void syncGeoserverLayers(OsirisFile osirisFile);

}
