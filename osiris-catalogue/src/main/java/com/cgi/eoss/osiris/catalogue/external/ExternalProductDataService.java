package com.cgi.eoss.osiris.catalogue.external;

import com.cgi.eoss.osiris.catalogue.OsirisFileService;
import com.cgi.eoss.osiris.model.OsirisFile;
import org.geojson.GeoJsonObject;

import java.net.URI;

public interface ExternalProductDataService extends OsirisFileService {
    OsirisFile ingest(GeoJsonObject geoJson);

    URI getUri(String productSource, String productId);
}
