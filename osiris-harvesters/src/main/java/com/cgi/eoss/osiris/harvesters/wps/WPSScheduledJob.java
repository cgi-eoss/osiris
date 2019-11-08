package com.cgi.eoss.osiris.harvesters.wps;


import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.rpc.wps.WpsJobOutputsAvailable;
import com.cgi.eoss.osiris.rpc.wps.WpsJobSpec;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatusUpdate;
import com.cgi.eoss.osiris.scheduledjobs.service.PersistentScheduledJob;
import lombok.extern.log4j.Log4j2;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.ExecuteResponseDocument.ExecuteResponse.ProcessOutputs;
import net.opengis.wps.x100.OutputDataType;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class WPSScheduledJob extends PersistentScheduledJob{
 	
	@Autowired
    WpsExecutionController wpsExecutionController;
	
	@Autowired
    WpsJobController wpsJobController;
	
	@Autowired
    OsirisQueueService queueService;
    
    @Override
    public void executeJob(Map<String, Object> jobContext) {
        pollJob(jobContext);
    }
	
	private void pollJob(Map<String, Object> jobContext) {
		WpsJobSpec wpsJobSpec = (WpsJobSpec) jobContext.get("wpsJobSpec");
        Job job = wpsJobSpec.getJob();
		String jobId = wpsJobSpec.getJob().getIntJobId();
		try {
			WpsJobStatus wpsJobStatus = getJobStatus(wpsJobSpec, jobContext);
			if (wpsJobStatus != null) {
				LOG.info("WPS job {} status update: {}", jobId, wpsJobStatus);
				switch (wpsJobStatus) {
				case FAILED: 
					sendError(wpsJobSpec.getJob().getIntJobId(), new WpsException("Wps Execute Response returned failed status"));
					unscheduleJob(job);
					break;
				case SUCCEEDED:
					sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobStatusUpdate.newBuilder().setJob(wpsJobSpec.getJob()).setStatus(wpsJobStatus).build());
					if (wpsJobController.hasWpsOutputs(wpsJobSpec)) {
						sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobOutputsAvailable.newBuilder().setJob(wpsJobSpec.getJob()).build());
					}
					unscheduleJob(job);
					break;
				case ACCEPTED:
				case PAUSED:
				case STARTED:
					sendMessage(wpsJobSpec.getJob().getIntJobId(), WpsJobStatusUpdate.newBuilder().setJob(wpsJobSpec.getJob()).setStatus(wpsJobStatus).build());
					break;
				default:
					sendError(wpsJobSpec.getJob().getIntJobId(), new WpsException("Unrecognized WPS status"));
				}
			}
		} catch (WpsException e) {
			LOG.error("Error updating wps status", e);
			sendError(jobId, e);
		}
    }
	
	private void unscheduleJob(Job job) {
		wpsJobController.unscheduleWpsJob(job);
	}

	private WpsJobStatus getJobStatus(WpsJobSpec wpsJobSpec, Map<String, Object> jobContext) throws WpsException {
		WpsJobStatus jobStatus = null;
		if (jobContext.containsKey("jobStatusLocation")) {
			LOG.info("Updating WPS job status via status location {}", wpsJobSpec.getJob().getIntJobId());
			String wpsJobStatusLocation = (String) jobContext.get("jobStatusLocation");
			jobStatus = getWpsJobStatus(wpsJobSpec, wpsJobStatusLocation);
		}
		else if (jobContext.containsKey("jobStatusWpsUrl")) {
			LOG.info("Updating WPS job status via custom Wps URL {}", wpsJobSpec.getJob().getIntJobId());
			String jobStatusWpsUrl = (String) jobContext.get("jobStatusWpsUrl");
			jobStatus = getCustomWpsJobStatus(jobStatusWpsUrl);
		}
		return jobStatus;
	}

	private WpsJobStatus getCustomWpsJobStatus(String jobStatusWpsUrl) throws WpsException {
		try {
			ExecuteResponseDocument executeResponse = wpsExecutionController.executeViaGET(jobStatusWpsUrl);
			ProcessOutputs outputs = executeResponse.getExecuteResponse().getProcessOutputs();
			String status = getOutput(outputs.getOutputArray(), "output.message");
			return WpsUtils.getWpsJobStatusFromClsJobStatus(status);
		}
		catch (WpsException e) {
			LOG.error("Error in WPS invocation to URL {}", jobStatusWpsUrl);
			throw e;
		}
	}
	
	private String getOutput(OutputDataType[] outputs, String identifier) {
		for (OutputDataType output: outputs) {
			if (output.getIdentifier().getStringValue().equals(identifier)){
				return output.getData().getLiteralData().getStringValue();
			}
		}
		return null;
	}

	private WpsJobStatus getWpsJobStatus(WpsJobSpec wpsJobSpec, String wpsJobStatusLocation) throws WpsException {
		ExecuteResponseDocument executeResponse = wpsExecutionController.getWpsStatus(wpsJobStatusLocation);
		return wpsJobController.handleExecuteResponseDocument(wpsJobSpec, executeResponse);
	}

	private void sendError(String jobId, Exception e) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("jobId", jobId);
		queueService.sendObject(OsirisQueueService.wpsJobUpdatesQueueName, headers, JobError.newBuilder().setErrorDescription(e.getMessage()).build());
	}
	
	private void sendMessage(String jobId, Object message) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("jobId", jobId);
		queueService.sendObject(OsirisQueueService.wpsJobUpdatesQueueName, headers, message);
	}
}
