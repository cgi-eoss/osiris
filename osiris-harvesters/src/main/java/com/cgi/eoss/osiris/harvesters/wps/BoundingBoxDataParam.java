package com.cgi.eoss.osiris.harvesters.wps;

import lombok.Data;

@Data
public class BoundingBoxDataParam {
	
	private final String lowerCorner;
	
	private final String upperCorner;
	
	private final String crs;
	
	private final int dimensions;
}
