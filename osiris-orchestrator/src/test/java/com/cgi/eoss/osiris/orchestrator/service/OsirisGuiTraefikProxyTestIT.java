package com.cgi.eoss.osiris.orchestrator.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.orchestrator.OrchestratorConfig;
import com.cgi.eoss.osiris.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.osiris.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.osiris.persistence.service.GroupDataService;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.worker.Binding;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;
import com.cgi.eoss.osiris.rpc.worker.PortBindings;
import com.cgi.eoss.osiris.security.OsirisSecurityService;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource(locations = "classpath:test-orchestrator.properties", properties = {"osiris.orchestrator.proxy.enabled=true", "osiris.orchestrator.proxy.traefik.enabled=true"})
@Transactional
public class OsirisGuiTraefikProxyTestIT {

    @Autowired
    private OsirisGuiServiceManager osirisGuiServiceManager;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    private OsirisWorkerGrpc.OsirisWorkerBlockingStub worker;

    private Server server;
    
    private MockWebServer webServer;
    
	private JobDataService jobDataService;

	private GroupDataService groupDataService;
	
	private OsirisSecurityService securityService;

    @Before
    public void setUp() throws IOException {
        serverBuilder.addService(new WorkerStub());
        server = serverBuilder.build().start();
        worker = OsirisWorkerGrpc.newBlockingStub(channelBuilder.build());
        webServer = new MockWebServer();
        webServer.start();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
        try {
			webServer.shutdown();
		} catch (IOException e) {
			
		}
    }

    @Test
    public void getProxyEntry() throws Exception {
    	Job job =  Job.newBuilder().setId("test-id").build();
        PortBinding guiPortBinding = osirisGuiServiceManager.getGuiPortBinding(worker,job);
        jobDataService = mock(JobDataService.class);
        groupDataService = mock(GroupDataService.class);
        securityService = mock(OsirisSecurityService.class);
        TraefikProxyService dynamicProxyService = new TraefikProxyService(webServer.url("").toString(), "test", "test", "http://osiris", "/gui/", false, null, jobDataService, groupDataService, securityService);
        ReverseProxyEntry proxyEntry = dynamicProxyService.getProxyEntry(job,guiPortBinding.getBinding().getIp(), guiPortBinding.getBinding().getPort());
        assertThat(proxyEntry.getPath(), is("http://osiris/gui/test-id/"));
        assertThat(proxyEntry.getEndpoint(), is("http://" + guiPortBinding.getBinding().getIp() + ":"  + guiPortBinding.getBinding().getPort()));
    }
    
    @Test
    public void testUpdate() throws Exception {
    	User testUser = new User("test");
    	OsirisService testService = new OsirisService("testService", testUser, "testTag");
    	com.cgi.eoss.osiris.model.JobConfig jobConfig = new com.cgi.eoss.osiris.model.JobConfig(testUser, testService);
    	com.cgi.eoss.osiris.model.Job job = new com.cgi.eoss.osiris.model.Job(jobConfig, "test-id", testUser);
    	 jobDataService = mock(JobDataService.class);
         when(jobDataService.findByStatusAndGuiUrlNotNull(Status.RUNNING)).thenReturn(Collections.singletonList(job));
     	TraefikProxyService dynamicProxyService = new TraefikProxyService(webServer.url("").toString(), "test", "test", "http://osiris", "/gui/", false, null, jobDataService, groupDataService, securityService);
    	PortBinding guiPortBinding = osirisGuiServiceManager.getGuiPortBinding(worker,ModelToGrpcUtils.toRpcJob(job));
        ReverseProxyEntry proxyEntry = dynamicProxyService.getProxyEntry(ModelToGrpcUtils.toRpcJob(job),guiPortBinding.getBinding().getIp(), guiPortBinding.getBinding().getPort());
        job.setGuiUrl(proxyEntry.getPath());
        job.setGuiEndpoint(proxyEntry.getEndpoint());
        dynamicProxyService.update();
    	RecordedRequest request = webServer.takeRequest();
    }

    private class WorkerStub extends OsirisWorkerGrpc.OsirisWorkerImplBase {
        @Override
        public void getPortBindings(Job request, StreamObserver<PortBindings> responseObserver) {
            PortBinding portBinding = PortBinding.newBuilder()
                    .setPortDef("8080/tcp")
                    .setBinding(Binding.newBuilder().setIp("127.0.0.1").setPort(12345).build())
                    .build();

            responseObserver.onNext(PortBindings.newBuilder().addBindings(portBinding).build());
            responseObserver.onCompleted();
        }
    }

}