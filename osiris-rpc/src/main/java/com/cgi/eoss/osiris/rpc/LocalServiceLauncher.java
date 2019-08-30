package com.cgi.eoss.osiris.rpc;


import org.springframework.scheduling.annotation.Async;
import com.cgi.eoss.osiris.rpc.OsirisJobLauncherGrpc.OsirisJobLauncherStub;
import com.cgi.eoss.osiris.rpc.OsirisServiceLauncherGrpc.OsirisServiceLauncherStub;
import com.cgi.eoss.osiris.rpc.SystematicProcessingServiceGrpc.SystematicProcessingServiceBlockingStub;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalServiceLauncher {
    private OsirisJobLauncherStub jobLauncher;
    private OsirisServiceLauncherStub serviceLauncher;
	private SystematicProcessingServiceBlockingStub blockingSystematicProcessingService;
    
    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher = OsirisServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        blockingSystematicProcessingService = SystematicProcessingServiceGrpc.newBlockingStub(inProcessChannelBuilder.build());
        
    }
    
    @Async
    public void asyncSubmitJob(OsirisServiceParams serviceParams, StreamObserver<OsirisJobResponse> responseObserver) {
        jobLauncher.submitJob(serviceParams, responseObserver);
    }

    @Async
    public void asyncLaunchService(OsirisServiceParams serviceParams, StreamObserver<OsirisServiceResponse> responseObserver) {
        serviceLauncher.launchService(serviceParams, responseObserver);
    }
    
    @Async
    public void asyncStopService(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        serviceLauncher.stopService(stopServiceParams, responseObserver);
    }
    
    @Async
    public void asyncCancelJob(CancelJobParams cancelJobParams, StreamObserver<CancelJobResponse> responseObserver) {
        jobLauncher.cancelJob(cancelJobParams, responseObserver);
    }
    
    @Async
    public void asyncStopJob(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        jobLauncher.stopJob(stopServiceParams, responseObserver);
    }

    @Async
    public void asyncRelaunchFailedJob(RelaunchFailedJobParams relaunchJobParams, StreamObserver<RelaunchFailedJobResponse> responseObserver) {
        jobLauncher.relaunchFailedJob(relaunchJobParams, responseObserver);
    }
    
    @Async
    public void asyncBuildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        jobLauncher.buildService(buildServiceParams, responseObserver);
    }

    public SystematicProcessingResponse launchSystematicProcessing(SystematicProcessingRequest request) {
        return blockingSystematicProcessingService.launch(request);
    }
    
    public TerminateSystematicProcessingResponse terminateSystematicProcessing(TerminateSystematicProcessingParams request) {
        return blockingSystematicProcessingService.terminate(request);
    }
    
    public RestartSystematicProcessingResponse restartSystematicProcessing(RestartSystematicProcessingParams request) {
        return blockingSystematicProcessingService.restart(request);
    }
}
