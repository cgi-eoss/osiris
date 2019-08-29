package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.FtpJobSpec;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.StopFtpJob;
import com.cgi.eoss.osiris.rpc.wps.ExecuteWpsParams;
import com.cgi.eoss.osiris.rpc.wps.StopWpsJob;
import com.cgi.eoss.osiris.rpc.wps.WpsJobSpec;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import lombok.extern.log4j.Log4j2;
import net.opengis.ows.x11.BoundingBoxType;
import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.DataType;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.OutputReferenceType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.StatusType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Service to control the execution of WPS Jobs 
 * </p>
 */
@Log4j2
public class WpsJobController {

    private static final int WPS_CHECK_FREQUENCY_SEC = 10;
    
    private static final String WPS_SCHEDULED_JOB_GROUP = "osiris-wps";
    
    private static final String WPS_SCHEDULED_JOB_PREFIX = "wps-job-";
   
    private ScheduledJobService scheduledJobService;
    
    private WpsExecutionController wpsExecutionController;

	private OsirisQueueService queueService;

	private Path outputPath;
    
	public WpsJobController(ScheduledJobService scheduledJobService, OsirisQueueService queueService, WpsExecutionController wpsExecutionController, Path outputPath) {
       this.scheduledJobService = scheduledJobService;
       this.wpsExecutionController = wpsExecutionController;
       this.queueService = queueService;
       this.outputPath = outputPath;
    }


	public void stopWpsJob(StopWpsJob stopWpsJob) throws WpsException {
		String wpsJobIdentity = WPS_SCHEDULED_JOB_PREFIX + stopWpsJob.getJob().getIntJobId();
        Map<String, Object> jobContext = scheduledJobService.getJobContext(wpsJobIdentity, WPS_SCHEDULED_JOB_GROUP);
        String jobCancelWpsUrl = (String) jobContext.get("jobCancelWpsUrl");
        if (jobCancelWpsUrl != null) {
        	//Use the custom url to cancel
        	wpsExecutionController.executeViaGET(jobCancelWpsUrl);
        	unscheduleWpsJob(stopWpsJob.getJob());
        }
        else {
        	throw new WpsException("This WPS process does not support job termination");
        }
	}
	
	public void unscheduleWpsJob(Job job) {
		LOG.info("Unscheduling WPS job {} ", job.getId());
		String wpsJobIdentity = WPS_SCHEDULED_JOB_PREFIX + job.getIntJobId();
        Map<String, Object> jobContext = scheduledJobService.getJobContext(wpsJobIdentity, WPS_SCHEDULED_JOB_GROUP);
        boolean ftpJobAssociated = (boolean) jobContext.getOrDefault("ftpJob", false);
        if (ftpJobAssociated) {
        	stopFtpJob(job);
        }
        scheduledJobService.unscheduleJob(wpsJobIdentity, WPS_SCHEDULED_JOB_GROUP);
        scheduledJobService.deleteJob(wpsJobIdentity, WPS_SCHEDULED_JOB_GROUP);
        
	}
    
    public WpsJobStatus launchWpsJob(WpsJobSpec wpsJobSpec) throws WpsException {
		ExecuteWpsParams executeWpsParams = wpsJobSpec.getExecuteWpsParams();
		Map<String, ComplexDataParam> complexDataParams = WpsUtils.rpcComplexParamListToMap(executeWpsParams.getComplexDataWpsParamList());
		Map<String, LiteralDataParam> literalDataParams = WpsUtils.rpcLiteralParamListToMap(executeWpsParams.getLiteralDataWpsParamList());
		Map<String, BoundingBoxDataParam> boundingBoxDataParams = WpsUtils.rpcBoundingBoxParamListToMap(executeWpsParams.getBoundingBoxDataWpsParamList());
		Map<String, ComplexDataOutputDefinition> complexDataOutputDefinitions = WpsUtils.rpcComplexDataOutputDefinitionListToMap(executeWpsParams.getComplexDataWpsOutputDefinitionList());
		ProcessDescriptionsDocument processDescriptionDocument = wpsExecutionController.describeProcess(executeWpsParams.getWpsServerUrl(), executeWpsParams.getProcessId());
		ProcessDescriptionType processDescription = processDescriptionDocument.getProcessDescriptions().getProcessDescriptionArray()[0];
		boolean requestOutputStorage = executeWpsParams.getStoreOutputs() && processDescription.getStoreSupported();
		Object response = wpsExecutionController.executeWpsProcess(executeWpsParams.getWpsServerUrl(),
				executeWpsParams.getProcessId(), requestOutputStorage, complexDataParams, literalDataParams, boundingBoxDataParams, complexDataOutputDefinitions);
		if (response instanceof ExecuteResponseDocument) {
			return handleExecuteResponseDocument(wpsJobSpec, response);
		}
		else if (response instanceof InputStream) {
			//If raw data is returned, there shall be a single output	
			 OutputDescriptionType outputDescriptionType = processDescription.getProcessOutputs().getOutputArray()[0];
			try {
				storeOutputFromStream(wpsJobSpec, outputDescriptionType.getIdentifier().getStringValue(), (InputStream) response);
			} catch (IOException e) {
				throw new WpsException("Error storing output file", e);
			}
			return WpsJobStatus.SUCCEEDED;
		}
		else if (response instanceof ExceptionReportDocument) {
			ExceptionReportDocument doc = (ExceptionReportDocument) response;
			throw new WpsException("Exception in WPS invocation" + doc.getExceptionReport().toString());
		}
		else {
			throw new WpsException("Unrecognized WPS response");
		}
    }
    
