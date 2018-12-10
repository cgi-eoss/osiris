package com.cgi.eoss.osiris.costing;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.Wallet;

/**
 * <p>Service to expose OSIRIS activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    Integer estimateJobCost(JobConfig jobConfig);
    
    Integer estimateSingleRunJobCost(JobConfig jobConfig);
    
    Integer estimateDownloadCost(OsirisFile download);

    void chargeForJob(Wallet wallet, Job job);

    void chargeForDownload(Wallet wallet, OsirisFile download);

    void refundUser(Wallet wallet, Job job);

}
