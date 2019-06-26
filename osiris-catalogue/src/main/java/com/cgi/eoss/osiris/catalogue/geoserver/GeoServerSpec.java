package com.cgi.eoss.osiris.catalogue.geoserver;

import lombok.Builder;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class GeoServerSpec {

    private String workspace;

    private GeoServerType geoserverType;
    
    private String datastoreName;
    
    private String coverageName;
    
    private String layerName;
    
    private String crs;
    
    private String style;
    
    @Builder.Default
    private Map<String, String> options = new HashMap<>();
}
