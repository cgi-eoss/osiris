package com.cgi.eoss.osiris.harvesters.wps;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.log4j.Log4j2;
import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.ows.x11.OperationDocument.Operation;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wps.client.ClientCapabiltiesRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


@Log4j2
public class CachingWpsClient {
	
	private static final String OGC_OWS_URI = "http://www.opengeospatial.net/ows";
	private static final String SUPPORTED_VERSION = "1.0.0"; 
	private Cache<String, CapabilitiesDocument> cachedCapabilities;
	private Cache<String, Map<String, ProcessDescriptionsDocument>> cachedProcessDescriptions;

	private XmlOptions options = null;
	
	private URLConnectionAuthenticator urlConnectionAuthenticator;
	
	public CachingWpsClient() {
		options = new XmlOptions();
		options.setLoadStripWhitespace();
		options.setLoadTrimTextBuffer();
		cachedCapabilities = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
		cachedProcessDescriptions = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
	}
	
	public void setUrlConnectionAuthenticator(URLConnectionAuthenticator urlConnectionAuthenticator) {
		this.urlConnectionAuthenticator = urlConnectionAuthenticator;
	}
	
	public CapabilitiesDocument getCapabilities(String serverUrl) throws WpsException {
		CapabilitiesDocument caps = this.cachedCapabilities.getIfPresent(serverUrl);
		if (caps != null) {
			return caps;
		}
		caps = retrieveCapsViaGET(serverUrl);
		this.cachedCapabilities.put(serverUrl, caps);
		return caps;
	}
	
	private CapabilitiesDocument retrieveCapsViaGET(String url) throws WpsException {
		ClientCapabiltiesRequest req = new ClientCapabiltiesRequest();
		url = req.getRequest(url);
		try {
			URL urlObj = new URL(url);
			URLConnection connection = urlObj.openConnection();
			if (this.urlConnectionAuthenticator != null) {
				urlConnectionAuthenticator.authenticate("WPS", "GetCapabilities", connection);
			}
			InputStream is = connection.getInputStream();
			Document doc = checkInputStream(is);
			return CapabilitiesDocument.Factory.parse(doc, options);
		} catch (MalformedURLException e) {
			throw new WpsException("Capabilities URL seems to be unvalid: " + url, e);
		} catch (IOException e) {
			throw new WpsException("Error occured while retrieving capabilities from url: " + url, e);
		} catch (XmlException e) {
			throw new WpsException("Error occured while parsing XML", e);
		}
	}
	
	public ProcessDescriptionsDocument describeProcess(String serverURL, String processID, boolean usePassedURL) throws WpsException {
		Map<String, ProcessDescriptionsDocument> cachedProcessDescriptionsForServer = cachedProcessDescriptions.getIfPresent(serverURL);
		if (cachedProcessDescriptionsForServer == null || !cachedProcessDescriptionsForServer.containsKey(processID)) {
			ProcessDescriptionsDocument processDescription = describeProcesses(serverURL, usePassedURL, new String[] {processID});
			if (cachedProcessDescriptionsForServer == null) {
				cachedProcessDescriptionsForServer = new HashMap<>();
				cachedProcessDescriptionsForServer.put(processID, processDescription);
			}
		}
		return cachedProcessDescriptionsForServer.get(processID);
	}
	
	public ProcessDescriptionsDocument describeProcesses(String serverUrl, boolean usePassedUrl, String[] processIDs) throws WpsException {
		String url = serverUrl;
		if (!usePassedUrl) {
			CapabilitiesDocument caps = this.getCapabilities(serverUrl);
			Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();

			for (Operation operation : operations) {
				if (operation.getName().equals("DescribeProcess")) {
					url = operation.getDCPArray()[0].getHTTP().getGetArray()[0].getHref();
				}
			}
			if (url == null) {
				throw new WpsException("Capabilities do not contain any information about the entry point for DescribeProcess operation.");
			}
		}
		return retrieveDescriptionViaGET(processIDs, url);
	}
	
