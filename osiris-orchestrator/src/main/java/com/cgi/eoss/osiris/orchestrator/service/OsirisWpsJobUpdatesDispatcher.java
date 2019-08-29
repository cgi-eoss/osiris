package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.rpc.wps.WpsJobOutputsAvailable;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatusUpdate;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStopped;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

@Component
public class OsirisWpsJobUpdatesDispatcher {

    private final JobDataService jobDataService;
    private OsirisWpsJobUpdatesManager osirisWpsJobUpdatesManager;


    @Autowired
    public OsirisWpsJobUpdatesDispatcher(JobDataService jobDataService, OsirisWpsJobUpdatesManager osirisWpsJobUpdatesManager) {
        this.jobDataService = jobDataService;
        this.osirisWpsJobUpdatesManager = osirisWpsJobUpdatesManager;

    }

    @JmsListener(destination = OsirisQueueService.wpsJobUpdatesQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage, @Header("jobId") String internalJobId) {
        Job job = jobDataService.reload(Long.parseLong(internalJobId));
        Serializable update = null;
        try {
            update = objectMessage.getObject();
        } catch (JMSException e) {
        	osirisWpsJobUpdatesManager.onJobError(job, e);
        }
        
        if (update instanceof WpsJobStatusUpdate) {
        	WpsJobStatusUpdate wpsJobStatusUpdate = (WpsJobStatusUpdate) update;
        	osirisWpsJobUpdatesManager.onWpsJobStatusUpdate(job, wpsJobStatusUpdate.getStatus());
        } 
        else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            osirisWpsJobUpdatesManager.onJobError(job, jobError.getErrorDescription());
        } 
        else if (update instanceof WpsJobStopped) {
        	osirisWpsJobUpdatesManager.onJobStopped(job);
        }
        else if (update instanceof WpsJobOutputsAvailable) {
        	try {
				osirisWpsJobUpdatesManager.ingestOutputs(job);
			} catch (IOException | InterruptedException e) {
				 osirisWpsJobUpdatesManager.onJobError(job, e);
			}
        }
    }
}
