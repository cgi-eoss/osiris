package com.cgi.eoss.osiris.orchestrator.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisService.Type;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter.DataNodeType;
import com.cgi.eoss.osiris.model.OsirisServiceDockerBuildInfo;
import com.cgi.eoss.osiris.model.OsirisServiceResources;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserMount;
import com.cgi.eoss.osiris.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.osiris.persistence.service.DatabasketDataService;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.UserMountDataService;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.BuildServiceParams;
import com.cgi.eoss.osiris.rpc.BuildServiceResponse;
import com.cgi.eoss.osiris.rpc.CancelJobParams;
import com.cgi.eoss.osiris.rpc.CancelJobResponse;
import com.cgi.eoss.osiris.rpc.FtpJobSpec;
import com.cgi.eoss.osiris.rpc.FtpJobSpec.Builder;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.JobParam;
import com.cgi.eoss.osiris.rpc.JobSpec;
import com.cgi.eoss.osiris.rpc.OsirisJobLauncherGrpc;
import com.cgi.eoss.osiris.rpc.OsirisJobResponse;
import com.cgi.eoss.osiris.rpc.OsirisServiceParams;
import com.cgi.eoss.osiris.rpc.RelaunchFailedJobParams;
import com.cgi.eoss.osiris.rpc.RelaunchFailedJobResponse;
import com.cgi.eoss.osiris.rpc.ResourceRequest;
import com.cgi.eoss.osiris.rpc.StopFtpJob;
import com.cgi.eoss.osiris.rpc.StopServiceParams;
import com.cgi.eoss.osiris.rpc.StopServiceResponse;
import com.cgi.eoss.osiris.rpc.worker.DockerImageConfig;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc.OsirisWorkerBlockingStub;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsOutputDefinition;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.ExecuteWpsParams;
import com.cgi.eoss.osiris.rpc.wps.LiteralDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.StopWpsJob;
import com.cgi.eoss.osiris.rpc.wps.WpsJobSpec;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * <p>
 * Primary entry point for WPS services to launch in OSIRIS.
 * </p>
 * <p>
 * Provides access to OSIRIS data services and job distribution capability.
 * </p>
 */
@Service
@Log4j2
@GRpcService
public class OsirisJobLauncher extends OsirisJobLauncherGrpc.OsirisJobLauncherImplBase {

    private static final String TIMEOUT_PARAM = "timeout";
    private static final String PARALLEL_INPUT_IDENTIFIER = "parallelInputs";

    private static final int SINGLE_JOB_PRIORITY = 9;

    private final CachingWorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final DatabasketDataService databasketDataService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;
    private final OsirisQueueService queueService;
    private final UserMountDataService userMountDataService;
    private final ServiceDataService serviceDataService;
    private final DynamicProxyService dynamicProxyService;
    @Value("${osiris.orchestrator.gui.baseUrl:http://osiris}")
    private String baseUrl;
    
    @Autowired
    public OsirisJobLauncher(CachingWorkerFactory workerFactory, JobDataService jobDataService,
            DatabasketDataService databasketDataService, OsirisGuiServiceManager guiService,
            CatalogueService catalogueService, CostingService costingService,
            OsirisSecurityService securityService, OsirisQueueService queueService, 
            UserMountDataService userMountDataService,
            ServiceDataService serviceDataService,
            DynamicProxyService dynamicProxyService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.databasketDataService = databasketDataService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
        this.queueService = queueService;
        this.userMountDataService = userMountDataService;
        this.serviceDataService = serviceDataService;
        this.dynamicProxyService = dynamicProxyService;
    }

