package com.cgi.eoss.osiris.orchestrator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;

/**
 * <p>Service for more specific interaction with graphical application-type OSIRIS services.</p>
 */
@Component
public class OsirisGuiServiceManager {

    static final String GUACAMOLE_PORT = "8080/tcp";

    @Autowired
    public OsirisGuiServiceManager() {
    }

    /**
     * @return The string representation of a URL suitable for accessing the GUI application represented by the given
     * Job running on the given worker.
     */
    public PortBinding getGuiPortBinding(OsirisWorkerGrpc.OsirisWorkerBlockingStub worker, Job rpcJob) {
        return worker.getPortBindings(rpcJob).getBindingsList().stream()
                .filter(b -> b.getPortDef().equals(GUACAMOLE_PORT))
                .findFirst()
                .orElseThrow(() -> new ServiceExecutionException("Could not find GUI port on docker container for job: " + rpcJob.getId()));
    }

}
