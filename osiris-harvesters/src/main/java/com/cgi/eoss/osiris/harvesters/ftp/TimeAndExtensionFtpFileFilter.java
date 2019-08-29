package com.cgi.eoss.osiris.harvesters.ftp;

import java.time.Instant;
import java.util.Set;

import com.google.common.collect.ImmutableList;
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
		if (isExtensionExcluded(file.getName())){
			return false;
		}
		
		if (file.isFile()) {
			return instant == null || file.getTimestamp().toInstant().compareTo(instant) >= 0;
		}
		
		if(file.isDirectory()) {
			return !ImmutableList.of(".", "..").contains(file.getName());
		}
		return false;
	}

	private boolean isExtensionExcluded(String name) {
		return excludedExtensions
				.stream().map(name::endsWith).filter(v -> v).anyMatch(v -> v);
	}
}
