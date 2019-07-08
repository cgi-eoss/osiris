package com.cgi.eoss.osiris.harvesters.ftp;

import java.time.Instant;

import lombok.Data;

@Data
public class FileItem {
	
	private final String uri;
	
	private final Instant timestamp;
	
}
