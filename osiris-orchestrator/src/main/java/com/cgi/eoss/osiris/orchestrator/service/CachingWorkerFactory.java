package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.WorkerLocatorExpression;
import com.cgi.eoss.osiris.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.osiris.rpc.Worker;
import com.cgi.eoss.osiris.rpc.WorkersList;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc.OsirisWorkerBlockingStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.expression.ExpressionParser;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Service providing access to OSIRIS Worker nodes based on environment requests.</p>
 */
@Log4j2
public class CachingWorkerFactory {

    private final DiscoveryClient discoveryClient;
    private final String workerServiceId;
    private final ExpressionParser expressionParser;
    private final WorkerLocatorExpressionDataService workerLocatorExpressionDataService;
    private final WorkerLocatorExpression defaultWorkerLocatorExpression;
    private Map<String, OsirisWorkerBlockingStub> workerStubCache = new ConcurrentHashMap<String, OsirisWorkerBlockingStub>();
    
    public CachingWorkerFactory(DiscoveryClient discoveryClient, String workerServiceId, ExpressionParser expressionParser, WorkerLocatorExpressionDataService workerLocatorExpressionDataService, String defaultWorkerExpression) {
        this.discoveryClient = discoveryClient;
        this.workerServiceId = workerServiceId;
        this.expressionParser = expressionParser;
        this.workerLocatorExpressionDataService = workerLocatorExpressionDataService;
        this.defaultWorkerLocatorExpression = WorkerLocatorExpression.builder().expression(defaultWorkerExpression).build();
    }

    /**
     * @return A Worker appropriate for the requested environment.
     */
    public OsirisWorkerGrpc.OsirisWorkerBlockingStub getWorker(JobConfig jobConfig) {
        WorkerLocatorExpression expression = getWorkerLocatorExpression(jobConfig);
        LOG.debug("Locating worker for jobConfig {} by expression: {}", jobConfig.getId(), expression);
        String env = expressionParser.parseExpression(expression.getExpression()).getValue(jobConfig).toString();

        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerEnv").equals(env))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find registered worker for environment: " + env));

        LOG.info("Located {} worker: {}:{}", env, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return OsirisWorkerGrpc.newBlockingStub(managedChannel);
    }
    
    
    public OsirisWorkerBlockingStub getWorkerById(String workerId) {
        OsirisWorkerBlockingStub existingWorkerStub = workerStubCache.get(workerId);
        if (existingWorkerStub != null) {
            return existingWorkerStub;
        }
        else {
            OsirisWorkerBlockingStub newWorkerStub = createStubForWorker(workerId);
            workerStubCache.put(workerId, newWorkerStub);
            return newWorkerStub;
        }
    }
    
    /**
     * @return The worker with the specified id
     */
    private OsirisWorkerGrpc.OsirisWorkerBlockingStub createStubForWorker(String workerId) {
        LOG.debug("Locating worker with id {}", workerId);
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .filter(si -> si.getMetadata().get("workerId").equals(workerId))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find worker with id: " + workerId));

        LOG.info("Located {} worker: {}:{}", workerId, worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return OsirisWorkerGrpc.newBlockingStub(managedChannel);
    }
    
    /**
     * @return An existing instance of a worker
     */
    public OsirisWorkerGrpc.OsirisWorkerBlockingStub getOne() {
        ServiceInstance worker = discoveryClient.getInstances(workerServiceId).stream()
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unable to find a worker"));

        LOG.info("Located worker: {}:{}", worker.getHost(), worker.getMetadata().get("grpcPort"));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(worker.getHost(), Integer.parseInt(worker.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return OsirisWorkerGrpc.newBlockingStub(managedChannel);
    }

    public WorkersList listWorkers() {
        WorkersList.Builder result = WorkersList.newBuilder();

        discoveryClient.getInstances(workerServiceId).stream()
                .map(si -> Worker.newBuilder()
                        .setHost(si.getHost())
                        .setPort(Integer.parseInt(si.getMetadata().get("grpcPort")))
                        .setEnvironment(si.getMetadata().get("workerEnv"))
                        .build())
                .forEach(result::addWorkers);

        return result.build();
    }

    private WorkerLocatorExpression getWorkerLocatorExpression(JobConfig jobConfig) {
        return Optional.ofNullable(workerLocatorExpressionDataService.getByService(jobConfig.getService())).orElse(defaultWorkerLocatorExpression);
    }

}
