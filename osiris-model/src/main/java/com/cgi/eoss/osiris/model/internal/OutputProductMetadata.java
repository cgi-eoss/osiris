package com.cgi.eoss.osiris.model.internal;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * <p>Convenience wrapper of metadata for a output data product.</p>
 */
@Data
@Builder
public class OutputProductMetadata {

    private User owner;
    private OsirisService service;
    private String outputId;
    private String jobId;
    private Map<String, Object> productProperties;

}
