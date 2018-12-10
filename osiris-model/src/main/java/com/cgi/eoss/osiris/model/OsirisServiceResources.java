package com.cgi.eoss.osiris.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OsirisServiceResources {

    private String cpus;
    
    private String ram;

    private String storage;
}