    public boolean hasWpsOutputs(WpsJobSpec wpsJobSpec){
    	Path jobDir = outputPath.resolve(wpsJobSpec.getJob().getId());
		return Files.exists(jobDir);
    }

	public WpsJobStatus handleExecuteResponseDocument(WpsJobSpec wpsJobSpec, Object response) throws WpsException {
		ExecuteResponseDocument responseDocument = (ExecuteResponseDocument) response;
		StatusType status = responseDocument.getExecuteResponse().getStatus();
		WpsJobStatus wpsJobStatus = WpsUtils.getWpsJobStatusFromExecuteResponseStatus(status);
		switch (wpsJobStatus) {
			case FAILED: 
				break;
			case SUCCEEDED:
				try {
					return handleSucceededWpsJob(wpsJobSpec, responseDocument);
				} catch (IOException e) {
					throw new WpsException("Error storing output file", e);
				}
			case ACCEPTED:
			case PAUSED:
			case STARTED:
				scheduleWpsStatusUpdateJob(wpsJobSpec, responseDocument);
				break;
			default:
				throw new WpsException("Unrecognized WPS status");
		}
		return wpsJobStatus;
	}
    
    
    
    private WpsJobStatus handleSucceededWpsJob(WpsJobSpec wpsJobSpec, ExecuteResponseDocument responseDocument) throws IOException {
		Map<String, Object> asyncOutputs = getCustomAsyncOutputs(responseDocument);
		if (asyncOutputs.isEmpty()) {
			LOG.info("Wps job {} succeeded", wpsJobSpec.getJob().getIntJobId());
			storeOutputs(wpsJobSpec, responseDocument);
			scheduleFtpJobIfNeeded(wpsJobSpec, responseDocument);
			return WpsJobStatus.SUCCEEDED;
		}
		else {
			LOG.info("Wps job {} returned immediately and will be updated later", wpsJobSpec.getJob().getIntJobId());
			boolean hasFtpJob = scheduleFtpJobIfNeeded(wpsJobSpec, responseDocument);
			scheduleStatusUpdateJob(wpsJobSpec, asyncOutputs, hasFtpJob);
			return WpsJobStatus.ACCEPTED;
		}
	}
    
    private void storeOutputs(WpsJobSpec wpsJobSpec, ExecuteResponseDocument responseDocument) throws IOException {
		OutputDataType[] processOutputs = responseDocument.getExecuteResponse().getProcessOutputs().getOutputArray();
		for (OutputDataType processOutput: processOutputs) {
			storeOutput(wpsJobSpec, processOutput);
		}
	}