	private ProcessDescriptionsDocument retrieveDescriptionViaGET(String[] processIDs, String url) throws WpsException{
		ClientDescribeProcessRequest req = new ClientDescribeProcessRequest();
		req.setIdentifier(processIDs);
		String requestURL = req.getRequest(url);
		try {
			URL urlObj = new URL(requestURL);
			URLConnection connection = urlObj.openConnection();
			if (this.urlConnectionAuthenticator != null) {
				urlConnectionAuthenticator.authenticate("WPS", "DescribeProcess", connection);
			}
			InputStream is = connection.getInputStream();
			Document doc = checkInputStream(is);
			return ProcessDescriptionsDocument.Factory.parse(doc, options);
		} catch (MalformedURLException e) {
			throw new WpsException("URL seems not to be valid", e);
		}
		catch (IOException e) {
			throw new WpsException("Error occured while receiving data", e);
		}
		catch(XmlException e) {
			throw new WpsException("Error occured while parsing ProcessDescription document", e);
		}
	}
	
	
	
	/**
	 * Executes a process at a WPS
	 * 
	 * @param url url of server not the entry additionally defined in the caps.
	 * @param execute Execute document
	 * @return either an ExecuteResponseDocument or an InputStream if asked for RawData or an Exception Report 
	 */
	public Object execute(String serverID, ExecuteDocument execute, boolean usePassedUrl) throws WpsException{
		if(execute.getExecute().isSetResponseForm() && execute.getExecute().getResponseForm().isSetRawDataOutput()){
			return execute(serverID, usePassedUrl, execute, true);
		}else{
			return execute(serverID, usePassedUrl, execute,false);
		}
	}
	
	/**
	 * Executes a process at a WPS
	 * 
	 * @param url url of server not the entry additionally defined in the caps.
	 * @param execute Execute document
	 * @return either an ExecuteResponseDocument or an InputStream if asked for RawData or an Exception Report 
	 */
	private Object execute(String serverUrl, boolean usePassedUrl, ExecuteDocument execute, boolean rawData) throws WpsException{
		String url = serverUrl;
		if (!usePassedUrl) {
			CapabilitiesDocument caps = getCapabilities(serverUrl);
			Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
			for (Operation operation : operations) {
				if (operation.getName().equals("Execute")) {
					url = operation.getDCPArray()[0].getHTTP().getPostArray()[0].getHref();
				}
			}
			if (url == null) {
				throw new WpsException(
						"Capabilities do not contain any information about the entry point for Execute operation.");
			}
		}
		execute.getExecute().setVersion(SUPPORTED_VERSION);
		return retrieveExecuteResponseViaPOST(url, execute,rawData);
	}
	
