package com.cgi.eoss.osiris.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OsirisServiceDockerBuildInfo {
    
    private String lastBuiltFingerprint;
    
    private Status dockerBuildStatus = Status.NOT_STARTED;
    
    public enum Status {
        NOT_STARTED, ONGOING, COMPLETED, ERROR
    }
}
