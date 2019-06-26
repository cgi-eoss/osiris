package com.cgi.eoss.osiris.catalogue.external;

import com.cgi.eoss.osiris.catalogue.CatalogueUri;
import com.cgi.eoss.osiris.catalogue.resto.RestoService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.persistence.service.OsirisFileDataService;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Handler for external product (e.g. S-1, S-2, Landsat) metadata and files.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ExternalProductDataServiceImpl implements ExternalProductDataService {

    private final OsirisFileDataService osirisFileDataService;
    private final RestoService resto;

    @Override
    public OsirisFile ingest(GeoJsonObject geoJson) {
        // TODO Handle non-feature objects?
        Feature feature = (Feature) geoJson;

        String productSource = feature.getProperty("productSource").toString().toLowerCase().replaceAll("[^a-z0-9]", "");
        String productId = feature.getProperty("productIdentifier");

        Long filesize = Optional.ofNullable((Long) feature.getProperties().get("filesize")).orElse(getFilesize(feature));
        feature.getProperties().put("filesize", filesize);

        URI uri = Optional.ofNullable((String) feature.getProperties().get("osirisUrl")).map(URI::create).orElse(getUri(productSource, productId));
        feature.getProperties().put("osirisUrl", uri);

        return Optional.ofNullable(osirisFileDataService.getByUri(uri)).orElseGet(() -> {
            UUID restoId;
            try {
                restoId = resto.ingestExternalProduct(productSource, feature);
                LOG.info("Ingested external product with Resto id {} and URI {}", restoId, uri);
            } catch (Exception e) {
                LOG.error("Failed to ingest external product to Resto, continuing...", e);
                // TODO Add GeoJSON to OsirisFile model
                restoId = UUID.randomUUID();
            }
            OsirisFile osirisFile = new OsirisFile(uri, restoId);
            osirisFile.setType(OsirisFile.Type.EXTERNAL_PRODUCT);
            osirisFile.setFilename(productId);
            osirisFile.setFilesize(filesize);
            return osirisFileDataService.save(osirisFile);
        });
    }

    @Override
    public URI getUri(String productSource, String productId) {
        URI uri;
        try {
            CatalogueUri productSourceUrl = CatalogueUri.valueOf(productSource);
            uri = productSourceUrl.build(ImmutableMap.of("productId", productId));
        } catch (IllegalArgumentException e) {
            uri = URI.create(productSource.replaceAll("[^a-z0-9+.-]", "-") + ":///" + productId);
            LOG.debug("Could not build a well-designed OSIRIS URI handler, returning automatic: {}", uri);
        }
        return uri;
    }

    @Override
    public Resource resolve(OsirisFile file) {
        // TODO Allow proxied access (with TEP coin cost) to some external data
        throw new UnsupportedOperationException("Direct download of external products via OSIRIS is not permitted");
    }

    @Override
    public void delete(OsirisFile file) throws IOException {
        resto.deleteExternalProduct(file.getRestoId());
    }

    @SuppressWarnings("unchecked")
    private Long getFilesize(Feature feature) {
        return Optional.ofNullable((Map<String, Object>) feature.getProperties().get("extraParams"))
                .map(ep -> (Map<String, Object>) ep.get("file"))
                .map(file -> file.get("data_file_size"))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(null);
    }

}
