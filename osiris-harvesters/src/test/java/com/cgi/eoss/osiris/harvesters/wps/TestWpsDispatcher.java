package com.cgi.eoss.osiris.harvesters.wps;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.cgi.eoss.osiris.harvesters.HarvestersTestConfig;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.ExecuteWpsParams;
import com.cgi.eoss.osiris.rpc.wps.LiteralDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.WpsJobSpec;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatusUpdate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { HarvestersTestConfig.class})
@TestPropertySource("classpath:test-harvesters.properties")
public class TestWpsDispatcher {

	@Autowired
	OsirisQueueService queueService;
	
	private MockWebServer mockWebServer;
	
	@MockBean
    private OsirisServerClient osirisServerClient;
	
	@Before
	public void setUp() throws IOException, URISyntaxException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		InputStream describeProcessIs = Files.newInputStream(Paths.get(TestWpsClient.class.getResource("/test-describe-process.xml").toURI()));
		byte[] describeProcessBytes = IOUtils.toByteArray(describeProcessIs);
		InputStream executeResponseIs = Files.newInputStream(Paths.get(TestWpsClient.class.getResource("/test-execute-response.xml").toURI()));
		byte[] executeResponseBytes = IOUtils.toByteArray(executeResponseIs);
		
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(describeProcessBytes)));
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(describeProcessBytes)));
		mockWebServer.enqueue(new MockResponse().setHeader("Content-Type", "applicaton/xml").setBody(new String(executeResponseBytes)));
    }
	
	@Test
	public void testWpsJob() {
		Job job = Job.newBuilder().setId("test").setIntJobId("1").setServiceId("test").build();
		WpsJobSpec wpsJobSpec = WpsJobSpec.newBuilder()
				.setJob(job)
				.setExecuteWpsParams(createExecuteWpsParams())
				.build();
		queueService.sendObject(OsirisQueueService.wpsJobQueueName, wpsJobSpec);
		Object resp = queueService.receiveObject(OsirisQueueService.wpsJobUpdatesQueueName);
		assertThat(resp instanceof WpsJobStatusUpdate, is(true));
		WpsJobStatusUpdate statusUpdate = (WpsJobStatusUpdate) resp;
		assertThat(statusUpdate.getStatus(), is (WpsJobStatus.SUCCEEDED));
	}
	
	private ExecuteWpsParams createExecuteWpsParams() {
		return ExecuteWpsParams.newBuilder()
				.setWpsServerUrl(mockWebServer.url("/wps").toString())
				.setStoreOutputs(true)
				.setProcessId("JTS:buffer")
				.addComplexDataWpsParam(ComplexDataWpsParam.newBuilder()
						.setParamName("geom")
						.setMimeType("application/wkt")
						.setParamValue("POINT(0 0)")
						.build())
				.addLiteralDataWpsParam(LiteralDataWpsParam.newBuilder()
						.setParamName("distance")
						.setParamValue("2.0")
						.build())
				.build();
	}
}
