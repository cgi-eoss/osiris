package com.cgi.eoss.osiris.wps;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.clouds.local.LocalNodeFactory;
import com.cgi.eoss.osiris.clouds.service.NodeFactory;
import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.io.ServiceInputOutputManager;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.Wallet;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata;
import com.cgi.eoss.osiris.orchestrator.service.DynamicProxyService;
import com.cgi.eoss.osiris.orchestrator.service.OsirisGuiServiceManager;
import com.cgi.eoss.osiris.orchestrator.service.OsirisServiceLauncher;
import com.cgi.eoss.osiris.orchestrator.service.ReverseProxyEntry;
import com.cgi.eoss.osiris.orchestrator.service.CachingWorkerFactory;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.rpc.worker.Binding;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.worker.worker.OsirisWorker;
import com.cgi.eoss.osiris.worker.worker.OsirisWorkerNodeManager;
import com.cgi.eoss.osiris.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.MoreFiles;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.core.DefaultDockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientBuilder;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.RemoteApiVersion;
import shadow.dockerjava.com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * <p>Integration test for launching WPS services.</p> <p><strong>This uses a real Docker engine to build and run a
 * container!</strong></p>
 */
public class OsirisServicesClientIT {
    private static final String RPC_SERVER_NAME = OsirisServicesClientIT.class.getName();
    private static final String SERVICE_NAME = "service1";
    private static final String PARALLEL_SERVICE_NAME = "service2";
    private static final String TEST_CONTAINER_IMAGE = "alpine:3.7";

    @Mock
    private OsirisGuiServiceManager guiService;

    @Mock
    private JobDataService jobDataService;

    @Mock
    private CatalogueService catalogueService;

    @Mock
    private CostingService costingService;
    
    @Mock
    private DynamicProxyService dynamicProxyService;

    private Path workspace;
    private Path dataDir;
    private Path ingestedOutputsDir;

    private OsirisServicesClient osirisServicesClient;

