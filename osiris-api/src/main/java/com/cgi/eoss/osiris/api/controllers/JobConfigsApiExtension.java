package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.persistence.dao.JobDao;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.JobParam;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.OsirisJobResponse;
import com.cgi.eoss.osiris.rpc.OsirisServiceParams;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.base.Strings;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link JobConfig}s. Offers additional functionality over
 * the standard CRUD-style {@link JobConfigsApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/jobConfigs")
@Log4j2
public class JobConfigsApiExtension {

    private final OsirisSecurityService osirisSecurityService;
    private final LocalServiceLauncher localServiceLauncher;
    private final JobDao jobRepository;

    @Autowired
    public JobConfigsApiExtension(OsirisSecurityService osirisSecurityService, LocalServiceLauncher localServiceLauncher, JobDao jobRepository) {
        this.osirisSecurityService = osirisSecurityService;
        this.localServiceLauncher = localServiceLauncher;
        this.jobRepository = jobRepository;
    }

    /**
     * <p>Provides a direct interface to the service orchestrator, allowing users to launch job configurations without
     * going via WPS.</p>
     * <p>Service are launched asynchronously; the gRPC response is discarded.</p>
     */
    @PostMapping("/{jobConfigId}/launch")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity<Resource<Job>> launch(@ModelAttribute("jobConfigId") JobConfig jobConfig) throws InterruptedException {

        OsirisServiceParams.Builder serviceParamsBuilder = OsirisServiceParams.newBuilder()
                .setJobId(UUID.randomUUID().toString())
                .setUserId(osirisSecurityService.getCurrentUser().getName())
                .setServiceId(jobConfig.getService().getName())
                .addAllInputs(GrpcUtil.mapToParams(jobConfig.getInputs()));

        if (!Strings.isNullOrEmpty(jobConfig.getLabel())) {
            serviceParamsBuilder.setJobConfigLabel(jobConfig.getLabel());
        }
        
        if ((jobConfig.getParent() != null)) {
            serviceParamsBuilder.setJobParent(String.valueOf(jobConfig.getParent().getId()));
        }

        OsirisServiceParams serviceParams = serviceParamsBuilder.build();

        LOG.info("Launching service via REST API: {}", serviceParams);

        final CountDownLatch latch = new CountDownLatch(1);
        JobLaunchObserver responseObserver = new JobLaunchObserver(latch);
        localServiceLauncher.asyncSubmitJob(serviceParams, responseObserver);

        // Block until the latch counts down (i.e. one message from the server
        latch.await(1, TimeUnit.MINUTES);
        Job job = jobRepository.getOne(responseObserver.getIntJobId());
        return ResponseEntity.accepted().body(new Resource<>(job));
    }

    
    /**
     * <p>Launches a systematic processing job asynchronously, in a form similar to {@link #launch(JobConfig)}.</p>
     */
    @PostMapping("/launchSystematic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfigTemplate.id == null) or hasPermission(#jobConfigTemplate, 'read')")
    public ResponseEntity<Void> launchSystematic(HttpServletRequest request, @RequestBody JobConfig jobConfigTemplate) throws InterruptedException {

        SystematicProcessingRequest.Builder grpcRequestBuilder = SystematicProcessingRequest.newBuilder()
                .setUserId(osirisSecurityService.getCurrentUser().getName())
                .setServiceId(jobConfigTemplate.getService().getName())
                .addAllInput(GrpcUtil.mapToParams(jobConfigTemplate.getInputs()))
                .setSystematicParameter(jobConfigTemplate.getSystematicParameter());

        Map<String, String[]> parameterMap = request.getParameterMap();
        for (String key : parameterMap.keySet()) {
            grpcRequestBuilder.addSearchParameter(JobParam.newBuilder()
                    .setParamName(key)
                    .addAllParamValue(Arrays.asList(parameterMap.get(key)))
                    .build());
        }

        if (!Strings.isNullOrEmpty(jobConfigTemplate.getLabel())) {
            grpcRequestBuilder.setJobConfigLabel(jobConfigTemplate.getLabel());
        }

        localServiceLauncher.launchSystematicProcessing(grpcRequestBuilder.build());
        
        return ResponseEntity.accepted().build();
    }
    
    private static final class JobLaunchObserver implements StreamObserver<OsirisJobResponse> {

        private final CountDownLatch latch;
        @Getter
        private long intJobId;

        JobLaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(OsirisJobResponse value) {
            this.intJobId = Long.parseLong(value.getJob().getIntJobId());
            LOG.info("Received job ID: {}", this.intJobId);
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to launch service via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }

}
