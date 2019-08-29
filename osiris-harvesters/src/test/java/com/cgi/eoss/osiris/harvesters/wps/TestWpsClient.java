package com.cgi.eoss.osiris.harvesters.wps;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class TestWpsClient {
	
	private MockWebServer mockWebServer;

	@Before
	public void setUp() throws IOException, URISyntaxException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		InputStream getCapabilitiesIs = Files.newInputStream(Paths.get(TestWpsClient.class.getResource("/test-get-capabilities.xml").toURI()));
		byte[] getCapabilitiesBytes = IOUtils.toByteArray(getCapabilitiesIs);
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(getCapabilitiesBytes)));
		InputStream describeProcessIs = Files.newInputStream(Paths.get(TestWpsClient.class.getResource("/test-describe-process.xml").toURI()));
		byte[] describeProcessBytes = IOUtils.toByteArray(describeProcessIs);
		InputStream executeResponseIs = Files.newInputStream(Paths.get(TestWpsClient.class.getResource("/test-execute-response.xml").toURI()));
		byte[] executeResponseBytes = IOUtils.toByteArray(executeResponseIs);
		
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(describeProcessBytes)));
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(describeProcessBytes)));
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(executeResponseBytes)));
	}

	@Test
	public void test() throws WpsException, IOException, URISyntaxException {
		
		WpsExecutionController wpsExecutionController = new WpsExecutionControllerImpl(null);
        String serverUrl = mockWebServer.url("/wps").toString();
        CapabilitiesDocument capabilities = wpsExecutionController.getCapabilities(serverUrl);
        ProcessBriefType processBrief = capabilities.getCapabilities().getProcessOfferings().getProcessArray()[0];
		assertThat(processBrief.getIdentifier().getStringValue(), is ("JTS:buffer"));
        ProcessDescriptionsDocument describeProcess = wpsExecutionController.describeProcess(serverUrl, "JTS:buffer");
        ProcessDescriptionType processDesc = describeProcess.getProcessDescriptions().getProcessDescriptionArray()[0];
        assertThat(processDesc.getIdentifier().getStringValue(), is ("JTS:buffer"));
        assertThat(processDesc.getDataInputs().getInputArray().length, is (4));
        Object response = wpsExecutionController.executeWpsProcess(serverUrl, 
            	"JTS:buffer", false, 
            	ImmutableMap.of("geom", new ComplexDataParam(null, null, "application/wkt", "POINT(0 0)")), 
            	ImmutableMap.of("distance", new LiteralDataParam("2.0")), 
            	Collections.emptyMap(), 
            	Collections.emptyMap()
            	);
       ExecuteResponseDocument executeResponseDocument = (ExecuteResponseDocument) response;
       assertThat (executeResponseDocument.getExecuteResponse().getProcessOutputs().getOutputArray().length, is (1));
    }
	
	@After
	public void tearDown() throws IOException {
		mockWebServer.close();
	}

}
