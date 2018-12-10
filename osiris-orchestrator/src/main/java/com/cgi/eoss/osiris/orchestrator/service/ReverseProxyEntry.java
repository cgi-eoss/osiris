package com.cgi.eoss.osiris.orchestrator.service;

import lombok.Data;

@Data 
public class ReverseProxyEntry {

	private final String path;
	
	private final String endpoint;
	
}
