package com.cgi.eoss.osiris.rpc;


import com.cgi.eoss.osiris.rpc.worker.CleanUpResponse;
import com.cgi.eoss.osiris.rpc.worker.ContainerExitCode;
import com.cgi.eoss.osiris.rpc.worker.ExitParams;
import com.cgi.eoss.osiris.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.JobDockerConfig;
import com.cgi.eoss.osiris.rpc.worker.JobEnvironment;
import com.cgi.eoss.osiris.rpc.worker.JobInputs;
import com.cgi.eoss.osiris.rpc.worker.LaunchContainerResponse;
import io.grpc.ManagedChannelBuilder;

public class LocalWorker {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    public JobEnvironment prepareInputs(JobInputs request) {
        OsirisWorkerGrpc.OsirisWorkerBlockingStub worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobDockerConfig request) {
        OsirisWorkerGrpc.OsirisWorkerBlockingStub worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.launchContainer(request);
    }

    public ContainerExitCode waitForContainerExitWithTimeout(ExitWithTimeoutParams request) {
        OsirisWorkerGrpc.OsirisWorkerBlockingStub worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExitWithTimeout(request);
   }

    public ContainerExitCode waitForContainerExit(ExitParams request) {
        OsirisWorkerGrpc.OsirisWorkerBlockingStub worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExit(request);
    }
    
    public CleanUpResponse cleanUp(Job request) {
        OsirisWorkerGrpc.OsirisWorkerBlockingStub worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.cleanUp(request);
    }

   
}
