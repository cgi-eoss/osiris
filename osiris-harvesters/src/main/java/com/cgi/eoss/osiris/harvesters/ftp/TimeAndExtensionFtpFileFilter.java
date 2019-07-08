package com.cgi.eoss.osiris.harvesters.ftp;

import java.time.Instant;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class TimeAndExtensionFtpFileFilter implements FTPFileFilter{

    private Instant instant;
	private Set<String> excludedExtensions;

    public TimeAndExtensionFtpFileFilter(Instant instant, Set<String> excludedExtensions) {
        this.instant = instant;
        this.excludedExtensions = excludedExtensions;
    }
    
	@Override
	public boolean accept(FTPFile file) {
		return !isExtensionExcluded(file.getName()) && (file.isDirectory() || instant == null || file.getTimestamp().toInstant().compareTo(instant) >= 0);
		
	}

	private boolean isExtensionExcluded(String name) {
		return excludedExtensions
				.stream().map(e -> name.endsWith(e)).filter(v -> v).anyMatch(v -> v);
	}
}
