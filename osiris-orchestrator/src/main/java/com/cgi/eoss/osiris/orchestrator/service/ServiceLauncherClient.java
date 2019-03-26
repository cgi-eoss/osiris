package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.OsirisJobResponse;
import com.cgi.eoss.osiris.rpc.OsirisServiceParams;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class ServiceLauncherClient{

    private LocalServiceLauncher localServiceLauncher;

    @Autowired
    public ServiceLauncherClient(LocalServiceLauncher localServiceLauncher) {
        this.localServiceLauncher = localServiceLauncher;
    }
    
    public void submitJob(String userName, String serviceName, String parentId, Multimap<String, String> inputs) throws InterruptedException, JobSubmissionException {
        OsirisServiceParams.Builder serviceParamsBuilder =
                OsirisServiceParams.newBuilder().setJobId(UUID.randomUUID().toString()).setUserId(userName)
                        .setJobParent(parentId)
                        .setServiceId(serviceName).addAllInputs(GrpcUtil.mapToParams(inputs));

        final CountDownLatch latch = new CountDownLatch(1);
        JobLaunchObserver responseObserver = new JobLaunchObserver(latch);
        localServiceLauncher.asyncSubmitJob(serviceParamsBuilder.build(), responseObserver);
        // Block until the latch counts down (i.e. one message from the server)
        latch.await(1, TimeUnit.MINUTES);
        if (responseObserver.getError() != null) {
            throw new JobSubmissionException(responseObserver.getError());
        } 
    }
    
    private static final class JobLaunchObserver implements StreamObserver<OsirisJobResponse> {

        private final CountDownLatch latch;
        @Getter
        private long intJobId;

        @Getter
        private Throwable error;

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
            error = t;
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
    
    public class JobSubmissionException extends Exception {

        JobSubmissionException(Throwable t) {
            super(t);
        }

    }

}
