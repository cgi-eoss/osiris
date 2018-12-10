package com.cgi.eoss.osiris.catalogue.files;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cgi.eoss.osiris.catalogue.CatalogueUri;
import com.cgi.eoss.osiris.catalogue.resto.RestoService;
import com.cgi.eoss.osiris.catalogue.util.GeoUtil;
import com.cgi.eoss.osiris.catalogue.util.GeometryException;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.internal.UploadableFileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class FilesystemReferenceDataService implements ReferenceDataService {

    private static final int GEOMETRY_BOUNDING_BOX_THRESHOLD = 10000;
	private final Path referenceDataBasedir;
    private final RestoService resto;
    private final ObjectMapper jsonMapper;

    @Autowired
    public FilesystemReferenceDataService(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, RestoService resto, ObjectMapper jsonMapper) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.resto = resto;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public OsirisFile ingest(User owner, String filename, UploadableFileType filetype, Map<String, Object> userProperties, MultipartFile multipartFile) throws IOException {
        Path dest = referenceDataBasedir.resolve(String.valueOf(owner.getId())).resolve(filename);
        LOG.info("Saving new reference data to: {}", dest);

        if (Files.exists(dest)) {
            LOG.warn("Found already-existing reference data, overwriting: {}", dest);
        }

        Files.createDirectories(dest.getParent());
        Files.copy(multipartFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        URI uri = CatalogueUri.REFERENCE_DATA.build(
                ImmutableMap.of(
                        "ownerId", owner.getId().toString(),
                        "filename", filename));

        Map<String, Object> properties = new HashMap<>();

        // Add automatically-determined properties
        properties.put("productIdentifier", owner.getName() + "_" + filename);
        properties.put("owner", owner.getName());
        properties.put("filename", filename);
        properties.put("osirisUrl", uri);
        // TODO Get the proper MIME type
        properties.put("resourceMimeType", "application/unknown");
        long filesize = Files.size(dest);
		properties.put("resourceSize", filesize);
        properties.put("resourceChecksum", "sha256=" + MoreFiles.asByteSource(dest).hash(Hashing.sha256()));
        
        String startTime = (String) userProperties.remove("startTime");
        String endTime = (String) userProperties.remove("endTime");
       
        if (startTime != null) {
        	properties.put("startDate", startTime.toString());
        }
        if (endTime != null) {
        	properties.put("completionDate", endTime.toString());
        }
        
        String description = (String) userProperties.remove("description");
        if (description != null) {
        	properties.put("description", description.toString());
        }
        
        GeoJsonObject geometry = null;
        switch (filetype) {
    	case GEOTIFF: 
    		   geometry = GeoUtil.extractBoundingBox(dest); break;
    	case SHAPEFILE:{
    		//Try to get shapefile as multipolygon, if not possible resort to bounding box
    					try {
    						geometry = GeoUtil.shapeFileToGeojson(dest, GEOMETRY_BOUNDING_BOX_THRESHOLD);
    					}
    					catch(GeometryException e) {
    						geometry = GeoUtil.extractBoundingBox(dest);
    					}
    					break;
    			}
    	case OTHER: {
    			String userWktGeometry = (String) userProperties.remove("geometry");
    			if (userWktGeometry != null) {
    				geometry =  GeoUtil.getGeoJsonGeometry(userWktGeometry);
    			}
    	}
        }

        if (geometry == null) {
        	geometry = GeoUtil.defaultGeometry();
        }
        
        // TODO Validate extra properties?
        properties.put("extraParams", jsonMapper.writeValueAsString(userProperties));

        Feature feature = new Feature();
        feature.setId(owner.getName() + "_" + filename);
        feature.setGeometry(geometry);        
        feature.setProperties(properties);

        UUID restoId;
        try {
            restoId = resto.ingestReferenceData(feature);
            LOG.info("Ingested reference data with Resto id {} and URI {}", restoId, uri);
        } catch (Exception e) {
            LOG.error("Failed to ingest reference data to Resto, continuing...", e);
            // TODO Add GeoJSON to OsirisFile model
            restoId = UUID.randomUUID();
        }

        OsirisFile osirisFile = new OsirisFile(uri, restoId);
        osirisFile.setOwner(owner);
        osirisFile.setType(OsirisFile.Type.REFERENCE_DATA);
        osirisFile.setFilename(referenceDataBasedir.relativize(dest).toString());
        osirisFile.setFilesize(filesize);
        return osirisFile;
    }

    

	@Override
    public Resource resolve(OsirisFile file) {
        Path path = referenceDataBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }

    @Override
    public void delete(OsirisFile file) throws IOException {
        Files.deleteIfExists(referenceDataBasedir.resolve(file.getFilename()));
        resto.deleteReferenceData(file.getRestoId());
    }

}
