package com.cgi.eoss.osiris.harvesters.wps;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class WpsFileMeta {

	private long fileSize;
	private String fileName;
	private InputStream fileInputStream;
}
