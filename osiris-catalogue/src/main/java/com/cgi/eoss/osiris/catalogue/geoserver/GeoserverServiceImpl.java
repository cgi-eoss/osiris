package com.cgi.eoss.osiris.catalogue.geoserver;

import static it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.catalogue.IngestionException;
import com.cgi.eoss.osiris.catalogue.geoserver.model.GeoserverImportTask;
import com.cgi.eoss.osiris.catalogue.util.GeoUtil;
import com.cgi.eoss.osiris.model.GeoserverLayer;
import com.cgi.eoss.osiris.model.GeoserverLayer.StoreType;
import com.google.common.io.MoreFiles;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSPostGISDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;

@Component
@Log4j2
public class GeoserverServiceImpl implements GeoserverService {

    private static final String RASTER_STYLE = "raster";

    @Getter
    private final HttpUrl externalUrl;
    private final GeoServerRESTPublisher publisher;
    private final GeoServerRESTReader reader;

    @Value("${osiris.catalogue.geoserver.enabled:true}")
    private boolean geoserverEnabled = true;

    private Set<String> ingestableFiletypes;
    
    private GeoserverMosaicManager mosaicManager;

	private GeoserverImporter importer;
	
	private String postgisStore;
	
	private String postgisWorkspace;

	private String postgisHost;

	private int postgisPort;

	private String postgisDb;

	private String postgisUsername;

	private String postgisPassword;

    @Autowired
    public GeoserverServiceImpl(@Value("${osiris.catalogue.geoserver.url:http://osiris-geoserver:9080/geoserver/}") String url,
            @Value("${osiris.catalogue.geoserver.externalUrl:http://osiris-geoserver:9080/geoserver/}") String externalUrl,
            @Value("#{'${osiris.catalogue.geoserver.ingest-filetypes:TIF}'.split(',')}") Set<String> ingestableFiletypes,
            @Value("${osiris.catalogue.geoserver.username:osirisgeoserver}") String username,
            @Value("${osiris.catalogue.geoserver.password:osirisgeoserverpass}") String password,
            @Value("${osiris.catalogue.geoserver.postgisHost:osirisdb}") String postgisHost,
            @Value("${osiris.catalogue.geoserver.postgisPort:5432}") int postgisPort,
            @Value("${osiris.catalogue.geoserver.postgisDb:osirisgeoserver}") String postgisDb,
            @Value("${osiris.catalogue.geoserver.postgisUsername:osirisgeoserverPostgis}") String postgisUsername,
            @Value("${osiris.catalogue.geoserver.postgisPassword:osirisgeoserverPostgispass}") String postgisPassword,
            @Value("${osiris.catalogue.geoserver.postgisStore:osirisgeoserverPostgisStore}") String postgisStore,
            @Value("${osiris.catalogue.geoserver.postgisWorkspace:osirisgeoserverPostgisWorkspace}") String postgisWorkspace
            ) throws MalformedURLException {
        this.externalUrl = HttpUrl.parse(externalUrl);
        GeoServerRESTManager geoserver = new GeoServerRESTManager(new URL(url), username, password);
        if (ingestableFiletypes == null){
        	ingestableFiletypes = new HashSet<>();
        }
        this.ingestableFiletypes = ingestableFiletypes;
        this.reader = geoserver.getReader();
        this.publisher = geoserver.getPublisher();
        this.importer = new GeoserverImporter(new URL(url), username, password);
        this.mosaicManager = new GeoserverMosaicManager(reader, new URL(url), username, password, postgisHost, postgisPort, postgisDb, postgisUsername, postgisPassword);
        this.postgisStore = postgisStore;
        this.postgisWorkspace = postgisWorkspace;
        this.postgisHost = postgisHost;
        this.postgisPort = postgisPort;
        this.postgisDb = postgisDb;
        this.postgisUsername = postgisUsername;
        this.postgisPassword = postgisPassword;
    }
    
