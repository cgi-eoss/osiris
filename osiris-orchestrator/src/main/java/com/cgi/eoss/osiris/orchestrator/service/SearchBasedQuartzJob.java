package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.orchestrator.service.ServiceLauncherClient.JobSubmissionException;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.search.api.SearchFacade;
import com.cgi.eoss.osiris.search.api.SearchParameters;
import com.cgi.eoss.osiris.search.api.SearchResults;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.geojson.Feature;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

@Component
@Log4j2
public class SearchBasedQuartzJob extends QuartzJobBean{

    @Autowired
    private SystematicProcessingService systematicProcessingService;
    
    @Autowired
    private SystematicProcessingDataService systematicProcessingDataService;
    
    @Autowired
    private SearchFacade searchFacade;
    
    @Autowired
    private ServiceLauncherClient serviceLauncherClient;
    
    @Autowired
    private CostingService costingService;
    
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        long systematicProcessingId = Long.parseLong(jobDataMap.getString("systematicProcessingId"));
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(systematicProcessingId);
        updateSystematicProcessing(systematicProcessing);
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
                        systematicProcessingService.block(systematicProcessing);
                        return;
                    }
                    serviceLauncherClient.submitJob(configTemplate.getOwner().getName(), configTemplate.getService().getName(), String.valueOf(systematicProcessing.getParentJob().getId()), inputs);
                    Map<String, Object> extraParams = (Map<String, Object>) feature.getProperties().get("extraParams");
                    systematicProcessing
                            .setLastUpdated(ZonedDateTime.parse(extraParams.get("osirisUpdated").toString()).toLocalDateTime().plusSeconds(1));
                    systematicProcessingDataService.save(systematicProcessing);
                }
                page++;
            } while (results.getLinks().containsKey("next"));
        } catch (IOException e) {
            LOG.error("Failure running search for systematic processing {}", systematicProcessing.getId());
        } catch (InterruptedException e) {
            LOG.error("Failure submitting job for systematic processing {}", systematicProcessing.getId());
        } catch (JobSubmissionException e) {
            LOG.error("Failure submitting job for systematic processing {} ", systematicProcessing.getId());
        } catch (SchedulerException e) {
            LOG.error("Failure blocking systematic processing {} ", systematicProcessing.getId());
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
}
