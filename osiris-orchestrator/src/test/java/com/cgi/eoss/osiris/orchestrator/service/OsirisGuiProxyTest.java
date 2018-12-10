package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.orchestrator.OrchestratorConfig;
import com.cgi.eoss.osiris.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.worker.Binding;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;
import com.cgi.eoss.osiris.rpc.worker.PortBindings;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource("classpath:test-orchestrator.properties")
@Transactional
public class OsirisGuiProxyTest {

    @Autowired
    private OsirisGuiServiceManager osirisGuiServiceManager;

    @Autowired
    private DynamicProxyService dynamicProxyService;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    private OsirisWorkerGrpc.OsirisWorkerBlockingStub worker;

    private Server server;

    @Before
    public void setUp() throws IOException {
        serverBuilder.addService(new WorkerStub());
        server = serverBuilder.build().start();

        worker = OsirisWorkerGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void getGuiUrl() throws Exception {
        PortBinding guiPortBinding = osirisGuiServiceManager.getGuiPortBinding(worker, Job.getDefaultInstance());
        ReverseProxyEntry proxyEntry = dynamicProxyService.getProxyEntry(Job.getDefaultInstance(),guiPortBinding.getBinding().getIp(), guiPortBinding.getBinding().getPort());
        assertThat(proxyEntry.getPath(), is("/gui/:12345/"));
        
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