package com.cgi.eoss.osiris.worker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.cgi.eoss.osiris.io.download.UnzipStrategy;

@Configuration
@ConfigurationProperties(prefix="osiris.worker")
public class UnzipBySchemeProperties {
	
	private final Map<String, UnzipStrategy> unzipByScheme = new HashMap<>();
	   
	public Map<String, UnzipStrategy> getUnzipByScheme() {
	        return unzipByScheme;
	}
}