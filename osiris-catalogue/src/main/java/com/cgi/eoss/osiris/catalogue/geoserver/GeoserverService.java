package com.cgi.eoss.osiris.catalogue.geoserver;

import java.nio.file.Path;
import java.util.UUID;
import com.cgi.eoss.osiris.model.GeoserverLayer;

import okhttp3.HttpUrl;

/**
 * <p>Facade to a Geoserver instance, to enhance/enable OSIRIS W*S functionality.</p>
 */
public interface GeoserverService {

    /**
     * <p>Ingests the file in geoserver according to the geoserverSpec</p>
     */

    GeoserverLayer ingest(Path path, GeoServerSpec geoServerSpec, UUID id);
    
    /**
     * <p>Performs a defalt ingestion of a raster file inside a store named as the file and exposed as a layer named after the file</p>
     */
    String ingest(String workspace, Path path, String crs);

    boolean isIngestibleFile(String filename);
    
    /**
     * <p>Delete the layer with the given name from the selected workspace.</p>
     */
    void deleteLayer(String workspace, String layerName);

    /**
     * <p>Delete the coverage store with the given name from the selected workspace.</p>
     */
    void deleteCoverageStore(String workspace, String storeName);
    
    HttpUrl getExternalUrl();

    void createEmptyMosaic(String workspace, String storeName, String coverageName, String timeRegexp);

    void deleteGranuleFromMosaic(String workspace, String coverageStoreName, String location);
}
