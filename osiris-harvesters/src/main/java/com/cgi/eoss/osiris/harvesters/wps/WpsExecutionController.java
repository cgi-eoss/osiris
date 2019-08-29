package com.cgi.eoss.osiris.harvesters.wps;

import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument;

import java.util.Map;

public interface WpsExecutionController {

	public CapabilitiesDocument getCapabilities(String string) throws WpsException;
	
	ProcessDescriptionsDocument describeProcess(String url, String processId) throws WpsException;

	Object executeWpsProcess(String url, String processId, boolean requestOutputStorage,
			Map<String, ComplexDataParam> complexParams, Map<String, LiteralDataParam> literalDataParams,
			Map<String, BoundingBoxDataParam> boundingBoxDataParam,	Map<String, ComplexDataOutputDefinition> complexDataOutputDefinitions) throws WpsException;
	
	public ExecuteResponseDocument getWpsStatus(String url) throws WpsException;

	public ExecuteResponseDocument executeViaGET(String url) throws WpsException;

	
}