    private Server server;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspace = Files.createTempDirectory(Paths.get("target"), OsirisServicesClientIT.class.getSimpleName());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                MoreFiles.deleteRecursively(workspace);
            } catch (IOException ignored) {
            }
        }));
        ingestedOutputsDir = workspace.resolve("ingestedOutputsDir");
        Files.createDirectories(ingestedOutputsDir);
        dataDir = workspace.resolve("dataDir");
        Files.createDirectories(dataDir);
        
        ReverseProxyEntry proxyEntry = new ReverseProxyEntry("/test", "127.0.0.1:32000");
        
        when(dynamicProxyService.getProxyEntry(any(), any(), anyInt())).thenReturn(proxyEntry);
        
        when(catalogueService.provisionNewOutputProduct(any(), any())).thenAnswer(invocation -> {
            Path outputPath = ingestedOutputsDir.resolve(((OutputProductMetadata) invocation.getArgument(0)).getJobId()).resolve((String) invocation.getArgument(1));
            Files.createDirectories(outputPath.getParent());
            return outputPath;
        });
        
        when(guiService.getGuiPortBinding(any(), any())).thenReturn(PortBinding.newBuilder().setBinding(Binding.newBuilder().setIp("127.0.0.1").setPort(32000).build()).build());
        
        when(catalogueService.ingestOutputProduct(any(), any())).thenAnswer(invocation -> {
            OutputFileMetadata outputFileMetadata = (OutputFileMetadata) invocation.getArgument(0);
            Path outputPath = (Path) invocation.getArgument(1);
            OsirisFile osirisFile = new OsirisFile(URI.create("osiris://outputs/" + ingestedOutputsDir.relativize(outputPath)), UUID.randomUUID());
            osirisFile.setFilename(ingestedOutputsDir.relativize(outputPath).toString());
            return osirisFile;
        });
        
       
        JobEnvironmentService jobEnvironmentService = spy(new JobEnvironmentService(workspace));
        ServiceInputOutputManager ioManager = mock(ServiceInputOutputManager.class);
        Mockito.when(ioManager.getServiceContext(SERVICE_NAME)).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());

        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
        NodeFactory nodeFactory = new LocalNodeFactory(-1, "unix:///var/run/docker.sock");
        OsirisWorkerNodeManager nodeManager = new OsirisWorkerNodeManager(nodeFactory, dataDir, Integer.MAX_VALUE);
        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(RPC_SERVER_NAME).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(RPC_SERVER_NAME).directExecutor();

        CachingWorkerFactory workerFactory = mock(CachingWorkerFactory.class);
        OsirisSecurityService securityService = mock(OsirisSecurityService.class);

        OsirisServiceLauncher osirisServiceLauncher = new OsirisServiceLauncher(workerFactory, jobDataService, guiService, catalogueService, costingService, securityService, dynamicProxyService);
        OsirisWorker osirisWorker = new OsirisWorker(nodeManager, jobEnvironmentService, ioManager, 0);

        when(workerFactory.getWorker(any())).thenReturn(OsirisWorkerGrpc.newBlockingStub(channelBuilder.build()));

        inProcessServerBuilder.addService(osirisServiceLauncher);
        inProcessServerBuilder.addService(osirisWorker);

        server = inProcessServerBuilder.build().start();

        osirisServicesClient = new OsirisServicesClient(channelBuilder);

        // Ensure the test image is available before testing
        dockerClient.pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void launchApplication() throws Exception {
        OsirisService service = mock(OsirisService.class);
        OsirisServiceDescriptor serviceDescriptor = mock(OsirisServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("osiris-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("osiris/testservice1");
        when(service.getType()).thenReturn(OsirisService.Type.APPLICATION); // Trigger ingestion of all outputs
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        List<Job> launchedJobs = new ArrayList<>();
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = osirisServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(1));

        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.get("1"), containsInAnyOrder("osiris://outputs/" + launchedJobs.get(0).getExtId() + "/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/OSIRIS-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

    @Test
    public void launchProcessor() throws Exception {
        OsirisService service = mock(OsirisService.class);
        OsirisServiceDescriptor serviceDescriptor = mock(OsirisServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("osiris-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("osiris/testservice1");
        when(service.getType()).thenReturn(OsirisService.Type.PROCESSOR);
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(serviceDescriptor.getDataOutputs()).thenReturn(ImmutableList.of(
                OsirisServiceDescriptor.Parameter.builder().id("output").build()
        ));
        List<Job> launchedJobs = new ArrayList<>();
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = osirisServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(1));

        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.get("output"), containsInAnyOrder("osiris://outputs/" + launchedJobs.get(0).getExtId() + "/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/OSIRIS-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

    @Test
    public void launchParallelProcessor() throws Exception {
        OsirisService service = mock(OsirisService.class);
        OsirisServiceDescriptor serviceDescriptor = mock(OsirisServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("osiris-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(PARALLEL_SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("osiris/testservice1");
        when(service.getType()).thenReturn(OsirisService.Type.PARALLEL_PROCESSOR);
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(serviceDescriptor.getDataOutputs()).thenReturn(ImmutableList.of(
                OsirisServiceDescriptor.Parameter.builder().id("output").build()
        ));

        final long[] jobCount = {0L};
        List<Job> launchedJobs = new ArrayList<>();

        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(jobCount[0]++);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("parallelInputs", "parallelInput1,parallelInput2,parallelInput3")
                .put("sharedInputFoo", "foo")
                .put("sharedInputBarBaz", "bar,baz")
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = osirisServicesClient.launchService(userId, PARALLEL_SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(4));

        assertThat(outputs, is(notNullValue()));
        assertThat(ImmutableSet.copyOf(outputs.get("output")), is(Seq.of(1, 2, 3).stream()
                .map(i -> {
                    Job job = launchedJobs.get(i);
                    return "osiris://outputs/" + job.getExtId() + "/output_file_1";
                })
                .collect(toSet())));

        assertThat(Files.exists(workspace.resolve("Job_" + launchedJobs.get(0).getExtId()).resolve("OSIRIS-WPS-INPUT.properties")), is(false));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(1).getExtId()).resolve("OSIRIS-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput1\""
        )));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(2).getExtId()).resolve("OSIRIS-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput2\""
        )));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(3).getExtId()).resolve("OSIRIS-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput3\""
        )));

        assertThat(Files.exists(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1")), is(false));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(1).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput1"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(2).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput2"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(3).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput3"
        )));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

}
