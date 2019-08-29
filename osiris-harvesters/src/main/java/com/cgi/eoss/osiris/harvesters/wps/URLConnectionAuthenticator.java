package com.cgi.eoss.osiris.harvesters.wps;

import java.net.URLConnection;

public interface URLConnectionAuthenticator {
	
	public void authenticate(String service, String request, URLConnection urlConnection);

}
