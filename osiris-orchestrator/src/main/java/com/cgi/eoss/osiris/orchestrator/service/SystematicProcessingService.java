package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.JobParam;
import com.cgi.eoss.osiris.rpc.RestartSystematicProcessingParams;
import com.cgi.eoss.osiris.rpc.RestartSystematicProcessingResponse;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.rpc.SystematicProcessingResponse;
import com.cgi.eoss.osiris.rpc.SystematicProcessingServiceGrpc;
import com.cgi.eoss.osiris.rpc.TerminateSystematicProcessingParams;
import com.cgi.eoss.osiris.rpc.TerminateSystematicProcessingResponse;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Log4j2
@GRpcService
public class SystematicProcessingService extends SystematicProcessingServiceGrpc.SystematicProcessingServiceImplBase {

    private static final String SYSTEMATIC_PROCESSING_JOB_PREFIX = "trigger-systematic-processing-";
    private static final String SYSTEMATIC_PROCESSING_JOB_GROUP = "osiris";
    private final SystematicProcessingDataService systematicProcessingDataService;
    private final ScheduledJobService scheduledJobService;
    private long searchPeriodMillis;
   
    @Autowired
    public SystematicProcessingService(SystematicProcessingDataService systematicProcessingDataService, ScheduledJobService scheduledJobService, @Value("${osiris.orchestrator.systematic.checkPeriod:3600000}")long searchPeriodMillis, TaskScheduler taskScheduler) {
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.scheduledJobService = scheduledJobService;
        this.searchPeriodMillis = searchPeriodMillis;
        taskScheduler.scheduleAtFixedRate(this::unblockSystematicProcessings, searchPeriodMillis);
        
    }

    @Override
    public void launch(SystematicProcessingRequest request, StreamObserver<SystematicProcessingResponse> responseObserver) {
        ListMultimap<String, String> searchParams = request.getSearchParameterList().stream()
                .collect(Multimaps.flatteningToMultimap(JobParam::getParamName,
                        sp -> sp.getParamValueList().stream(),
                        MultimapBuilder.linkedHashKeys().arrayListValues()::build));
        
        //Put the necessary parameters for systematic processing
        searchParams.put("sortOrder", "ascending");
        searchParams.put("sortParam", "updated");

        Instant dateStart = searchParams.get("productDateStart").stream()
                .map(Instant::parse)
                .findFirst()
                .orElse(Instant.now());
        searchParams.replaceValues("productDateStart", Collections.singleton(dateStart.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)));
        SystematicProcessing systematicProcessing = systematicProcessingDataService.buildNew(UUID.randomUUID().toString(),
                request.getUserId(), request.getServiceId(), request.getJobConfigLabel(), request.getSystematicParameter(),
                GrpcUtil.paramsListToMap(request.getInputList()), searchParams, request.getCronExpression(), LocalDateTime.ofInstant(dateStart, ZoneOffset.UTC));

        LOG.info("Systematic processing {} saved", systematicProcessing.getId());
        scheduleSystematicProcessing(systematicProcessing);
        responseObserver.onNext(SystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }

    private void unblockSystematicProcessings() {
        List<SystematicProcessing> blockedSystematicProcessings = systematicProcessingDataService.findByStatus(Status.BLOCKED);

        //Try to resume blocked systematic processings
        for (SystematicProcessing blockedSystematicProcessing : blockedSystematicProcessings) {
            User user = blockedSystematicProcessing.getOwner();
            if (user.getWallet().getBalance() > 0) {
                unblock(blockedSystematicProcessing);
                LOG.info("Unblocked systematic processing {}", blockedSystematicProcessing.getId());
            }
            else {
                LOG.info("Canmot unblock systematic processing {} - insufficient user credit", blockedSystematicProcessing.getId());
                
            }
        }
    }
    public void block(SystematicProcessing systematicProcessing) {
        scheduledJobService.pauseJob(getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP);
        systematicProcessing.setStatus(Status.BLOCKED);
        systematicProcessingDataService.save(systematicProcessing);
    }
    
    public void unblock(SystematicProcessing systematicProcessing){
        scheduledJobService.resumeJob(getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP);
        systematicProcessing.setStatus(Status.ACTIVE);
        systematicProcessingDataService.save(systematicProcessing);
    }
    
    private void scheduleSystematicProcessing(SystematicProcessing systematicProcessing) {
        if (!Strings.isNullOrEmpty(systematicProcessing.getCronExpression())) {
            scheduleFixedInputsCronJob(systematicProcessing);
        }
        
        else if (systematicProcessing.getSearchParameters() != null) {
            scheduleSearchJob(systematicProcessing);
        }
        else {
            throw new IllegalArgumentException("A systematic processing must have a cron expression or search parameters to be scheduled");
        }
    }

    private void scheduleSearchJob(SystematicProcessing systematicProcessing) {
        Map<String, Object> jobContext = new HashMap<>();
        jobContext.put("systematicProcessingId", String.valueOf(systematicProcessing.getId()));
        scheduledJobService.scheduleJobEveryNSeconds(SearchBasedQuartzJob.class, getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP, jobContext, (int) searchPeriodMillis/1000);
    }

    private String getSystematicProcessingIdentity(SystematicProcessing systematicProcessing) {
        return SYSTEMATIC_PROCESSING_JOB_PREFIX + systematicProcessing.getId();
    }

    private void scheduleFixedInputsCronJob(SystematicProcessing systematicProcessing) {
        Map<String, Object> jobContext = new HashMap<>();
        jobContext.put("systematicProcessingId", String.valueOf(systematicProcessing.getId()));
        scheduledJobService.scheduleCronJob(FixedInputsQuartzJob.class, getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP, jobContext, systematicProcessing.getCronExpression(), false);
    }
    
    @Override
    public void terminate(TerminateSystematicProcessingParams params, StreamObserver<TerminateSystematicProcessingResponse> responseObserver) {
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(params.getSystematicProcessingId());
        scheduledJobService.unscheduleJob(getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP);
        scheduledJobService.deleteJob(getSystematicProcessingIdentity(systematicProcessing), SYSTEMATIC_PROCESSING_JOB_GROUP);
        systematicProcessing.setStatus(Status.COMPLETED);
        systematicProcessingDataService.save(systematicProcessing);
        responseObserver.onNext(TerminateSystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void restart(RestartSystematicProcessingParams params, StreamObserver<RestartSystematicProcessingResponse> responseObserver) {
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(params.getSystematicProcessingId());
        scheduleSystematicProcessing(systematicProcessing);
        systematicProcessing.setStatus(Status.ACTIVE);
        systematicProcessingDataService.save(systematicProcessing);
        responseObserver.onNext(RestartSystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }
}