    @Override
    public void submitJob(OsirisServiceParams request,
            StreamObserver<OsirisJobResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String parentId = request.getJobParent();
        String jobConfigLabel = request.getJobConfigLabel();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.osiris.rpc.Job rpcJob = null;
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("OSIRIS Service Orchestrator").put("userId", userId)
                        .put("serviceId", serviceId).put("zooId", zooId)) {
            
            if (!Strings.isNullOrEmpty(parentId)) {
                //this is a request to attach a subjob to an existing parent
                job = jobDataService.reload(Long.valueOf(parentId));
            }
           
            else {
                job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            } 

            rpcJob = ModelToGrpcUtils.toRpcJob(job);
            // Post back the job metadata for async responses
            responseObserver.onNext(OsirisJobResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            OsirisService service = job.getConfig().getService();

            if (service.getType() == OsirisService.Type.PARALLEL_PROCESSOR) {
                if (!checkInputs(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                
                if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested output collections");
                }
                
                //TODO: Check that the user can use the geoserver spec

                //TODO: externalise remaining "parallelInputs" instances into a static class
                Collection<String> parallelInput = inputs.get(PARALLEL_INPUT_IDENTIFIER);
                if (parallelInput.isEmpty()) {
                    LOG.warn("Attempted to launch a parallel job without any values for required input: \"{}\". No subjobs will be run.", PARALLEL_INPUT_IDENTIFIER);
                }
                List<String> newInputs = explodeParallelInput(parallelInput);
                checkCost(job.getOwner(), job.getConfig(), newInputs);

                if (!checkInputList(job.getOwner(), newInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                jobDataService.save(job);
                List<Job> subJobs = createSubJobs(job, userId, service, newInputs, inputs);

                int i = 0;
                for (Job subJob : subJobs) {
                    chargeUser(subJob.getOwner(), subJob);
                    submitJob(subJob, ModelToGrpcUtils.toRpcJob(subJob),
                            GrpcUtil.mapToParams(subJob.getConfig().getInputs()), getJobPriority(i));
                    i++;
                }

            } 
            else {
                if (!Strings.isNullOrEmpty(parentId)) {
                    Job subJob = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs, job);
                    submitSingleJob(userId, rpcInputs, subJob, ModelToGrpcUtils.toRpcJob(subJob));
                }
                else {
                    submitSingleJob(userId, rpcInputs, job, rpcJob);
                }
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }

            LOG.error("Failed to run processor. Notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(
                    io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

    private void submitSingleJob(String userId, List<JobParam> rpcInputs, Job job, com.cgi.eoss.osiris.rpc.Job rpcJob) throws IOException {
        checkCost(job.getOwner(), job.getConfig());
        if (!checkInputs(job.getOwner(), rpcInputs)) {
            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                LOG.error("User {} does not have read access to all requested inputs",
                        userId);
            }
            throw new ServiceExecutionException(
                    "User does not have read access to all requested inputs");
        }
           //TODO: Check that the user can use the geoserver spec
        if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                LOG.error("User {} does not have read access to all requested output collections",
                        userId);
            }
            throw new ServiceExecutionException(
                    "User does not have read access to all requested output collections");
        }
        
        chargeUser(job.getOwner(), job);
        submitJob(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
    }
    
    @Override
	public void stopJob(StopServiceParams request,
	        StreamObserver<StopServiceResponse> responseObserver) {
	    com.cgi.eoss.osiris.rpc.Job rpcJob = request.getJob();
	    Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
	    OsirisService service = job.getConfig().getService();
	    try {
	    	routeStopRequest(rpcJob, job, service);
	    	LOG.info("Successfully stopped job {}", rpcJob.getId());
	        responseObserver.onNext(StopServiceResponse.newBuilder().build());
	        responseObserver.onCompleted();
	    }
	    catch (Exception e) {
	        LOG.error("Failed to stop job {} - message {}; notifying gRPC client", rpcJob.getId(),
	                e.getMessage());
	        responseObserver.onError(new StatusRuntimeException(
	                io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
	    }
	}

	private void routeStopRequest(com.cgi.eoss.osiris.rpc.Job rpcJob, Job job, OsirisService service) {
		switch (service.getType()) {
	    case APPLICATION:
	    case PROCESSOR:
	    case PARALLEL_PROCESSOR:
	    case BULK_PROCESSOR:
	    	stopDockerJob(rpcJob, job);
	    	return;
	    case FTP_SERVICE:
	    	stopFtpJob(rpcJob, job);
	    	return;
	    case WPS_SERVICE:
	    	stopWpsJob(rpcJob, job);
	    	return;
	    }
		throw new IllegalArgumentException(String.format("Unrecognized service type %s for service %s ", service.getType(), service.getName()));
	}

	private void stopWpsJob(com.cgi.eoss.osiris.rpc.Job rpcJob, Job job) {
		StopWpsJob stopWpsJob = StopWpsJob.newBuilder().setJob(rpcJob).build();
		enqueueJobMessage(OsirisQueueService.wpsJobQueueName, job.getId(), stopWpsJob, 1);
		
	}

	private void stopFtpJob(com.cgi.eoss.osiris.rpc.Job rpcJob, Job job) {
		StopFtpJob stopFtpJob = StopFtpJob.newBuilder().setJob(rpcJob).build();
		enqueueJobMessage(OsirisQueueService.ftpJobQueueName, job.getId(), stopFtpJob, 1);
		
	}

	private void stopDockerJob(com.cgi.eoss.osiris.rpc.Job rpcJob, Job job) {
		OsirisWorkerGrpc.OsirisWorkerBlockingStub worker =
		        workerFactory.getWorkerById(job.getWorkerId());
		if (worker == null)
		    throw new IllegalStateException(
		            "OSIRIS worker not found for job " + rpcJob.getId());
		LOG.info("Stop requested for job {}", rpcJob.getId());
		worker.stopContainer(rpcJob);
	}

	@Override
	public void cancelJob(CancelJobParams request,
	        StreamObserver<CancelJobResponse> responseObserver) {
	    com.cgi.eoss.osiris.rpc.Job rpcJob = request.getJob();
	    Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
	    Set<Job> subJobs = job.getSubJobs();
	    if (subJobs.size() > 0) {
	        for (Job subJob : subJobs) {
	            if (subJob.getStatus() != Job.Status.CANCELLED)
	            cancelJob(subJob);
	        }
	        //TODO Check if this implies parent is completed
	    } else {
	        if (job.getStatus() != Job.Status.CANCELLED) {
	            cancelJob(job);
	        }
	    }
	    responseObserver.onNext(CancelJobResponse.newBuilder().build());
	    responseObserver.onCompleted();
	
	}

	@Override
    public void relaunchFailedJob(RelaunchFailedJobParams request, StreamObserver<RelaunchFailedJobResponse> responseObserver) {

        com.cgi.eoss.osiris.rpc.Job rpcJob = request.getJob();
        Job job = jobDataService.reload(Long.parseLong(rpcJob.getIntJobId()));
        responseObserver.onNext(RelaunchFailedJobResponse.newBuilder().build());
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("OSIRIS Service Orchestrator").put("userId", String.valueOf(job.getOwner().getId()))
                        .put("serviceId", String.valueOf(job.getConfig().getService().getId())).put("zooId", job.getExtId())) {
            if (job.getConfig().getService().getType() == Type.PARALLEL_PROCESSOR && job.isParent()) {
                Set<Job> failedSubJobs =
                        job.getSubJobs().stream().filter(j -> j.getStatus() == Status.ERROR).collect(Collectors.toSet());
                if (failedSubJobs.size() > 0) {
                    checkCost(job.getOwner(), job.getConfig(), failedSubJobs.size());
                    //TODO: Check that the user can use the geoserver spec
                    for (Job failedSubJob : failedSubJobs) {
                        List<JobParam> failedSubJobInputs = GrpcUtil.mapToParams(failedSubJob.getConfig().getInputs());
                        if (!checkInputs(job.getOwner(), failedSubJobInputs)) {
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                            }
                            throw new ServiceExecutionException("User does not have read access to all requested inputs");
                        }

                        if (!checkAccessToOutputCollection(job.getOwner(), failedSubJobInputs)) {
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.error("User {} does not have read access to all requested output collections",
                                        job.getOwner().getId());
                            }
                            throw new ServiceExecutionException("User does not have read access to all requested output collections");
                        }
                    }
                    int i = 0;
                    for (Job failedSubJob : failedSubJobs) {
                        List<JobParam> failedSubJobInputs = GrpcUtil.mapToParams(failedSubJob.getConfig().getInputs());
                        if (failedSubJob.getStage().equals("Step 1 of 3: Data-Fetch") == false) {
                        	chargeUser(failedSubJob.getOwner(), failedSubJob);
                        }
                        failedSubJob.setStatus(Status.CREATED);
                        failedSubJob.setStartTime(null);
                        failedSubJob.setEndTime(null);
                        failedSubJob.setStage(null);
                        failedSubJob.setWorkerId(null);
                        jobDataService.save(failedSubJob);
                        submitJob(failedSubJob, ModelToGrpcUtils.toRpcJob(failedSubJob), failedSubJobInputs, getJobPriority(i));
                    }
                    i++;
                }
            } else {
                List<JobParam> jobInputs = GrpcUtil.mapToParams(job.getConfig().getInputs());
                checkCost(job.getOwner(), job.getConfig());
                //TODO: Check that the user can use the geoserver spec
                if (!checkInputs(job.getOwner(), jobInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested inputs");
                }

                if (!checkAccessToOutputCollection(job.getOwner(), jobInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested output collections");
                }
                if (job.getStage() != null && !job.getStage().equals("Step 1 of 3: Data-Fetch")) {
                	chargeUser(job.getOwner(), job);
                }
                job.setStatus(Status.CREATED);
                job.setStartTime(null);
                job.setEndTime(null);
                job.setStage(null);
                job.setWorkerId(null);
                jobDataService.save(job);
                submitJob(job, rpcJob, GrpcUtil.mapToParams(job.getConfig().getInputs()), SINGLE_JOB_PRIORITY);
            }
        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }

            LOG.error("Failed to run processor. Notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }
    
    @Override
	public void buildService(BuildServiceParams buildServiceParams,
	        StreamObserver<BuildServiceResponse> responseObserver) {
    	Long serviceId = Long.parseLong(buildServiceParams.getServiceId());
    	OsirisService service = serviceDataService.getById(serviceId);
	    try {
	        OsirisWorkerBlockingStub worker = workerFactory.getOne();
		    responseObserver.onNext(BuildServiceResponse.newBuilder().build());
		    DockerImageConfig dockerImageConfig = DockerImageConfig.newBuilder()
		    .setDockerImage(service.getDockerTag())
		    .setServiceName(service.getName())
		    .build();
		    worker.prepareDockerImage(dockerImageConfig);
	        service.getDockerBuildInfo().setDockerBuildStatus(OsirisServiceDockerBuildInfo.Status.COMPLETED);
	        service.getDockerBuildInfo().setLastBuiltFingerprint(buildServiceParams.getBuildFingerprint());
	        serviceDataService.save(service);
	    }
	    catch(Exception e){
	        service.getDockerBuildInfo().setDockerBuildStatus(OsirisServiceDockerBuildInfo.Status.ERROR);
	        serviceDataService.save(service);
            LOG.error("Failed building service {}", service.getId(), e);
	    }
	    responseObserver.onCompleted();
	}

	private void endJobWithError(Job job) {
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now());
        jobDataService.save(job);
    }

    private int getJobPriority(int messageNumber) {
        if (messageNumber >= 0 && messageNumber < 10) {
            return 6;
        }
        else if (messageNumber >= 10 && messageNumber < 30) {
            return 5;
        }
        else if (messageNumber >= 30 && messageNumber < 70) {
            return 4;
        }
        else if (messageNumber >= 70 && messageNumber < 150) {
            return 3;
        }
        else if (messageNumber >= 150 && messageNumber < 310) {
            return 2;
        }
        return 1;
    }

    private void checkCost(User user, JobConfig jobConfig) {
        int estimatedCost = costingService.estimateJobCost(jobConfig);
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }
    
    private void checkCost(User user, JobConfig config, List<String> newInputs) {
        int singleJobCost = costingService.estimateSingleRunJobCost(config);
        int estimatedCost = newInputs.size() * singleJobCost;
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }
    
    private void checkCost(User user, JobConfig config, int numberOfJobs) {
        int singleJobCost = costingService.estimateSingleRunJobCost(config);
        int estimatedCost = numberOfJobs * singleJobCost;
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }
    
    private boolean checkInputs(User user, List<JobParam> inputsList) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(inputsList);

        Set<URI> inputUris = inputs.entries().stream().filter(e -> this.isValidUri(e.getValue()))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create))
                .collect(toSet());

        return inputUris.stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    private boolean checkInputList(User user, List<String> inputsList) {
        return inputsList.stream().filter(e -> this.isValidUri(e)).map(URI::create).collect(toSet())
                .stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }
        
    private boolean checkAccessToOutputCollection(User user, List<JobParam> rpcInputs) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);
        Map<String, String> collectionSpecs;
        try {
            collectionSpecs = getCollectionSpecs(inputs);
            return collectionSpecs.values().stream()
                    .allMatch(collectionId -> catalogueService.canUserWrite(user, collectionId));
        } catch (IOException e) {
            return false;
        }
    }
    
    private Map<String, String> getCollectionSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String collectionsStr = Iterables.getOnlyElement(inputs.get("collection"), null);
        Map<String, String> collectionSpecs = new HashMap<String, String>();
        if (collectionsStr != null && collectionsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
                collectionSpecs.putAll(mapper.readValue(collectionsStr, mapType));
        }
        return collectionSpecs;
    }

    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }

    private void submitJob(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs, int priority){
		OsirisService service = job.getConfig().getService();
		switch (service.getType()) {
		case APPLICATION:
			submitApplicationJob(job, rpcJob, rpcInputs, priority);
			return;
		case PROCESSOR:
		case PARALLEL_PROCESSOR:
		case BULK_PROCESSOR:
			submitProcessorJob(job, rpcJob, rpcInputs, priority);
			return;
		case FTP_SERVICE:
			submitFtpJob(job, rpcJob, rpcInputs, priority);
			return;
		case WPS_SERVICE:
			submitWpsJob(job, rpcJob, rpcInputs, priority);
			return;
		}
	}

	private void submitWpsJob(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs, int priority) {
		WpsJobSpec.Builder wpsJobSpecBuilder = WpsJobSpec.newBuilder();
		ExecuteWpsParams executeWpsParams = createExecuteWpsParams(job.getConfig().getService(), rpcInputs);
		WpsJobSpec wpsJobSpec = wpsJobSpecBuilder
				.setExecuteWpsParams(executeWpsParams)
				.setJob(rpcJob)
				.build();
		LocalDateTime jobDate = LocalDateTime.now();
		job.setStartTime(jobDate);
		//Temporarily sets the job end time to the start time otherwise output ingestion will fail
		job.setEndTime(jobDate);
		jobDataService.save(job);
		enqueueJobMessage(OsirisQueueService.wpsJobQueueName, job.getId(), wpsJobSpec, priority);
	}

	private ExecuteWpsParams createExecuteWpsParams(OsirisService service, List<JobParam> rpcInputs) {
		ExecuteWpsParams.Builder executeWpsParamsBuilder = ExecuteWpsParams.newBuilder();
		URI serviceUri=service.getExternalServiceUri();
		String wpsServerUrl;
		String processId;
		try {
			wpsServerUrl = removeQueryParameter(serviceUri.toString(), "processId");
			processId = getQueryParameter(serviceUri.toString(), "processId");
		} catch (URISyntaxException e) {
			throw new ServiceExecutionException("Failed to get wps uri from service");
		}
		executeWpsParamsBuilder.setWpsServerUrl(wpsServerUrl);
		executeWpsParamsBuilder.setProcessId(processId);
		executeWpsParamsBuilder.setStoreOutputs(true);
		for (JobParam jobParam: rpcInputs) {
			String parameterId = jobParam.getParamName();
			if(PlatformParameterExtractor.isPlatformParameter(parameterId)){
				continue;
			}
			List<String> paramValues = jobParam.getParamValueList();
			Optional<Parameter> inputParameterOpt = service.getServiceDescriptor().getDataInputs().stream().filter(p -> p.getId().equals(parameterId)).findFirst();
			if (inputParameterOpt.isPresent()){
				Parameter inputParameter = inputParameterOpt.get();
				DataNodeType parameterNodeType = inputParameter.getData();
				if (parameterNodeType.equals(DataNodeType.LITERAL)) {
					executeWpsParamsBuilder.addLiteralDataWpsParam(
							LiteralDataWpsParam.newBuilder()
							.setParamName(parameterId)
							.setParamValue(paramValues.get(0))
							.build()
					);
				}
				else if (parameterNodeType.equals(DataNodeType.COMPLEX)) {
					String mimeType = inputParameter.getDefaultAttrs().get("mimeType");
					executeWpsParamsBuilder.addComplexDataWpsParam(
							ComplexDataWpsParam.newBuilder()
							.setParamName(parameterId)
							.setMimeType(mimeType)
							.setParamValue(paramValues.get(0))
							.build());
				}
				else if (parameterNodeType.equals(DataNodeType.BOUNDING_BOX)) {
					//TODO Handle bounding box
				}
			}
			else {
				throw new ServiceExecutionException("Unrecognized input parameter " + parameterId);
			}
		}
		
		for (Parameter outputParameter: service.getServiceDescriptor().getDataOutputs()) {
			if (outputParameter.getData().equals(DataNodeType.COMPLEX)) {
				String mimeType = outputParameter.getDefaultAttrs().get("mimeType");
				String schema = outputParameter.getDefaultAttrs().get("schema");
				String encoding = outputParameter.getDefaultAttrs().get("encoding");
				ComplexDataWpsOutputDefinition.Builder builder = ComplexDataWpsOutputDefinition.newBuilder()
				.setOutputName(outputParameter.getId());
				if (mimeType != null) {
					builder.setMimeType(mimeType);
				}
				if (schema != null) {
					builder.setSchema(schema);
				}
				if (encoding != null) {
					builder.setEncoding(encoding);
				}
				executeWpsParamsBuilder.addComplexDataWpsOutputDefinition(builder.build());
			}
		}
		
		return executeWpsParamsBuilder.build();
	}

	public String removeQueryParameter(String url, String parameterName) throws URISyntaxException {
	    URIBuilder uriBuilder = new URIBuilder(url);
	    List<NameValuePair> queryParameters = uriBuilder.getQueryParams();
	    for (Iterator<NameValuePair> queryParameterItr = queryParameters.iterator(); queryParameterItr.hasNext();) {
	        NameValuePair queryParameter = queryParameterItr.next();
	        if (queryParameter.getName().equals(parameterName)) {
	            queryParameterItr.remove();
	        }
	    }
	    uriBuilder.setParameters(queryParameters);
	    return uriBuilder.build().toString();
	}
	
	public String getQueryParameter(String url, String parameterName) throws URISyntaxException {
	    URIBuilder uriBuilder = new URIBuilder(url);
	    List<NameValuePair> queryParameters = uriBuilder.getQueryParams();
	    for (Iterator<NameValuePair> queryParameterItr = queryParameters.iterator(); queryParameterItr.hasNext();) {
	        NameValuePair queryParameter = queryParameterItr.next();
	        if (queryParameter.getName().equals(parameterName)) {
	            return queryParameter.getValue();
	        }
	    }
		return null;
	}
	
	private void submitFtpJob(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs, int priority) {
		Builder ftpJobSpecBuilder = FtpJobSpec.newBuilder();
		ftpJobSpecBuilder.setJob(rpcJob);
		Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);
		String ftpRootUri = Iterables.getOnlyElement(inputs.get("ftpRootUri"), null);
		FtpJobSpec ftpJobSpec = ftpJobSpecBuilder.setFtpRootUri(ftpRootUri).build();
		LocalDateTime jobDate = LocalDateTime.now();
		job.setStartTime(jobDate);
		//Temporarily sets the job end time to the start time otherwise output ingestion will fail
		job.setEndTime(jobDate);
		jobDataService.save(job);
		enqueueJobMessage(OsirisQueueService.ftpJobQueueName, job.getId(), ftpJobSpec, priority);
	}

	private void submitApplicationJob(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs,
			int priority) {
		JobSpec.Builder jobSpecBuilder = createJobSpecBuilder(job.getConfig().getService(), rpcJob, rpcInputs);
		jobSpecBuilder.addExposedPorts(OsirisGuiServiceManager.GUACAMOLE_PORT);
		if (dynamicProxyService.supportsProxyRoute()) {
			jobSpecBuilder.putEnvironmentVariables("PLATFORM_REVERSE_PROXY_PREFIX",
					dynamicProxyService.getProxyRoute(rpcJob));
		}
		JobSpec jobSpec = jobSpecBuilder.build();
		enqueueJobMessage(OsirisQueueService.jobQueueName, job.getId(), jobSpec, priority);
	}

	private void submitProcessorJob(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs,
			int priority) {
		JobSpec.Builder jobSpecBuilder = createJobSpecBuilder(
				job.getConfig().getService(), rpcJob, rpcInputs);
		JobSpec jobSpec = jobSpecBuilder.build();
		enqueueJobMessage(OsirisQueueService.jobQueueName, job.getId(), jobSpec, priority);
	}

	private void enqueueJobMessage(String queueName, Long jobId, Object message, int priority) {
		HashMap<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("jobId", jobId);
		queueService.sendObject(queueName, messageHeaders, message, priority);
		LOG.info("Sent message for job {} to queue {}", jobId, queueName);
	}
	
	private JobSpec.Builder createJobSpecBuilder(OsirisService service,
			com.cgi.eoss.osiris.rpc.Job rpcJob, List<JobParam> rpcInputs) {
		JobSpec.Builder jobSpecBuilder = JobSpec.newBuilder().setService(ModelToGrpcUtils.toRpcService(service))
				.setJob(rpcJob).addAllInputs(rpcInputs);
		Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

		if (inputs.containsKey(TIMEOUT_PARAM)) {
			int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
			jobSpecBuilder = jobSpecBuilder.setHasTimeout(true).setTimeoutValue(timeout);
		}
		Map<Long, String> additionalMounts = service.getAdditionalMounts();

		for (Long userMountId : additionalMounts.keySet()) {
			UserMount userMount = userMountDataService.getById(userMountId);
			String targetPath = additionalMounts.get(userMountId);
			String bind = userMount.getMountPath() + ":" + targetPath + ":"
					+ userMount.getType().toString().toLowerCase();
			jobSpecBuilder = jobSpecBuilder.addUserBinds(bind);
		}
		// TODO Add CPU, RAM Management
		// TODO Add per job requests
		if (service.getRequiredResources() != null) {
			OsirisServiceResources requiredResources = service.getRequiredResources();
			jobSpecBuilder = jobSpecBuilder.setResourceRequest(
					ResourceRequest.newBuilder().setStorage(Integer.valueOf(requiredResources.getStorage())));
		}
		return jobSpecBuilder;
	}
	
	private void cancelJob(Job job) {
        LOG.info("Cancelling job with id {}", job.getId());
        JobSpec queuedJobSpec = (JobSpec) queueService
                .receiveSelectedObject(OsirisQueueService.jobQueueName, "jobId = " + job.getId());
        if (queuedJobSpec != null) {
            LOG.info("Refunding user for job id {}", job.getId());
            costingService.refundUser(job.getOwner().getWallet(), job);
            job.setStatus(Status.CANCELLED);
            jobDataService.save(job);
        }
    }
    
    private List<String> explodeParallelInput(Collection<String> inputUris) {
        List<String> results = new ArrayList<String>();
        for (String inputUri : inputUris) {
            if (inputUri.startsWith("osiris://databasket")) {
                Databasket dataBasket = getDatabasketFromUri(inputUri);
                results.addAll(dataBasket.getFiles().stream().map(f -> f.getUri().toString())
                        .collect(toList()));
            } else {
                if (inputUri.contains((","))) {
                    results.addAll(Arrays.asList(inputUri.split(",")));
                } else {
                    results.add(inputUri);
                }
            }
        }
        return results;

    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new ServiceExecutionException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId))
                .orElseThrow(() -> new ServiceExecutionException(
                        "Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

    private List<Job> createSubJobs(Job parentJob, String userId, OsirisService service,
            List<String> newInputs, Multimap<String, String> inputs) {
        List<Job> childJobs = new ArrayList<Job>();
        // Create the simpler map of parameters shared by all parallel jobs
        SetMultimap<String, String> sharedParams =
                MultimapBuilder.hashKeys().hashSetValues().build(inputs);
        sharedParams.removeAll(PARALLEL_INPUT_IDENTIFIER);
        for (String newInput : newInputs) {
            SetMultimap<String, String> parallelJobParams =
                    MultimapBuilder.hashKeys().hashSetValues().build(sharedParams);
            parallelJobParams.put("input", newInput);
            Job childJob =
                    jobDataService.buildNew(UUID.randomUUID().toString(), userId, service.getName(),
                            parentJob.getConfig().getLabel(), parallelJobParams, parentJob);
            childJob = jobDataService.reload(childJob.getId());
            parentJob.getSubJobs().add(childJob);
            childJobs.add(childJob);
        }
        parentJob.setParent(true);
        jobDataService.save(parentJob);
        return childJobs;
    }
    

    private void chargeUser(User user, Job job) {
        costingService.chargeForJob(user.getWallet(), job);
    }
 

}
