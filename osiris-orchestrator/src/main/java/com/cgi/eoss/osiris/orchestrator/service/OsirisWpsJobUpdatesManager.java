package com.cgi.eoss.osiris.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.rpc.LocalWpsResultsManager;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import com.cgi.eoss.osiris.rpc.wps.controller.DeleteWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.GetWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.ListWpsOutputFilesParam;
import com.cgi.eoss.osiris.rpc.wps.controller.WpsOutputFileItem;
import com.cgi.eoss.osiris.rpc.wps.controller.WpsOutputFileList;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Log4j2
public class OsirisWpsJobUpdatesManager {

	private final JobDataService jobDataService;
	private OutputIngestionService outputIngestionService;
	private LocalWpsResultsManager localWpsResultsManager;

	@Autowired
	public OsirisWpsJobUpdatesManager(JobDataService jobDataService, OutputIngestionService outputIngestionService, LocalWpsResultsManager localWpsResultsManager) {
		this.jobDataService = jobDataService;
		this.outputIngestionService = outputIngestionService;
		this.localWpsResultsManager = localWpsResultsManager;
	}

	public void onWpsJobStatusUpdate(Job job, WpsJobStatus wpsJobStatus) {
		switch (wpsJobStatus) {
		case ACCEPTED:
			job.setStatus(Job.Status.CREATED);
			job.setStartTime(LocalDateTime.now());
			break;
		case STARTED:
			if (job.getStartTime() == null) {
				job.setStartTime(LocalDateTime.now());
			}
			job.setStatus(Job.Status.RUNNING);
			job.setStage(JobStep.PROCESSING.getText());
			break;
		case FAILED:
			job.setEndTime(LocalDateTime.now());
			job.setStatus(Job.Status.ERROR);
			break;
		case SUCCEEDED:
			if (job.getStartTime() == null) {
				job.setStartTime(LocalDateTime.now());
			}
			job.setEndTime(LocalDateTime.now());
			job.setStage(JobStep.OUTPUT_LIST.getText());
			break;
		default:
			return;
		}
		logStatusUpdate(job, wpsJobStatus.toString());
		jobDataService.save(job);
	}

	private void logStatusUpdate(Job job, String status) {
		try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("OSIRIS Wps Job Updates Manager").put("userId", String.valueOf(job.getOwner().getId()))
                        .put("serviceId", String.valueOf(job.getConfig().getService().getId())).put("zooId", job.getExtId())) {
        
			try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
				LOG.info("WPS status update: {} ", status);
			}
		}
		
	}

	public void onJobStopped(Job job) {
		job.setStatus(Job.Status.CANCELLED);
		job.setEndTime(LocalDateTime.now());
		jobDataService.save(job);

	}

	public void onJobError(Job job, String description) {
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

	public void ingestOutputs(Job job) throws IOException, InterruptedException {
		com.cgi.eoss.osiris.rpc.Job rpcJob = ModelToGrpcUtils.toRpcJob(job);
		WpsOutputFileList outputFileList = localWpsResultsManager
				.listOutputFiles(ListWpsOutputFilesParam.newBuilder().setJob(rpcJob).build());
		List<String> relativePaths = outputFileList.getItemsList().stream().map(WpsOutputFileItem::getPath)
				.collect(toList());
		OsirisService service = job.getConfig().getService();
		Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs().stream()
				.map(OsirisServiceDescriptor.Parameter::getId).collect(toSet());
		Multimap<String, String> outputsByRelativePath = ArrayListMultimap.create();
		for (String expectedOutputId : expectedServiceOutputIds) {
			List<String> relativePathValues = relativePaths.stream()
					.filter(path -> path.startsWith(expectedOutputId)).collect(Collectors.toList());
			outputsByRelativePath.putAll(expectedOutputId, relativePathValues);

		}
		Multimap<String, OsirisFile> outputFiles = ArrayListMultimap.create();

		for (String outputId : outputsByRelativePath.keySet()) {
			for (String relativePath : outputsByRelativePath.get(outputId)) {
				// match output to output id
				GetWpsOutputFileParams getWpsOutputFileParams = GetWpsOutputFileParams.newBuilder()
						.setJob(rpcJob)
						.setPath(relativePath).build();
				OsirisFile osirisFile = outputIngestionService.repatriateAndIngestOutputFile(job, outputId, Paths.get(relativePath),
						f -> localWpsResultsManager.asyncGetFile(getWpsOutputFileParams, f));
				localWpsResultsManager.deleteFile(DeleteWpsOutputFileParams.newBuilder().setJob(rpcJob).setPath(relativePath).build());
				outputFiles.put(outputId, osirisFile);
			}
		}
		job.setStatus(Job.Status.COMPLETED);
		job.setOutputs(outputFiles.entries().stream().collect(toMultimap(Entry::getKey,
				e -> e.getValue().getUri().toString(), MultimapBuilder.hashKeys().hashSetValues()::build)));
		job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
		jobDataService.save(job);

	}
}
