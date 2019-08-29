package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.wps.StopWpsJob;
import com.cgi.eoss.osiris.rpc.wps.WpsJobOutputsAvailable;
import com.cgi.eoss.osiris.rpc.wps.WpsJobSpec;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatusUpdate;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStopped;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

/**
 * <p>
 * Service to control the dispatch of WPS Jobs 
 * </p>
 */
@Log4j2
@Service
public class WpsJobDispatcher {

	private OsirisQueueService queueService;
    
    private WpsJobController wpsJobController;
    
    @Autowired
    public WpsJobDispatcher(OsirisQueueService queueService, WpsJobController wpsJobController) {
       this.queueService = queueService;
       this.wpsJobController = wpsJobController;
    }

    @JmsListener(destination = OsirisQueueService.wpsJobQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage) throws JMSException {
        LOG.debug("Checking for available jobs in the queue");
        dispatchMessage(objectMessage.getObject());
    }


    private void dispatchMessage(Object message) {
        if (message instanceof WpsJobSpec) {
            WpsJobSpec wpsJobSpec = (WpsJobSpec) message;
            launchJob(wpsJobSpec);
        }
        else if (message instanceof StopWpsJob) {
        	StopWpsJob stopWpsJob = (StopWpsJob) message;
        	try {
				wpsJobController.stopWpsJob(stopWpsJob);
	            Map<String, Object> headers = new HashMap<>();
	        	headers.put("jobId", stopWpsJob.getJob().getIntJobId());
	        	queueService.sendObject(OsirisQueueService.wpsJobUpdatesQueueName, headers, WpsJobStopped.newBuilder().setJob(stopWpsJob.getJob()).build());
			} catch (WpsException e) {
				sendError(stopWpsJob.getJob().getIntJobId(), e);
			}
        }
    }

	private void launchJob(WpsJobSpec wpsJobSpec) {
		try {
			WpsJobStatus wpsJobStatus = wpsJobController.launchWpsJob(wpsJobSpec);
			switch (wpsJobStatus) {
			case FAILED: 
				sendError(wpsJobSpec.getJob().getIntJobId(), new WpsException("Wps Execute Response returned failed status"));
				break;
			case ACCEPTED:
			case PAUSED:
			case STARTED:
				sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobStatusUpdate.newBuilder().setJob(wpsJobSpec.getJob()).setStatus(wpsJobStatus).build());
				break;
			case SUCCEEDED:
				sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobStatusUpdate.newBuilder().setJob(wpsJobSpec.getJob()).setStatus(wpsJobStatus).build());
				if (wpsJobController.hasWpsOutputs(wpsJobSpec)) {
					sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobOutputsAvailable.newBuilder().setJob(wpsJobSpec.getJob()).build());
				}
				break;
			default:
				sendError(wpsJobSpec.getJob().getIntJobId(), new WpsException("Unrecognized WPS status"));
		}
		} catch (WpsException e) {
			sendError(wpsJobSpec.getJob().getIntJobId(), e);
		}
	}


	private void sendError(String jobId, Exception e) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("jobId", jobId);
		LOG.error("Error in wps execution", e);
		queueService.sendObject(OsirisQueueService.wpsJobUpdatesQueueName, headers, JobError.newBuilder().setErrorDescription(e.getMessage()).build());
	}
		
	private void sendMessage(String jobId, Object message) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("jobId", jobId);
		queueService.sendObject(OsirisQueueService.wpsJobUpdatesQueueName, headers, message);
	}
}
