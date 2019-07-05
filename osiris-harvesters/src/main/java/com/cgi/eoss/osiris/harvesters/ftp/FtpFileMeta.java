package com.cgi.eoss.osiris.harvesters.ftp;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class FtpFileMeta {

	private long fileSize;
	private String fileName;
	private InputStream fileInputStream;
}