    @PostConstruct
    public void ensurePostgisStoreExists() {
    	//TODO replace with non deprecated versions of geosolutions library - requires change in lib version
    	try {
            ensureWorkspaceExists(postgisWorkspace);
        	if (!reader.existsDatastore(postgisWorkspace, postgisStore)) {
                LOG.info("Creating new store {}", postgisStore);
                GSPostGISDatastoreEncoder postgisDataStoreEncoder = new GSPostGISDatastoreEncoder();
                postgisDataStoreEncoder.setName(postgisStore);
                postgisDataStoreEncoder.setHost(postgisHost);
                postgisDataStoreEncoder.setPort(postgisPort);
                postgisDataStoreEncoder.setDatabase(postgisDb);
                postgisDataStoreEncoder.setUser(postgisUsername);
                postgisDataStoreEncoder.setPassword(postgisPassword);
                boolean created = publisher.createPostGISDatastore(postgisWorkspace, postgisDataStoreEncoder);
                if (!created) {
                	throw new IngestionException("Cannot create postgis store");
                }
            }
    	}catch(Exception e) {
    	    LOG.error("Cannot ensure availability of postgis store");
    	}
    }
    
    @Override
    public GeoserverLayer ingest(Path path, GeoServerSpec geoServerSpec, UUID id) {
        String workspace = geoServerSpec.getWorkspace();
        String datastoreName = geoServerSpec.getDatastoreName();
        String coverageName = geoServerSpec.getCoverageName();
        String layerName = geoServerSpec.getLayerName();
        String srs = geoServerSpec.getCrs();
        String style = geoServerSpec.getStyle();
        
        switch (geoServerSpec.getGeoserverType()) {
            case SINGLE_COVERAGE: {
            	ingestCoverage(workspace, path, srs, datastoreName, coverageName, style);
            	return new GeoserverLayer(null, workspace, datastoreName, coverageName, StoreType.GEOTIFF);
            } 
            case MOSAIC: {
            	ingestCoverageInMosaic(workspace, path, srs, datastoreName, coverageName);
            	return new GeoserverLayer(null, workspace, datastoreName, coverageName, StoreType.MOSAIC);
            }	
            case SHAPEFILE_POSTGIS_IMPORT: { 
            	ingestShapefileInPostgis(path, layerName, id);
            	return new GeoserverLayer(null, postgisWorkspace, postgisStore, layerName, StoreType.POSTGIS);
            }
            default: throw new IngestionException("GeoServerType not specified");
        }
    }
    
    @Override
    public String ingest(String workspace, Path path, String crs) {
        Path fileName = path.getFileName();
        String datastoreName = MoreFiles.getNameWithoutExtension(fileName);
        String layerName = MoreFiles.getNameWithoutExtension(fileName);
        
        return ingestCoverage(workspace, path, crs, datastoreName, layerName, RASTER_STYLE);
    }
    
