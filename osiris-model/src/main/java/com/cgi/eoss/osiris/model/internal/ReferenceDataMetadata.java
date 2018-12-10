package com.cgi.eoss.osiris.model.internal;

import com.cgi.eoss.osiris.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * <p>Convenience wrapper of metadata for a reference data product.</p>
 */
@Data
@Builder
public class ReferenceDataMetadata {

    private User owner;
    private String filename;
    private UploadableFileType filetype;
    private Map<String, Object> userProperties;

}
