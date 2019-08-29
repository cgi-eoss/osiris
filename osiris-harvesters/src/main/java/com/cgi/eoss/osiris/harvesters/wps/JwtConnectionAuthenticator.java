package com.cgi.eoss.osiris.harvesters.wps;

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Jwts;

import java.net.URLConnection;
import java.security.Key;

public class JwtConnectionAuthenticator implements URLConnectionAuthenticator{

	private Key privateKey;
	
	public JwtConnectionAuthenticator(Key privateKey) {
		this.privateKey = privateKey;
	}
	
	@Override
	public void authenticate(String service, String request, URLConnection urlConnection) {
		urlConnection.setRequestProperty("Authorization", "Bearer " + getJwtToken(service, request));
		
	}

	private String getJwtToken(String service, String request) {
		return Jwts.builder()
				.setClaims(ImmutableMap.of("service", service, "request", request))
				.signWith(privateKey).compact();
	}
	
	

}