	private String ingestCoverage(String workspace, Path path, String crs, String datastoreName, String layerName, String style) {
        Path fileName = path.getFileName();
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, layerName);
            return null;
        }

        ensureWorkspaceExists(workspace);

        if (!isIngestibleFile(fileName.toString())) {
            // TODO Ingest more filetypes
            LOG.info("Unable to ingest product with filename: {}", fileName);
            return null;
        }

        try {
            RESTCoverageStore restCoverageStore = publishExternalGeoTIFF(workspace, datastoreName, path.toFile(), layerName, crs,
                    REPROJECT_TO_DECLARED, style);
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
            return restCoverageStore.getURL();
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IngestionException(e);
        }
    }
    
    private String ingestCoverageInMosaic(String workspace, Path path, String crs, String datastoreName, String coverageName) {
        Path fileName = path.getFileName();
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, datastoreName);
            return null;
        }

        ensureWorkspaceExists(workspace);

        if (!isIngestibleFile(fileName.toString())) {
            // TODO Ingest more filetypes
            LOG.info("Unable to ingest product with filename: {}", fileName);
            return null;
        }

        try {
            RESTCoverageStore restCoverageStore = publishExternalGeoTIFFToMosaic(workspace, datastoreName, path.toFile());
            if (coverageName != null) {
                mosaicManager.createCoverageIfNotExists(workspace, datastoreName, coverageName);
            }
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, datastoreName);
            LOG.info("Reloading GeoServer configuration");
            publisher.reload();
            return restCoverageStore.getURL();
        } catch (IOException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IngestionException(e);
        }
    }

    @Override
    public boolean isIngestibleFile(String filename) {
        return ingestableFiletypes.stream().anyMatch(ft -> filename.toUpperCase().endsWith("." + ft.toUpperCase()));
    }

    @Override
    public void deleteLayer(String workspace, String layerName) {
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; no deletion occurring for {}:{}", workspace, layerName);
            return;
        }

        publisher.removeLayer(workspace, layerName);
        LOG.info("Deleted layer from geoserver: {}{}", workspace, layerName);
    }
    
    @Override
    public void deleteCoverageStore(String workspace, String coverageStoreName) {
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; no deletion occurring for {}:{}", workspace, coverageStoreName);
            return;
        }

        publisher.removeCoverageStore(workspace, coverageStoreName, false);
        LOG.info("Deleted coverage store from geoserver: {}{}", workspace, coverageStoreName);
    }

    private void ensureWorkspaceExists(String workspace) {
        if (!reader.existsWorkspace(workspace)) {
            LOG.info("Creating new workspace {}", workspace);
            publisher.createWorkspace(workspace);
        }
    }

    private RESTCoverageStore publishExternalGeoTIFF(String workspace, String storeName, File geotiff, String coverageName, String srs,
            GSResourceEncoder.ProjectionPolicy policy, String defaultStyle) throws FileNotFoundException, IllegalArgumentException {
        if (workspace == null || storeName == null || geotiff == null || coverageName == null || srs == null || policy == null
                || defaultStyle == null)
            throw new IllegalArgumentException("Unable to run: null parameter");

        // config coverage props (srs)
        final GSCoverageEncoder coverageEncoder = new GSCoverageEncoder();
        coverageEncoder.setName(coverageName);
        coverageEncoder.setTitle(coverageName);
        coverageEncoder.setSRS(srs);
        coverageEncoder.setProjectionPolicy(policy);

        // config layer props (style, ...)
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        layerEncoder.setDefaultStyle(defaultStyle);

        return publisher.publishExternalGeoTIFF(workspace, storeName, geotiff, coverageEncoder, layerEncoder);
    }
    
    private RESTCoverageStore publishExternalGeoTIFFToMosaic(String workspace, String storeName, File geotiff) throws FileNotFoundException, IllegalArgumentException {
        if (workspace == null || storeName == null || geotiff == null)
            throw new IllegalArgumentException("Unable to run: null parameter");
        RESTCoverageStore response = mosaicManager.addGeoTiffToExternalMosaic(workspace, storeName, geotiff);
        return response;
    }

	@Override
	public void createEmptyMosaic(String workspace, String storeName, String coverageName, String timeRegexp) {
		ensureWorkspaceExists(workspace);
		mosaicManager.createEmptyMosaic(workspace, storeName, coverageName, timeRegexp);
	}
	
	private String ingestShapefileInPostgis(Path path, String layerName, UUID id) {
		String importUrl;
		try {
			importUrl = importer.createImport(postgisWorkspace, postgisStore);
			//Duplicate the shapefile, as the table name and the layer are taken from the internal shapefile name
			//Add a column to trace back to file (using the id)
			Map<String, Object> newAttributes = new HashMap<>();
			newAttributes.put("osiris_id", id);
			Path transformedShapeFile = GeoUtil.duplicateShapeFile(path, layerName, newAttributes, true);
			String taskUrl = importer.addShapeFileToImport(transformedShapeFile, importUrl);
			if (importer.existsFeatureType(postgisWorkspace, postgisStore, layerName)) {
				importer.setTaskUpdateMode(taskUrl, GeoserverImportTask.UpdateMode.APPEND);
			}
			importer.runImport(importUrl);
			return importUrl;
		} catch (IOException e) {
			throw new IngestionException(e);
		}
	}

    @Override
    public void deleteGranuleFromMosaic(String workspace, String storeName, String layerName, String location) {
        mosaicManager.deleteGranuleFromMosaic(workspace, storeName, layerName, location);
        
    }

}
