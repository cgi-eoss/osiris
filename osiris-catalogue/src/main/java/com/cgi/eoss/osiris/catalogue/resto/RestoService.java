package com.cgi.eoss.osiris.catalogue.resto;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisFile;
import java.util.UUID;
import org.geojson.GeoJsonObject;

/**
 * <p>Facade to a Resto instance, to enable OSIRIS OpenSearch Geo/Time functionality.</p>
 */
public interface RestoService {
    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the OSIRIS Reference Data collection, and return the
     * new record's UUID.</p>
     */
    UUID ingestReferenceData(GeoJsonObject object);
    
    /**
     * Ingest the given GeoJsonObject product in a specific collection
     */
    UUID ingestOutputProduct(String collection, GeoJsonObject object);

    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the given collection, and return the new record's
     * UUID.</p>
     */
    UUID ingestExternalProduct(String collection, GeoJsonObject object);

    /**
     * <p>Remove the given OSIRIS Output product from the Resto catalogue.</p>
     */
    void deleteOutputProduct(UUID restoId);
    
    /**
     * <p>Remove the given OSIRIS Reference Data product from the Resto catalogue.</p>
     */
    void deleteReferenceData(UUID restoId);

    /**
     * @return The Resto catalogue GeoJSON data for the given OsirisFile.
     */
    GeoJsonObject getGeoJson(OsirisFile osirisFile);

    /**
     * @return The Resto catalogue GeoJSON data for the given OsirisFile, or null if any exception is encountered.
     */
    GeoJsonObject getGeoJsonSafe(OsirisFile osirisFile);

    /**
     * @return The Resto collection name identifying OSIRIS reference data.
     */
    String getReferenceDataCollection();

    /**
     * @return The Resto collection name identifying OSIRIS output products.
     */
    String getOutputProductsCollection();
    
    /**
     * Creates a new Resto collection
     */
    boolean createOutputCollection(Collection collection);
    
    /**
     * Deletes a resto collection
     * @return 
     */
    boolean deleteCollection(Collection collection);
}
