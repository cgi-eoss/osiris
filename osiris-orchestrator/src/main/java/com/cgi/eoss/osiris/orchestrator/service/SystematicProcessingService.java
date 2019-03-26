package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
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
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@GRpcService
public class SystematicProcessingService extends SystematicProcessingServiceGrpc.SystematicProcessingServiceImplBase {

    private static final String TRIGGER_SYSTEMATIC_PROCESSING_PREFIX = "trigger-systematic-processing-";
    private final SystematicProcessingDataService systematicProcessingDataService;
    private final Scheduler scheduler;
    private long searchPeriodMillis;
   
    @Autowired
    public SystematicProcessingService(SystematicProcessingDataService systematicProcessingDataService, Scheduler scheduler, @Value("${osiris.orchestrator.systematic.checkPeriod:3600000}")long searchPeriodMillis, TaskScheduler taskScheduler) {
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.scheduler = scheduler;
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
        try {
            scheduleSystematicProcessing(systematicProcessing);
        }
        catch (SchedulerException e) {
            LOG.error(e);
            responseObserver.onError(e);
        }
        responseObserver.onNext(SystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }

    private void unblockSystematicProcessings() {
        List<SystematicProcessing> blockedSystematicProcessings = systematicProcessingDataService.findByStatus(Status.BLOCKED);

        //Try to resume blocked systematic processings
        LOG.info("Trying to unblock systematic processing activities", blockedSystematicProcessings.size());
        for (SystematicProcessing blockedSystematicProcessing : blockedSystematicProcessings) {
            User user = blockedSystematicProcessing.getOwner();
            if (user.getWallet().getBalance() > 0) {
                try {
                    unblock(blockedSystematicProcessing);
                    LOG.info("Unblocked systematic processing {}", blockedSystematicProcessing.getId());
                } catch (SchedulerException e) {
                    LOG.error("Could not unblock systematic processing " +  blockedSystematicProcessing.getId(), e);
                }
            }
        }
    }
    public void block(SystematicProcessing systematicProcessing) throws SchedulerException {
        scheduler.pauseTrigger(TriggerKey.triggerKey(TRIGGER_SYSTEMATIC_PROCESSING_PREFIX + systematicProcessing.getId(), "osiris"));
        systematicProcessing.setStatus(Status.BLOCKED);
        systematicProcessingDataService.save(systematicProcessing);
    }
    
    public void unblock(SystematicProcessing systematicProcessing) throws SchedulerException {
        scheduler.resumeTrigger(TriggerKey.triggerKey(TRIGGER_SYSTEMATIC_PROCESSING_PREFIX + systematicProcessing.getId(), "osiris"));
        systematicProcessing.setStatus(Status.ACTIVE);
        systematicProcessingDataService.save(systematicProcessing);
    }
    
    private void scheduleSystematicProcessing(SystematicProcessing systematicProcessing) throws SchedulerException{
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

    private void scheduleSearchJob(SystematicProcessing systematicProcessing) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("systematicProcessingId", String.valueOf(systematicProcessing.getId()));
        JobDetail jobDetail = JobBuilder.newJob(SearchBasedQuartzJob.class).withIdentity(UUID.randomUUID().toString(), "osiris-jobs")
                        .storeDurably().withDescription("Run Osiris Job").usingJobData(jobDataMap).storeDurably().build();
        
        
        SimpleTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_SYSTEMATIC_PROCESSING_PREFIX+ systematicProcessing.getId(), "osiris")
                        .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever((int) searchPeriodMillis/1000).withMisfireHandlingInstructionIgnoreMisfires()).build();
        scheduler.scheduleJob(jobDetail, cronTrigger);
    }

    private void scheduleFixedInputsCronJob(SystematicProcessing systematicProcessing) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("systematicProcessingId", String.valueOf(systematicProcessing.getId()));
        JobDetail jobDetail = JobBuilder.newJob(FixedInputsQuartzJob.class).withIdentity(UUID.randomUUID().toString(), "osiris-jobs")
                        .storeDurably().withDescription("Run Osiris Job").usingJobData(jobDataMap).storeDurably().build();
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_SYSTEMATIC_PROCESSING_PREFIX + systematicProcessing.getId(), "osiris")
                        .withSchedule(CronScheduleBuilder.cronSchedule(systematicProcessing.getCronExpression()).withMisfireHandlingInstructionIgnoreMisfires()).build();
        scheduler.scheduleJob(jobDetail, cronTrigger);
    }
    
    @Override
    public void terminate(TerminateSystematicProcessingParams params, StreamObserver<TerminateSystematicProcessingResponse> responseObserver) {
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(params.getSystematicProcessingId());
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(TRIGGER_SYSTEMATIC_PROCESSING_PREFIX + systematicProcessing.getId(), "osiris"));
        } catch (SchedulerException e) {
            responseObserver.onError(e);
        }
        systematicProcessing.setStatus(Status.COMPLETED);
        systematicProcessingDataService.save(systematicProcessing);
        responseObserver.onNext(TerminateSystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void restart(RestartSystematicProcessingParams params, StreamObserver<RestartSystematicProcessingResponse> responseObserver) {
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(params.getSystematicProcessingId());
        try {
            scheduleSystematicProcessing(systematicProcessing);
        }
        catch (SchedulerException e) {
            responseObserver.onError(e);
        }
        systematicProcessing.setStatus(Status.ACTIVE);
        systematicProcessingDataService.save(systematicProcessing);
        responseObserver.onNext(RestartSystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }
}
