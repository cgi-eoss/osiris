package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.rpc.OsirisServiceLauncherGrpc;
import com.cgi.eoss.osiris.rpc.ListWorkersParams;
import com.cgi.eoss.osiris.rpc.Worker;
import com.cgi.eoss.osiris.rpc.WorkersList;
import io.grpc.ManagedChannelBuilder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * <p>Functionality for admins to examine OSIRIS worker metrics.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/workers")
@Log4j2
public class WorkersApi {

    private final ManagedChannelBuilder inProcessChannelBuilder;

    @Autowired
    public WorkersApi(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity getWorkers() {
        OsirisServiceLauncherGrpc.OsirisServiceLauncherBlockingStub serviceLauncher = OsirisServiceLauncherGrpc.newBlockingStub(inProcessChannelBuilder.build());

        WorkersList workersList = serviceLauncher.listWorkers(ListWorkersParams.getDefaultInstance());

        return ResponseEntity.ok(workersList.getWorkersList().stream().map(WorkerResponse::new).collect(Collectors.toList()));
    }

    @Data
    private static final class WorkerResponse {
        private final String host;
        private final Integer port;
        private final String environment;

        WorkerResponse(Worker worker) {
            this.host = worker.getHost();
            this.port = worker.getPort();
            this.environment = worker.getEnvironment();
        }
    }

}
