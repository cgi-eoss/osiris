package com.cgi.eoss.osiris.rpc;


import org.springframework.scheduling.annotation.Async;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalServiceLauncher {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    @Async
    public void asyncSubmitJob(OsirisServiceParams serviceParams, StreamObserver<OsirisJobResponse> responseObserver) {
        OsirisJobLauncherGrpc.OsirisJobLauncherStub jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.submitJob(serviceParams, responseObserver);
    }

    @Async
    public void asyncLaunchService(OsirisServiceParams serviceParams, StreamObserver<OsirisServiceResponse> responseObserver) {
        OsirisServiceLauncherGrpc.OsirisServiceLauncherStub serviceLauncher = OsirisServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.launchService(serviceParams, responseObserver);
    }
    
    @Async
    public void asyncStopService(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        OsirisServiceLauncherGrpc.OsirisServiceLauncherStub serviceLauncher = OsirisServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.stopService(stopServiceParams, responseObserver);
    }
    
    @Async
    public void asyncCancelJob(CancelJobParams cancelJobParams, StreamObserver<CancelJobResponse> responseObserver) {
        OsirisJobLauncherGrpc.OsirisJobLauncherStub jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.cancelJob(cancelJobParams, responseObserver);
    }
    
    @Async
    public void asyncStopJob(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        OsirisJobLauncherGrpc.OsirisJobLauncherStub jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.stopJob(stopServiceParams, responseObserver);
    }

    @Async
    public void asyncRelaunchFailedJob(RelaunchFailedJobParams relaunchJobParams, StreamObserver<RelaunchFailedJobResponse> responseObserver) {
        OsirisJobLauncherGrpc.OsirisJobLauncherStub jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.relaunchFailedJob(relaunchJobParams, responseObserver);
    }
    
    @Async
    public void asyncBuildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        OsirisJobLauncherGrpc.OsirisJobLauncherStub jobLauncher = OsirisJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.buildService(buildServiceParams, responseObserver);
    }

    public SystematicProcessingResponse launchSystematicProcessing(SystematicProcessingRequest request) {
        SystematicProcessingServiceGrpc.SystematicProcessingServiceBlockingStub blockingStub = SystematicProcessingServiceGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return blockingStub.launch(request);
    }
    
    public TerminateSystematicProcessingResponse terminateSystematicProcessing(TerminateSystematicProcessingParams request) {
        SystematicProcessingServiceGrpc.SystematicProcessingServiceBlockingStub blockingStub = SystematicProcessingServiceGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return blockingStub.terminate(request);
    }
    
    public RestartSystematicProcessingResponse restartSystematicProcessing(RestartSystematicProcessingParams request) {
        SystematicProcessingServiceGrpc.SystematicProcessingServiceBlockingStub blockingStub = SystematicProcessingServiceGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return blockingStub.restart(request);
    }
}
