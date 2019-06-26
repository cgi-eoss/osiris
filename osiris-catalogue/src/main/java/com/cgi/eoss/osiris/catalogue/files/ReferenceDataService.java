package com.cgi.eoss.osiris.catalogue.files;

import com.cgi.eoss.osiris.catalogue.OsirisFileService;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.internal.UploadableFileType;
import org.springframework.hateoas.Link;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ReferenceDataService extends OsirisFileService {
    OsirisFile ingest(String collection, User user, String filename, UploadableFileType filetype, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;

    public String getDefaultCollection();

    void createCollection(Collection collection) throws IOException;

    void deleteCollection(Collection collection) throws IOException;
    
	Set<Link> getOGCLinks(OsirisFile osirisFile);
}