    private void storeOutput(WpsJobSpec wpsJobSpec, OutputDataType processOutput) throws IOException {
    	Path jobDir = outputPath.resolve(wpsJobSpec.getJob().getId());
		String outputIdentifier = processOutput.getIdentifier().getStringValue();
		Path outputDir  = jobDir.resolve(outputIdentifier);
		Files.createDirectories(outputDir);
		if (processOutput.getData() != null) {
			storeEmbeddedData(outputDir, outputIdentifier, processOutput.getData());
		}
		else if (processOutput.getReference() != null) {
			Path referenceFile = outputDir.resolve(getFileName(outputIdentifier, processOutput.getReference()));
			InputStream in = new URL(processOutput.getReference().getHref()).openStream();
			try(ReadableByteChannel readableByteChannel = Channels.newChannel(in); 
			FileChannel fileChannel = FileChannel.open(referenceFile)){
				fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
		}
	}
	
	private void storeEmbeddedData(Path outputDir, String outputIdentifier, DataType dataType) throws IOException {
		if (dataType.getLiteralData() != null) {
			Path literalFile = outputDir.resolve(outputIdentifier + "_value.txt");
			Files.write(literalFile, dataType.getLiteralData().getStringValue().getBytes());
		}
		else if (dataType.getComplexData() != null) {
			ComplexDataType cd = dataType.getComplexData();
			Path complexFile = outputDir.resolve(getFileName(outputIdentifier, cd));
			Files.write(complexFile, cd.toString().getBytes());
		}
		
		else if (dataType.getBoundingBoxData() != null) {
			BoundingBoxType boundingBoxData = dataType.getBoundingBoxData();
			Path boundingBoxFile = outputDir.resolve(getFileName(outputIdentifier, dataType.getBoundingBoxData()));
			Files.write(boundingBoxFile, boundingBoxData.toString().getBytes());
		}
	}

	private String getFileName(String outputIdentifier, ComplexDataType complexDataType) {
		String extension = MimeTypeToExtension.getExtension(complexDataType.getMimeType());
		if (extension.equals(MimeTypeToExtension.EXTENSION_UNKNOWN)){
			extension = "xml";
		}
		return outputIdentifier + "." + extension;
	}
	
	private String getFileName(String outputIdentifier, OutputReferenceType outputReferenceType) {
		String extension = MimeTypeToExtension.getExtension(outputReferenceType.getMimeType());
		if (extension.equals(MimeTypeToExtension.EXTENSION_UNKNOWN)){
			extension = "xml";
		}
		return outputIdentifier + "." + extension;
	}

	private String getFileName(String outputIdentifier, BoundingBoxType boundingBoxData) {
		return outputIdentifier + ".xml";
	}


	private void storeOutputFromStream(WpsJobSpec wpsJobSpec, String outputIdentifier, InputStream is) throws IOException {
		Path jobDir = outputPath.resolve(wpsJobSpec.getJob().getId());
		Path outputDir = jobDir.resolve(outputIdentifier);
		Files.createDirectories(outputDir);
		String fileName = outputIdentifier + ".xml";
		//TODO Get the format in which the output has been requested, or the default one
		Path outputFile = outputDir.resolve(fileName);
		try(ReadableByteChannel readableByteChannel = Channels.newChannel(is); 
		FileChannel fileChannel = FileChannel.open(outputFile)){
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		}
	}
	
	    
	private boolean scheduleFtpJobIfNeeded(WpsJobSpec wpsJobSpec, ExecuteResponseDocument responseDocument) {
    	OutputDataType[] outputs = responseDocument.getExecuteResponse().getProcessOutputs().getOutputArray();
		String ftpLocation = getOutput(outputs, "output.output");
		if (ftpLocation != null) {
			LOG.info("Scheduled FTP job for WPS job {}", wpsJobSpec.getJob().getIntJobId());
			FtpJobSpec ftpJobSpec = FtpJobSpec.newBuilder()
					.setFtpRootUri(ftpLocation.replace(" ", "%20"))
					.setJob(wpsJobSpec.getJob())
					.build();
			queueService.sendObject(OsirisQueueService.ftpJobQueueName, ftpJobSpec);
			return true;
		}
		return false;
	}
	
	private void stopFtpJob(Job job) {
		LOG.info("Stop FTP job for WPS job {} ", job.getId());
		
    	StopFtpJob stopFtpJob = StopFtpJob.newBuilder()
				.setJob(job)
				.build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, stopFtpJob);
	}

	private void scheduleWpsStatusUpdateJob(WpsJobSpec wpsJobSpec, ExecuteResponseDocument responseDocument) {
    	String statusLocation = responseDocument.getExecuteResponse().getStatusLocation();
		if (statusLocation != null) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("jobStatusLocation", statusLocation);
			scheduleStatusUpdateJob(wpsJobSpec, parameters, false);
	   }
    }

	private void scheduleStatusUpdateJob(WpsJobSpec wpsJobSpec, Map<String, Object> parameters, boolean hasFtpJob) {
		Map<String, Object> jobContext = new HashMap<>();
		jobContext.putAll(parameters);
		jobContext.put("wpsJobSpec", wpsJobSpec);
		if (hasFtpJob) {
			jobContext.put("ftpJob", true);
		}
		String wpsJobIdentity = WPS_SCHEDULED_JOB_PREFIX + wpsJobSpec.getJob().getIntJobId();
		scheduledJobService.scheduleJobEveryNSeconds(WPSScheduledJob.class, wpsJobIdentity, WPS_SCHEDULED_JOB_GROUP, jobContext, WPS_CHECK_FREQUENCY_SEC);
	}

	private Map<String, Object> getCustomAsyncOutputs(ExecuteResponseDocument responseDocument) {
		Map<String, Object> asyncOutputs = new HashMap<>();
		OutputDataType[] outputs = responseDocument.getExecuteResponse().getProcessOutputs().getOutputArray();
		
		String processIdentifier = getOutput(outputs, "output.identifier");
		if (processIdentifier != null) {
			asyncOutputs.put("processIdentifier", processIdentifier);
		}
		
		String statusWpsUrl = getOutput(outputs, "output.status");
		if (statusWpsUrl != null) {
			asyncOutputs.put("jobStatusWpsUrl", statusWpsUrl);
		}
		String cancelWpsUrl = getOutput(outputs, "output.cancel");
		if (statusWpsUrl != null) {
			asyncOutputs.put("jobCancelWpsUrl", cancelWpsUrl);
		}
		
		return asyncOutputs;
	}

	private String getOutput(OutputDataType[] outputs, String identifier) {
		for (OutputDataType output: outputs) {
			if (output.getIdentifier().getStringValue().equals(identifier)){
				return output.getData().getLiteralData().getStringValue();
			}
		}
		return null;
	}
}
