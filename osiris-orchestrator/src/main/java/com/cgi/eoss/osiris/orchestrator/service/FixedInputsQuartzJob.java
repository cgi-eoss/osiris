package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.orchestrator.service.ServiceLauncherClient.JobSubmissionException;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.log4j.Log4j2;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class FixedInputsQuartzJob extends QuartzJobBean{
    
    @Autowired
    private SystematicProcessingService systematicProcessingService;
    @Autowired
    private SystematicProcessingDataService systematicProcessingDataService;
    @Autowired
    private ServiceLauncherClient serviceLauncherClient;
    @Autowired
    private CostingService costingService;
    
   
    
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        long systematicProcessingId = Long.parseLong(jobDataMap.getString("systematicProcessingId"));
        SystematicProcessing systematicProcessing = systematicProcessingDataService.getById(systematicProcessingId);
        LOG.info("Updating systematic processing {}", systematicProcessing.getId());
        
        JobConfig configTemplate = systematicProcessing.getParentJob().getConfig();
        int jobCost = costingService.estimateSingleRunJobCost(configTemplate);
        if (jobCost > systematicProcessing.getOwner().getWallet().getBalance()) {
            try {
                systematicProcessingService.block(systematicProcessing);
            } catch (SchedulerException e) {
                LOG.error("Error blocking systematic processing", e);
            }
            
            return;
        }
        Multimap<String, String> inputs = ArrayListMultimap.create();
        inputs.putAll(configTemplate.getInputs());
        try {
            serviceLauncherClient.submitJob(configTemplate.getOwner().getName(), configTemplate.getService().getName(), String.valueOf(systematicProcessing.getParentJob().getId()), inputs);
        } catch (InterruptedException | JobSubmissionException e) {
            LOG.error("Error submitting job", e);
        }
    }
}
