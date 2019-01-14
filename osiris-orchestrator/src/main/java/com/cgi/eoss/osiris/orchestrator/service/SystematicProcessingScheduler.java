package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.model.SystematicProcessing.Status;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.OsirisJobResponse;
import com.cgi.eoss.osiris.rpc.OsirisServiceParams;
import com.cgi.eoss.osiris.search.api.SearchFacade;
import com.cgi.eoss.osiris.search.api.SearchParameters;
import com.cgi.eoss.osiris.search.api.SearchResults;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Service to schedule systematic processing activities
 * </p>
 */
@Log4j2
@Service
@ConditionalOnProperty(name="osiris.orchestrator.systematic.enabled", havingValue="true")
public class SystematicProcessingScheduler {

    private final SearchFacade searchFacade;
    private final SystematicProcessingDataService systematicProcessingDataService;
    private final LocalServiceLauncher localServiceLauncher;
    private final CostingService costingService;

    @Autowired
    public SystematicProcessingScheduler(SystematicProcessingDataService systematicProcessingDataService,
                                         SearchFacade searchFacade,
                                         LocalServiceLauncher localServiceLauncher,
                                         CostingService costingService,
                                         TaskScheduler taskScheduler,
                                         @Value("${osiris.orchestrator.systematic.checkPeriod:3600000}") long checkPeriod) {
        this.systematicProcessingDataService = systematicProcessingDataService;
        this.searchFacade = searchFacade;
        this.localServiceLauncher = localServiceLauncher;
        this.costingService = costingService;
        LOG.info("Scheduling systematic processing updates every {}ms", checkPeriod);
        taskScheduler.scheduleAtFixedRate(this::updateSystematicProcessings, checkPeriod);
    }

    private void updateSystematicProcessings() {
        List<SystematicProcessing> activeSystematicProcessings = systematicProcessingDataService.findByStatus(Status.ACTIVE);
        List<SystematicProcessing> blockedSystematicProcessings = systematicProcessingDataService.findByStatus(Status.BLOCKED);

        LOG.info("Updating {} active and {} blocked systematic processing activities",
                activeSystematicProcessings.size(), blockedSystematicProcessings.size());
        
        for (SystematicProcessing activeSystematicProcessing : activeSystematicProcessings) {
            updateSystematicProcessing(activeSystematicProcessing);
        }
        
        //Try to resume blocked systematic processings
        for (SystematicProcessing blockedSystematicProcessing : blockedSystematicProcessings) {
            User user = blockedSystematicProcessing.getOwner();
            if (user.getWallet().getBalance() > 0) {
                blockedSystematicProcessing.setStatus(Status.ACTIVE);
                systematicProcessingDataService.save(blockedSystematicProcessing);
                updateSystematicProcessing(blockedSystematicProcessing);
            }
        }
    }

    private void updateSystematicProcessing(SystematicProcessing systematicProcessing) {
        LOG.debug("Updating systematic processing {}", systematicProcessing.getId());

        ListMultimap<String, String> queryParameters = systematicProcessing.getSearchParameters();
        queryParameters.replaceValues("publishedAfter",
                Collections.singletonList(ZonedDateTime.of(systematicProcessing.getLastUpdated(), ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        queryParameters.replaceValues("publishedBefore",
                Collections.singletonList(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        queryParameters.replaceValues("sortOrder", Collections.singletonList("ascending"));
        queryParameters.replaceValues("sortParam", Collections.singletonList("published"));
        int page = 0;
        HttpUrl requestUrl = new HttpUrl.Builder().scheme("http").host("local").build();
        SearchResults results;
        JobConfig configTemplate = systematicProcessing.getParentJob().getConfig();
        try {
            do {
                results = getSearchResultsPage(requestUrl, page, queryParameters);
                for (Feature feature : results.getFeatures()) {
                    String url = feature.getProperties().get("osirisUrl").toString();
                    Multimap<String, String> inputs = ArrayListMultimap.create();
                    inputs.putAll(configTemplate.getInputs());
                    inputs.replaceValues(configTemplate.getSystematicParameter(), Collections.singletonList(url));
                    configTemplate.getInputs().put(configTemplate.getSystematicParameter(), url);
                    int jobCost = costingService.estimateSingleRunJobCost(configTemplate);
                    if (jobCost > systematicProcessing.getOwner().getWallet().getBalance()) {
                        systematicProcessing.setStatus(Status.BLOCKED);
                        systematicProcessingDataService.save(systematicProcessing);
                        return;
                    }
                    submitJob(configTemplate.getOwner().getName(), configTemplate.getService().getName(), String.valueOf(systematicProcessing.getParentJob().getId()), inputs);
                    Map<String, Object> extraParams = (Map<String, Object>) feature.getProperties().get("extraParams");
                    systematicProcessing
                            .setLastUpdated(ZonedDateTime.parse(extraParams.get("osirisUpdated").toString()).toLocalDateTime().plusSeconds(1));
                }
                page++;
            } while (results.getLinks().containsKey("next"));
        } catch (IOException e) {
            LOG.error("Failure running search for systematic processing {}", systematicProcessing.getId());
        } catch (InterruptedException e) {
            LOG.error("Failure submitting job for systematic processing {}", systematicProcessing.getId());
        } catch (JobSubmissionException e) {
            LOG.error("Failure submitting job for systematic processing {} ", systematicProcessing.getId());
            systematicProcessing.setStatus(Status.BLOCKED);
        } finally {
            systematicProcessingDataService.save(systematicProcessing);
        }
    }

    private SearchResults getSearchResultsPage(HttpUrl requestUrl, int page, ListMultimap<String, String> queryParameters)
            throws IOException {
        SearchParameters sp = new SearchParameters();
        sp.setPage(page);
        sp.setResultsPerPage(20);
        sp.setRequestUrl(requestUrl);
        sp.setParameters(queryParameters);
        return searchFacade.search(sp);

    }

    private void submitJob(String userName, String serviceName, String parentId, Multimap<String, String> inputs) throws InterruptedException, JobSubmissionException {
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
