package com.cgi.eoss.osiris.harvesters.wps;

import lombok.Data;

@Data
public class ComplexDataParam {
	
	private final String encoding;
	private final String schema;
	private final String mimeType;
	private final String value;
	
}
