package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.worker.ContainerExit;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.rpc.worker.JobEvent;
import com.cgi.eoss.osiris.rpc.worker.JobEventType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

@Component
@Log4j2
public class OsirisJobUpdatesDispatcher {

    private final JobDataService jobDataService;
    private OsirisJobUpdatesManager osirisJobUpdatesManager;


    @Autowired
    public OsirisJobUpdatesDispatcher(JobDataService jobDataService, OsirisJobUpdatesManager osirisJobUpdatesManager) {
        this.jobDataService = jobDataService;
        this.osirisJobUpdatesManager = osirisJobUpdatesManager;

    }

    @JmsListener(destination = OsirisQueueService.jobUpdatesQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId,
                    @Header("jobId") String internalJobId) {
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        Serializable update = null;
        try {
            update = objectMessage.getObject();
        } catch (JMSException e) {
            osirisJobUpdatesManager.onJobError(job, e);
        }
        if (update instanceof Job) {
            JobEvent jobEvent = (JobEvent) update;
            JobEventType jobEventType = jobEvent.getJobEventType();
            if (jobEventType == JobEventType.DATA_FETCHING_STARTED) {
                osirisJobUpdatesManager.onJobDataFetchingStarted(job, workerId);
            } else if (jobEventType == JobEventType.DATA_FETCHING_COMPLETED) {
                osirisJobUpdatesManager.onJobDataFetchingCompleted(job);
            } else if (jobEventType == JobEventType.PROCESSING_STARTED) {
                osirisJobUpdatesManager.onJobProcessingStarted(job);
            }
        } else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            osirisJobUpdatesManager.onJobError(job, jobError.getErrorDescription());
        } else if (update instanceof ContainerExit) {
            ContainerExit containerExit = (ContainerExit) update;
            try {
                osirisJobUpdatesManager.onContainerExit(job, containerExit.getJobEnvironment(), containerExit.getExitCode());
            } catch (Exception e) {
                osirisJobUpdatesManager.onJobError(job, e);
            }
        }
    }

}