	private InputStream retrieveDataViaPOST(XmlObject obj, String urlString) throws WpsException{
		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setRequestProperty("Content-Type", "text/xml");
			if (this.urlConnectionAuthenticator != null) {
				urlConnectionAuthenticator.authenticate("WPS", "Execute", conn);
			}
			conn.setDoOutput(true);
			obj.save(conn.getOutputStream());
			InputStream input = null;
			String encoding = conn.getContentEncoding();
			int responseCode = conn.getResponseCode();
			if(responseCode == 400) {
				input = conn.getErrorStream();
			}
			else if(encoding != null && encoding.equalsIgnoreCase("gzip")) {
				input = new GZIPInputStream(conn.getInputStream());
			}
			else {
				input = conn.getInputStream();
			}
			return input;
		} catch (MalformedURLException e) {
			throw new WpsException("URL seems to be unvalid", e);
		} catch (IOException e) {
			throw new WpsException("Error while transmission", e);
		}
	}
	
	private Document checkInputStream(InputStream is) throws WpsException {
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		fac.setNamespaceAware(true);
		try {
			Document doc = fac.newDocumentBuilder().parse(is);
			if(getFirstElementNode(doc.getFirstChild()).getLocalName().equals("ExceptionReport") && getFirstElementNode(doc.getFirstChild()).getNamespaceURI().equals(OGC_OWS_URI)) {
				try {
					ExceptionReportDocument exceptionDoc = ExceptionReportDocument.Factory.parse(doc);
					LOG.debug(exceptionDoc.xmlText(options));
					throw new WpsException("Error occured while executing query: " + exceptionDoc.getExceptionReport().toString());
				}
				catch(XmlException e) {
					throw new WpsException("Error while parsing ExceptionReport retrieved from server", e);
				}
			}
			return doc;
		} catch (SAXException e) {
			throw new WpsException("Error while parsing input.", e);
		} catch (IOException e) {
			throw new WpsException("Error occured while transfer", e);
		} catch (ParserConfigurationException e) {
			throw new WpsException("Error occured, parser is not correctly configured", e);
		}
	}

	private Node getFirstElementNode(Node node) {
		if(node == null) {
			return null;
		}
		if(node.getNodeType() == Node.ELEMENT_NODE) {
			return node;
		}
		else {
			return getFirstElementNode(node.getNextSibling());
		}
		
	}
	/**
	 * either an ExecuteResponseDocument or an InputStream if asked for RawData or an Exception Report
	 * @param url
	 * @param doc
	 * @param rawData
	 * @return
	 * @throws WpsException
	 */
	private Object retrieveExecuteResponseViaPOST(String url, ExecuteDocument doc, boolean rawData) throws WpsException{
		InputStream is = retrieveDataViaPOST(doc, url);
		if(rawData) {
			return is;
		}
		Document documentObj = checkInputStream(is);
		ExceptionReportDocument erDoc = null;
		try {
			return ExecuteResponseDocument.Factory.parse(documentObj);
		}
		catch(XmlException e) {
			try {
				erDoc = ExceptionReportDocument.Factory.parse(documentObj);
			} catch (XmlException e1) {
				throw new WpsException("Error occured while parsing executeResponse", e);
			}
			return erDoc;
		}
	}
	
	/**
	 * Executes a process at a WPS
	 * 
	 * @param url url of server not the entry additionally defined in the caps.
	 * @param executeAsGETString KVP Execute request
	 * @return either an ExecuteResponseDocument or an InputStream if asked for RawData or an Exception Report 
	 */
	public Object executeViaGET(String url, String executeAsGETString) throws WpsException {
		url = url + executeAsGETString;
		try {
			URL urlObj = new URL(url);
			URLConnection conn = urlObj.openConnection();
			if (this.urlConnectionAuthenticator != null) {
				urlConnectionAuthenticator.authenticate("WPS", "Execute", conn);
			}
			
			InputStream is = conn.getInputStream();
		
			if(executeAsGETString.toUpperCase().contains("RAWDATA")){
				return is;
			}
			Document doc = checkInputStream(is);
			ExceptionReportDocument erDoc = null;
			try {
				return ExecuteResponseDocument.Factory.parse(doc);
			}
			catch(XmlException e) {
				try {
					erDoc = ExceptionReportDocument.Factory.parse(doc);
				} catch (XmlException e1) {
					throw new WpsException("Error occured while parsing executeResponse", e);
				}
				throw new WpsException("Error occured while parsing executeResponse :" + erDoc.getExceptionReport().toString());
			}
		} catch (MalformedURLException e) {
			throw new WpsException("Execute GET URL seems to be invalid: " + url, e);
		} catch (IOException e) {
			throw new WpsException("Error occured while executing via GET a process from url: " + url, e);
		}
		
	}
	
	
	/**
	 * Calls the status location returned by a WPS process
	 * 
	 * @param url url of status location
	 */
	public Object getStatus(String statusLocation) throws WpsException {
		try {
			URL urlObj = new URL(statusLocation);
			URLConnection conn = urlObj.openConnection();
			if (this.urlConnectionAuthenticator != null) {
				urlConnectionAuthenticator.authenticate("WPS", "GetStatus", conn);
			}
			
			InputStream is = conn.getInputStream();
			Document doc = checkInputStream(is);
			ExceptionReportDocument erDoc = null;
			try {
				return ExecuteResponseDocument.Factory.parse(doc);
			}
			catch(XmlException e) {
				try {
					erDoc = ExceptionReportDocument.Factory.parse(doc);
				} catch (XmlException e1) {
					throw new WpsException("Error occured while parsing executeResponse", e);
				}
				throw new WpsException("Error occured while parsing executeResponse :" + erDoc.getExceptionReport().toString());
			}
		} catch (MalformedURLException e) {
			throw new WpsException("Status Location URL seems to be unvalid: " + statusLocation, e);
		} catch (IOException e) {
			throw new WpsException("Error occured while retrieving status location from url: " + statusLocation, e);
		}
		
	}
}
