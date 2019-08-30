package com.cgi.eoss.osiris.rpc;


import com.cgi.eoss.osiris.rpc.worker.CleanUpResponse;
import com.cgi.eoss.osiris.rpc.worker.ContainerExitCode;
import com.cgi.eoss.osiris.rpc.worker.ExitParams;
import com.cgi.eoss.osiris.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc.OsirisWorkerBlockingStub;
import com.cgi.eoss.osiris.rpc.worker.JobDockerConfig;
import com.cgi.eoss.osiris.rpc.worker.JobEnvironment;
import com.cgi.eoss.osiris.rpc.worker.JobInputs;
import com.cgi.eoss.osiris.rpc.worker.LaunchContainerResponse;
import io.grpc.ManagedChannelBuilder;

public class LocalWorker {
	
	OsirisWorkerBlockingStub worker;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
    	worker = OsirisWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
    }
    
    public JobEnvironment prepareInputs(JobInputs request) {
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobDockerConfig request) {
        return worker.launchContainer(request);
    }

    public ContainerExitCode waitForContainerExitWithTimeout(ExitWithTimeoutParams request) {
        return worker.waitForContainerExitWithTimeout(request);
   }

    public ContainerExitCode waitForContainerExit(ExitParams request) {
        return worker.waitForContainerExit(request);
    }
    
    public CleanUpResponse cleanUp(Job request) {
        return worker.cleanUp(request);
    }

   
}
