package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.rpc.Credentials;
import com.cgi.eoss.osiris.rpc.GetCredentialsParams;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.log4j.Log4j2;
import net.opengis.ows.x11.BoundingBoxType;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import org.n52.wps.client.ExecuteRequestBuilder;
import org.n52.wps.client.WPSClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Log4j2
public class WpsExecutionControllerImpl implements WpsExecutionController {

	private OsirisServerClient osirisServerClient;
	
	Cache<String, CachingWpsClient> wpsClients;
	
	@Autowired
	public WpsExecutionControllerImpl(OsirisServerClient osirisServerClient) {
		this.osirisServerClient = osirisServerClient;
		wpsClients = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
				
	}
	@Override
	public CapabilitiesDocument getCapabilities(String url) throws WpsException {
		CachingWpsClient wpsClient = getWpsClientForServer(url);
		return wpsClient.getCapabilities(url);
	}
	
	private CachingWpsClient getWpsClientForServer(String url) throws WpsException {
		try {
			return wpsClients.get(url, () -> createWpsClientForServer(url));
		} catch (ExecutionException e) {
			throw new WpsException("Cannot create WPS Client", e);
		}
	}
	
	private CachingWpsClient createWpsClientForServer(String url) {
		CachingWpsClient w = new CachingWpsClient();
		URLConnectionAuthenticator authenticator = getAuthenticator(url);
		w.setUrlConnectionAuthenticator(authenticator);
		return w;
		
	}

