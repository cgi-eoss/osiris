package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.rpc.Job;

public interface DynamicProxyService {
	
	public ReverseProxyEntry getProxyEntry(Job job, String host, int port);
	
	public default boolean supportsProxyRoute() {
		return false;
	}
	
	public String getProxyRoute(Job job);
	
	public void update();
}
