package com.cgi.eoss.osiris.harvesters.wps;

import java.util.HashMap;

public class MimeTypeToExtension {

	public static final String EXTENSION_UNKNOWN = "unknown";
	public static final String MIME_APPLICATION_XML = "application/xml";
	public static final String MIME_APPLICATION_WKT = "application/wkt";
	
	private static HashMap<String, String> mimeTypeToExtensionMap = new HashMap<>();
	
	static {
		mimeTypeToExtensionMap.put(MIME_APPLICATION_WKT, "wkt");
		
	}
	
	public static String getExtension(String mimeType) {
		 return mimeTypeToExtensionMap.getOrDefault(mimeType.toLowerCase(), EXTENSION_UNKNOWN);
	}
}
