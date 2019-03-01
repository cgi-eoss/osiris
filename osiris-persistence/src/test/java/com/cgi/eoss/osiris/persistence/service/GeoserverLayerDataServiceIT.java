package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.GeoserverLayer;
import com.cgi.eoss.osiris.model.GeoserverLayer.StoreType;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class GeoserverLayerDataServiceIT {
    @Autowired
    private OsirisFileDataService fileDataService;
    
    @Autowired
    private GeoserverLayerDataService geoserverLayerDataService;
    
    
    @Autowired
    private UserDataService userService;

    @Test
    public void testSaveGeoserverLayer() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        OsirisFile osirisFile = new OsirisFile();
        osirisFile.setUri(URI.create("osiris://osirisFile"));
        osirisFile.setRestoId(UUID.randomUUID());
        osirisFile.setOwner(owner);
        
        GeoserverLayer geoserverLayer1 = new GeoserverLayer(owner, "test1", "test", StoreType.MOSAIC);
        geoserverLayer1 = geoserverLayerDataService.save(geoserverLayer1);
        GeoserverLayer geoserverLayer2 = new GeoserverLayer(owner, "test", "test", StoreType.MOSAIC);
        geoserverLayer2 = geoserverLayerDataService.save(geoserverLayer2);
        
        GeoserverLayer geoserverLayer = new GeoserverLayer(owner, "test", "test", StoreType.MOSAIC);
        osirisFile.getGeoserverLayers().add(geoserverLayer);
        Collection<GeoserverLayer> syncedLayers = geoserverLayerDataService.save(osirisFile.getGeoserverLayers());
        osirisFile.getGeoserverLayers().clear();
        osirisFile.getGeoserverLayers().addAll(syncedLayers);
        fileDataService.save(ImmutableSet.of(osirisFile));

    }

}