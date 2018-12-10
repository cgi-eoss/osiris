package com.cgi.eoss.osiris.catalogue.files;

import com.cgi.eoss.osiris.catalogue.OsirisFileService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.internal.UploadableFileType;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ReferenceDataService extends OsirisFileService {
    OsirisFile ingest(User user, String filename, UploadableFileType filetype, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;
}
