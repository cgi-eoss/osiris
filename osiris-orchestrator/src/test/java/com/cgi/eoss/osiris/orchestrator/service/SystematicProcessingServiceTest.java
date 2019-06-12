package com.cgi.eoss.osiris.orchestrator.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.orchestrator.OrchestratorConfig;
import com.cgi.eoss.osiris.orchestrator.OrchestratorTestConfig;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.rpc.SystematicProcessingResponse;
import com.cgi.eoss.osiris.rpc.SystematicProcessingServiceGrpc;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource("classpath:test-orchestrator.properties")
@Transactional
public class SystematicProcessingServiceTest {

    @Mock
    private SystematicProcessingDataService systematicProcessingDataService;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    private Server server;

    private SystematicProcessingServiceGrpc.SystematicProcessingServiceBlockingStub stub;

    @Mock
    private ScheduledJobService scheduledJobService;
    
    @Autowired 
    private TaskScheduler taskScheduler;
    
    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        serverBuilder.addService(new SystematicProcessingService(systematicProcessingDataService, scheduledJobService, 3000, taskScheduler));
        server = serverBuilder.build().start();
        stub = SystematicProcessingServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testLaunchSystematicService() {

        String userId = "user";
        String serviceId = "service";
        String jobConfigLabel = "jobConfigLabel";
        String systematicParam = "systematicParam";
        Multimap<String, String> inputsMap = ArrayListMultimap.create();
        inputsMap.putAll("testInput", ImmutableList.of("value1", "value2"));
        inputsMap.put("otherInput", "something");

        ListMultimap<String, String> searchParams = ArrayListMultimap.create();
        searchParams.put("collection", "sentinel1");
        searchParams.put("processingLevel", "L1B");

        SystematicProcessing dummySysProc = new SystematicProcessing();
        dummySysProc.setSearchParameters(searchParams);
        dummySysProc.setId(5L);
        when(systematicProcessingDataService.buildNew(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(dummySysProc);

        SystematicProcessingRequest.Builder builder = SystematicProcessingRequest.newBuilder();
        builder.setUserId(userId);
        builder.setServiceId(serviceId);
        builder.setJobConfigLabel(jobConfigLabel);
        builder.setSystematicParameter(systematicParam);
        builder.addAllInput(GrpcUtil.mapToParams(inputsMap));
        builder.addAllSearchParameter(GrpcUtil.mapToParams(searchParams));
        SystematicProcessingResponse response = stub.launch(builder.build());

        assertThat(response.getSystematicProcessingId(), is(dummySysProc.getId()));

        ArgumentMatcher<ListMultimap<String, String>> isSearchParams = argument -> {
            boolean matches = true;
            for (Map.Entry<String, String> entry : searchParams.entries()) {
                if (!argument.containsEntry(entry.getKey(), entry.getValue())) {
                    matches = false;
                }
            }
            if (!(argument.containsEntry("sortOrder", "ascending") && argument.containsEntry("sortParam", "updated"))) {
                matches = false;
            }
            return matches;
        };

        verify(systematicProcessingDataService, times(1)).buildNew(any(), eq(userId), eq(serviceId), eq(jobConfigLabel),
                eq(systematicParam), eq(inputsMap), ArgumentMatchers.argThat(isSearchParams), any(), any());
    }
}