	private URLConnectionAuthenticator getAuthenticator(String url) {
		if (osirisServerClient == null) {
			return null;
		}
		Credentials creds;
		try {
			creds = osirisServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(URI.create(url).getHost()).build());
			if (creds == null) {
	        	return null;
			}
	    } catch (Exception e) {
			return null;
		}
        switch (creds.getType()) {
        	case PKCS8: try {
				return new JwtConnectionAuthenticator(readPrivateKey(creds.getData()));
			} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				LOG.error("Credentials authentication failed - continuing without credentials", e);
				return null;
			}
        	default: return null;
        }
	}
	
	private Key readPrivateKey(String privateKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] privateKeyDER = Base64.getDecoder().decode(privateKeyPEM);

		PKCS8EncodedKeySpec spec =
			      new PKCS8EncodedKeySpec(privateKeyDER);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}
	
	@Override
	public ProcessDescriptionsDocument describeProcess(String url, String processId) throws WpsException {
		CachingWpsClient wpsClient = getWpsClientForServer(url);
		return wpsClient.describeProcess(url, processId, true);
	}

	@Override
	public Object executeWpsProcess(String url, String processId, boolean requestOutputStorage, 
			Map<String, ComplexDataParam> complexParams, 
			Map<String, LiteralDataParam> literalDataParams, 
			Map<String, BoundingBoxDataParam> boundingBoxDataParams,
			Map<String, ComplexDataOutputDefinition> complexDataOutputDefinitions) throws WpsException{
    	CachingWpsClient wpsClient = getWpsClientForServer(url);
		
		ProcessDescriptionType processDescription = getProcessDescription(wpsClient, url, processId);
		if (requestOutputStorage && !processDescription.getStoreSupported()) {
			throw new WpsException("Output storage requested but not supported");
		}
		LOG.debug("Echo process description:\n" + processDescription.xmlText() + "\n");
		ExecuteDocument executeDocument = createExecuteDocument(processDescription, requestOutputStorage, complexParams, literalDataParams, boundingBoxDataParams, complexDataOutputDefinitions);

		LOG.info("Sending execute request:\n" + executeDocument.xmlText() + "\n");
		Object response;
		response = wpsClient.execute(url, executeDocument, true);
		LOG.info("Got response:\n" + response.toString() + "\n");
		return response;
	}
	
	@Override
	public ExecuteResponseDocument getWpsStatus(String url) throws WpsException {
		CachingWpsClient wpsClient = getWpsClientForServer(url);
		return (ExecuteResponseDocument) wpsClient.getStatus(url);
	}
	
	@Override
	public ExecuteResponseDocument executeViaGET(String url) throws WpsException {
		CachingWpsClient wpsClient = getWpsClientForServer(url);
		Object response = wpsClient.executeViaGET(url, "");
		LOG.info("Got response:\n" + response.toString() + "\n");
		return (ExecuteResponseDocument) response;
	}

	private ProcessDescriptionType getProcessDescription(CachingWpsClient wpsClient, String url,
			String processId) throws WpsException {
		return wpsClient.describeProcess(url, processId, true).getProcessDescriptions().getProcessDescriptionArray(0);
	}
	

	private ExecuteDocument createExecuteDocument(ProcessDescriptionType processDescription, 
			boolean requestOutputStorage, 
			Map<String, ComplexDataParam> complexDataParams, 
			Map<String, LiteralDataParam> literalDataParams, 
			Map<String, BoundingBoxDataParam> boundingBoxDataParams,
			Map<String, ComplexDataOutputDefinition> complexDataOutputsDefinitions)
			throws WpsException {
		ExecuteRequestBuilder executeBuilder = new ExecuteRequestBuilder(processDescription);
		prepareInputs(executeBuilder, processDescription, complexDataParams, literalDataParams, boundingBoxDataParams);
		prepareOutputs(executeBuilder, processDescription, requestOutputStorage, complexDataOutputsDefinitions);
		if (!executeBuilder.isExecuteValid()) {
			throw new WpsException("Created execute request is NOT valid.");
		}
		ExecuteDocument executeDocument = executeBuilder.getExecute();
		LOG.debug(executeDocument);
		return executeDocument;
	}

	private void prepareInputs(ExecuteRequestBuilder executeBuilder, ProcessDescriptionType processDescription,
			Map<String, ComplexDataParam> complexDataParams, Map<String, LiteralDataParam> literalDataParams, Map<String, BoundingBoxDataParam> boundingBoxDataParams) throws WpsException {
		Map<String, InputDescriptionType> inputDescriptionMap = Arrays.stream(processDescription.getDataInputs().getInputArray()).collect(Collectors.toMap(i -> i.getIdentifier().toString(), Functions.identity()));
		
		for (Entry<String, ComplexDataParam> complexDataParamEntry: complexDataParams.entrySet()) {
			validateComplexDataParam(complexDataParamEntry.getKey(), complexDataParamEntry.getValue(), inputDescriptionMap);
			 try {
				executeBuilder.addComplexData(complexDataParamEntry.getKey(), complexDataParamEntry.getValue().getValue(), 
						 complexDataParamEntry.getValue().getSchema(), complexDataParamEntry.getValue().getEncoding(),
						 complexDataParamEntry.getValue().getMimeType());
			} catch (WPSClientException e) {
				throw new WpsException(e);
			}
		}
		
		for (Entry<String, LiteralDataParam> literalDataParamEntry: literalDataParams.entrySet()) {
			validateLiteralDataParam(literalDataParamEntry.getKey(), literalDataParamEntry.getValue(), inputDescriptionMap);
			executeBuilder.addLiteralData(literalDataParamEntry.getKey(), literalDataParamEntry.getValue().getValue());
			
		}
		
		for (Entry<String, BoundingBoxDataParam> boundingBoxDataParam: boundingBoxDataParams.entrySet()) {
			//Support this externally as it is not supported currently by underlying library
			addBoundingBoxData(executeBuilder, boundingBoxDataParam.getKey(), boundingBoxDataParam.getValue());
		}
		
	}
	
	

	private void addBoundingBoxData(ExecuteRequestBuilder executeBuilder, String parameterID, BoundingBoxDataParam boundingBoxDataParam) {
		ExecuteDocument executeDocument = executeBuilder.getExecute();
		InputType input = executeDocument.getExecute().getDataInputs().addNewInput();
		input.addNewIdentifier().setStringValue(parameterID);
		BoundingBoxType boundingBoxData = input.addNewData().addNewBoundingBoxData();
		boundingBoxData.setCrs(boundingBoxDataParam.getCrs());
		boundingBoxData.setDimensions(BigInteger.valueOf(boundingBoxDataParam.getDimensions()));
		String[] lowerCornerCoordsUnparsed = boundingBoxDataParam.getLowerCorner().split(" ");	
		List<Double> lowerCornerCoords = Arrays.stream(lowerCornerCoordsUnparsed).map( Double::parseDouble).collect(Collectors.toList());
		String[] upperCornerCoordsUnparsed = boundingBoxDataParam.getUpperCorner().split(" ");	
		List<Double> upperCornerCoords = Arrays.stream(upperCornerCoordsUnparsed).map( Double::parseDouble).collect(Collectors.toList());
		boundingBoxData.setLowerCorner(lowerCornerCoords);
		boundingBoxData.setUpperCorner(upperCornerCoords);
	}
	
	private void prepareOutputs(ExecuteRequestBuilder executeBuilder, ProcessDescriptionType processDescription, boolean requestOutputStorage,
			Map<String, ComplexDataOutputDefinition> complexOutputDefinitions) {
		Map<String, OutputDescriptionType> outputDescriptionMap = Arrays.stream(processDescription.getProcessOutputs().getOutputArray()).collect(Collectors.toMap(i -> i.getIdentifier().toString(), Functions.identity()));
		for (OutputDescriptionType processOutput: processDescription.getProcessOutputs().getOutputArray()) {
			for (Entry<String, ComplexDataOutputDefinition> entry: complexOutputDefinitions.entrySet()) {
				ComplexDataOutputDefinition complexOutputDefinition = entry.getValue();
				validateComplexDataOutputDefinition(entry.getKey(), complexOutputDefinition, outputDescriptionMap);
				executeBuilder.setResponseDocument(entry.getKey(), complexOutputDefinition.getSchema(), complexOutputDefinition.getEncoding(), complexOutputDefinition.getMimeType());
			}
			
			if(requestOutputStorage) {
				executeBuilder.setStoreSupport(processOutput.getIdentifier().getStringValue(), true);
				executeBuilder.setStatus(processOutput.getIdentifier().getStringValue(), requestOutputStorage);
				executeBuilder.setResponseDocument(processOutput.getIdentifier().getStringValue(), null, null, null);
				executeBuilder.setAsReference(processOutput.getIdentifier().getStringValue(), true);
			}
		}
	}
	
	private void validateLiteralDataParam(String identifier, LiteralDataParam literalDataParam, Map<String, InputDescriptionType> inputDescriptionMap) {
		// TODO Check that the specification is in line with the process description
		
	}
	
	private void validateComplexDataParam(String identifier, ComplexDataParam complexDataParam, Map<String, InputDescriptionType> inputDescriptionMap) {
		// TODO Check that the specification is in line with the process description
	}
	
	private void validateComplexDataOutputDefinition(String identifier, ComplexDataOutputDefinition complexDataOutputDefinition, Map<String, OutputDescriptionType> inputDescriptionMap) {
		// TODO Check that the specification is in line with the process description
	}
	
}