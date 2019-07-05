package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.queues.service.Message;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.FtpJobStarted;
import com.cgi.eoss.osiris.rpc.FtpJobStopped;
import com.cgi.eoss.osiris.rpc.JobFtpFileAvailable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

@Component
@Log4j2
public class OsirisFtpJobUpdatesDispatcher {

    private static final long UPDATE_RETRIES_PERIOD_MS = 60_000;
	private final JobDataService jobDataService;
    private OsirisFtpJobUpdatesManager osirisFtpJobUpdatesManager;
    private OsirisQueueService osirisQueueService;
    
    @Autowired
    public OsirisFtpJobUpdatesDispatcher(JobDataService jobDataService, OsirisFtpJobUpdatesManager osirisFtpJobUpdatesManager, OsirisQueueService osirisQueueService) {
        this.jobDataService = jobDataService;
        this.osirisFtpJobUpdatesManager = osirisFtpJobUpdatesManager;
        this.osirisQueueService = osirisQueueService;

    }

    @JmsListener(destination = OsirisQueueService.ftpJobUpdatesQueueName, selector = "messageType IS NULL OR messageType <> 'FTPFileAvailable'", concurrency = "1-1")
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage,
    		@Header("jobId") String internalJobId) {
    	Job job = jobDataService.reload(Long.parseLong(internalJobId));
        Serializable update = null;
        try {
        	update = objectMessage.getObject();
        	if (update instanceof FtpJobStopped) {
        		osirisFtpJobUpdatesManager.onJobStopped(job);
        	}
        	else if (update instanceof FtpJobStarted) {
        		osirisFtpJobUpdatesManager.onJobStarted(job);
        	}
        } catch (JMSException e) {
        	osirisFtpJobUpdatesManager.onJobError(job, e);
        }
    }
    
    @JmsListener(containerFactory = "rateLimitedJmsListenerContainerFactory", destination = OsirisQueueService.ftpJobUpdatesQueueName, selector = "messageType = 'FTPFileAvailable'", concurrency = "1-1")
    public void receiveDownloadRequest(@Payload ObjectMessage objectMessage, @Headers Map<String, Object> headers){
    	Job job = jobDataService.reload(Long.parseLong((String) headers.get("jobId")));
        JobFtpFileAvailable jobFtpFileAvailable;
		try {
			jobFtpFileAvailable = (JobFtpFileAvailable) objectMessage.getObject();
			osirisFtpJobUpdatesManager.onJobFtpFileAvailable(job, jobFtpFileAvailable.getFtpRoot(), URI.create(jobFtpFileAvailable.getFileUri()));
		}
		catch (IOException e) {
			int priority;
			Object object;
			try {
				priority = objectMessage.getJMSPriority();
				object = objectMessage.getObject();
				Map<String, Object> retryHeaders = new HashMap<>();
				retryHeaders.put("jobId", headers.get("jobId"));
				retryHeaders.put("messageType", headers.get("messageType"));
				retryHeaders.put("retried", headers.getOrDefault("retried", false));
				osirisQueueService.sendObject(OsirisQueueService.ftpJobUpdatesRetryQueueName, retryHeaders, object, priority);
			} catch (JMSException e1) {
				LOG.error("Error processing message: ", e);
				throw new RuntimeException(e);
			}
	    }
		catch (Exception e) {
			LOG.error("Error processing message: ", e);
			throw new RuntimeException(e);
		}
    }
    
    @Scheduled(fixedDelay = UPDATE_RETRIES_PERIOD_MS)
    public void scheduleUpdateRetries() {
    	//Clean all retried message
    	//TODO The behavior below should be part of a transaction to avoid message loss between the deque and reque operation
    	Message message = osirisQueueService.receiveSelectedNoWait(OsirisQueueService.ftpJobUpdatesRetryQueueName, "retried = true");
    	while (message != null) {
    		LOG.info("Cleaning message");
    		Map<String, Object> headers = new HashMap<>();
			headers.put("jobId", message.getHeaders().get("jobId"));
			headers.put("messageType", message.getHeaders().get("messageType"));
			headers.put("retried", false);
			osirisQueueService.sendObject(OsirisQueueService.ftpJobUpdatesRetryQueueName, headers, message.getPayload(), message.getPriority());
			message = osirisQueueService.receiveSelectedNoWait(OsirisQueueService.ftpJobUpdatesRetryQueueName, "retried = true");
    	}
    	
    	//Process non retried message
    	message = osirisQueueService.receiveSelectedNoWait(OsirisQueueService.ftpJobUpdatesRetryQueueName, "retried = false");
    	while (message != null) {
    		Map<String, Object> headers = new HashMap<>();
			headers.put("jobId", message.getHeaders().get("jobId"));
			headers.put("messageType", message.getHeaders().get("messageType"));
			headers.put("retried", true);
			LOG.info("Requeueing message");
			osirisQueueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, message.getPayload(), message.getPriority());
			message = osirisQueueService.receiveSelectedNoWait(OsirisQueueService.ftpJobUpdatesRetryQueueName, "retried = false");
    	}
    }

}
