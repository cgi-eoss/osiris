package com.cgi.eoss.osiris.catalogue.files;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.hateoas.Link;

import com.cgi.eoss.osiris.catalogue.OsirisFileService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;

public interface OutputProductService extends OsirisFileService {
    
    OsirisFile ingest(String collection, User owner, String jobId, String crs, String geometry, OffsetDateTime startDateTime, OffsetDateTime endDateTime, Map<String, Object> properties,
            Path path) throws IOException;
    
    public String getDefaultCollection();
    
    public boolean createCollection(Collection collection);

    boolean deleteCollection(Collection collection);

    Path provision(String jobId, String filename) throws IOException;
    
	Set<Link> getOGCLinks(OsirisFile osirisFile);



}
