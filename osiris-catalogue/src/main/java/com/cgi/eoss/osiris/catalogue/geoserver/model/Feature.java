package com.cgi.eoss.osiris.catalogue.geoserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature {

    private String id;
    
    
}
