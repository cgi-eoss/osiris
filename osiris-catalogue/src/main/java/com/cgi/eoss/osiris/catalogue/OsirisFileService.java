package com.cgi.eoss.osiris.catalogue;

import com.cgi.eoss.osiris.model.OsirisFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 */
public interface OsirisFileService {
    Resource resolve(OsirisFile file);
    void delete(OsirisFile file) throws IOException;
}
