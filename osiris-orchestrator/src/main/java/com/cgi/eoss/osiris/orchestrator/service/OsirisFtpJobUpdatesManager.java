package com.cgi.eoss.osiris.orchestrator.service;

import static java.util.stream.Collectors.toSet;

import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.rpc.LocalFtpHarvester;
import com.cgi.eoss.osiris.rpc.ftp.harvester.GetFileParams;
import com.google.common.collect.MultimapBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@Log4j2
public class OsirisFtpJobUpdatesManager {

	private static final String FALLBACK_OUTPUT_ID = "1";
	private final JobDataService jobDataService;
	private final OutputIngestionService outputIngestionService;
	private LocalFtpHarvester localFtpHarvester;
 
	@Autowired
	public OsirisFtpJobUpdatesManager(JobDataService jobDataService, OutputIngestionService outputIngestionService, LocalFtpHarvester localFtpHarvester) {
		this.jobDataService = jobDataService;
		this.outputIngestionService = outputIngestionService;
		this.localFtpHarvester = localFtpHarvester;
	}
	
	public void onJobStarted(Job job) {
		job.setStatus(Job.Status.RUNNING);
		job.setStage(JobStep.PROCESSING.getText());
		job.setStartTime(LocalDateTime.now());
		jobDataService.save(job);
		
	}

	public void onJobStopped(Job job) {
		job.setStage(JobStep.OUTPUT_LIST.getText());
		jobDataService.save(job);
		
	}
	
	public void onJobCompleted(Job job) {
		job.setStatus(Job.Status.COMPLETED);
		job.setStage(JobStep.OUTPUT_LIST.getText());
		job.setEndTime(LocalDateTime.now());
		jobDataService.save(job);
		
	}

	public void onJobFtpFileAvailable(Job job, String ftpRoot, URI fileUri) throws IOException, InterruptedException {
		try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("OSIRIS FTP Job Updates Manager").put("userId", String.valueOf(job.getOwner().getId()))
                        .put("serviceId", String.valueOf(job.getConfig().getService().getId())).put("zooId", job.getExtId())) {
        
			try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
				LOG.info("Found new FTP file {}", fileUri.getPath());
			}
			ingestFile(job, ftpRoot, fileUri);
			try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
				LOG.info("Ingested FTP file {}", fileUri.getPath());
			}
		}
	}

	void onJobError(Job job, String description) {
		LOG.error("Error in Job {}: {}", job.getExtId(), description);
		endJobWithError(job);
	}

	void onJobError(Job job, Throwable t) {
		LOG.error("Error in Job " + job.getExtId(), t);
		endJobWithError(job);
	}

	private void endJobWithError(Job job) {
		job.setStatus(Job.Status.ERROR);
		job.setEndTime(LocalDateTime.now());
		jobDataService.save(job);
	}

	private void ingestFile(Job job, String ftpRoot, URI fileUri) throws IOException, InterruptedException {
		// Match the file to the corresponding output
		String outputId;
		Path path = Paths.get(fileUri.getPath());
		Path relativePath = Paths.get(ftpRoot).relativize(path);
		OsirisService service = job.getConfig().getService();
		//If service has no output identifier, return fallback id
		if (service.getServiceDescriptor().getDataOutputs() == null || service.getServiceDescriptor().getDataOutputs().isEmpty()) {
			outputId = FALLBACK_OUTPUT_ID;
		}
		else {
			Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs().stream()
					.map(OsirisServiceDescriptor.Parameter::getId).collect(toSet());
			Path pathRoot = relativePath.getParent();
			if (pathRoot != null && expectedServiceOutputIds.contains(pathRoot.toString())) {
				outputId = pathRoot.toString();
				relativePath = relativePath.relativize(pathRoot); 
			}
			else {
				//Fall back to first service output
				outputId = service.getServiceDescriptor().getDataOutputs().get(0).getId();
			}
		}
		// Repatriate output file
		GetFileParams getFileParams = GetFileParams.newBuilder().setFileUri(fileUri.toString()).build();
		LOG.info("Fetching file {}", getFileParams.getFileUri());
		
		OsirisFile osirisFile = outputIngestionService.repatriateAndIngestOutputFile(job, outputId, relativePath, f -> localFtpHarvester.asyncGetFile(getFileParams, f));
		
		if(osirisFile == null) {
			try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
				LOG.error("Ingestion of file {} failed - will be retried", fileUri.getPath());
			}
			throw new IOException("Ingestion of file " + fileUri.getPath() + " failed - will be retried");
		}
		if (job.getOutputs() == null) {
			job.setOutputs(MultimapBuilder.hashKeys().hashSetValues().build());
		}
		job.getOutputs().put(outputId, osirisFile.getUri().toString());
		job.getOutputFiles().add(osirisFile);
		jobDataService.save(job);
	}
}
