package com.cgi.eoss.osiris.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.osiris.rpc.worker.JobEnvironment;
import com.cgi.eoss.osiris.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc.OsirisWorkerBlockingStub;
import com.cgi.eoss.osiris.rpc.worker.OutputFileItem;
import com.cgi.eoss.osiris.rpc.worker.OutputFileList;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Log4j2
public class OsirisJobUpdatesManager {
	
	private final JobDataService jobDataService;
    private final DynamicProxyService dynamicProxyService;
    private final OsirisGuiServiceManager guiService;
    private final CachingWorkerFactory workerFactory;
    private final OsirisSecurityService securityService;
    private final OutputIngestionService outputIngestionService;
    
	 @Autowired
	    public OsirisJobUpdatesManager(JobDataService jobDataService, 
	    		DynamicProxyService dynamicProxyService, 
	    		OsirisGuiServiceManager guiService, 
	    		CachingWorkerFactory workerFactory,
	    		OsirisSecurityService securityService,
	    		OutputIngestionService outputIngestionService) {
	        this.jobDataService = jobDataService;
	        this.dynamicProxyService = dynamicProxyService;
	        this.guiService = guiService;
	        this.workerFactory = workerFactory;
	        this.securityService = securityService;
	        this.outputIngestionService = outputIngestionService;
	    }

    

    void onJobDataFetchingStarted(Job job, String workerId) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setWorkerId(workerId);
        job.setStartTime(LocalDateTime.now());
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);

    }

    void onJobDataFetchingCompleted(Job job) {
        LOG.info("Launching docker container for job {}", job.getExtId());
    }

    void onJobProcessingStarted(Job job) {
        OsirisService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(),
                service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == OsirisService.Type.APPLICATION) {
            String zooId = job.getExtId();
            OsirisWorkerBlockingStub worker = workerFactory.getWorkerById(job.getWorkerId());
            com.cgi.eoss.osiris.rpc.Job rpcJob = ModelToGrpcUtils.toRpcJob(job);
            PortBinding portBinding = guiService.getGuiPortBinding(worker, rpcJob);
            ReverseProxyEntry guiEntry = dynamicProxyService.getProxyEntry(rpcJob, portBinding.getBinding().getIp(), portBinding.getBinding().getPort());
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId,
                    job.getConfig().getService().getName(), guiEntry.getPath());
            job.setGuiUrl(guiEntry.getPath());
            job.setGuiEndpoint(guiEntry.getEndpoint());
            jobDataService.save(job);
            dynamicProxyService.update();
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);

    }

    void onContainerExit(Job job, JobEnvironment jobEnvironment,
            int exitCode) throws Exception {
        switch (exitCode) {
            case 0:
                // Normal exit
                break;
            case 137:
                LOG.info("Docker container for {} terminated via SIGKILL (exit code 137)",
                        job.getExtId());
                break;
            case 143:
                LOG.info("Docker container for {} terminated via SIGTERM (exit code 143)",
                        job.getExtId());
                break;
            default:
                throw new Exception("Docker container returned with exit code " + exitCode);
        }
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        job.setGuiEndpoint(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
        	OsirisWorkerBlockingStub worker = workerFactory.getWorkerById(job.getWorkerId());
            ingestOutput(job, ModelToGrpcUtils.toRpcJob(job), worker, jobEnvironment);
        } catch (IOException e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    void onJobError(Job job, String description) {
        LOG.error("Error in Job {}: {}",
                job.getExtId(), description);
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
    
    private void ingestOutput(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob,
            OsirisWorkerBlockingStub worker, JobEnvironment jobEnvironment)
            throws IOException, Exception {
        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath =
                listOutputFiles(job, rpcJob, worker, jobEnvironment);
        // Repatriate output files
        Multimap<String, OsirisFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, jobEnvironment, outputsByRelativePath);
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entries().stream()
                .collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(),
                        MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
        jobDataService.save(job);
        if (job.getConfig().getService().getType() == OsirisService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values())
                    .forEach(f -> securityService.publish(OsirisFile.class, f.getId()));
        }
        if (job.getParentJob() != null) {
            Job parentJob = job.getParentJob();
            if (allChildJobCompleted(parentJob)) {
                completeParentJob(parentJob);
            }
         }
     }

    private void completeParentJob(Job parentJob) {
        //Must collect all child jobs, save for parent and send a response.
        Multimap<String, OsirisFile> jobOutputFiles = collectSubJobOutputs(parentJob);
        // Wrap up the parent job
        parentJob.setStatus(Job.Status.COMPLETED);
        parentJob.setStage(JobStep.OUTPUT_LIST.getText());
        parentJob.setEndTime(LocalDateTime.now());
        parentJob.setGuiUrl(null);
        parentJob.setGuiEndpoint(null); 
        parentJob.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(
              e -> e.getKey(),
              e -> e.getValue().getUri().toString(),
              MultimapBuilder.hashKeys().hashSetValues()::build)));
        parentJob.setOutputFiles(ImmutableSet.copyOf(jobOutputFiles.values()));
        jobDataService.save(parentJob);
    }

    private SetMultimap<String, OsirisFile> collectSubJobOutputs(Job parentJob) {
        SetMultimap<String, OsirisFile> jobOutputsFiles = MultimapBuilder.hashKeys().hashSetValues().build();
        for (Job subJob: parentJob.getSubJobs()) {
          if (subJob.getStatus().equals(Status.COMPLETED)){  
              subJob.getOutputs().forEach((k, v) -> subJob.getOutputFiles().stream().
                  filter(x -> x.getUri().toString().equals(v)).findFirst().ifPresent(match -> jobOutputsFiles.put(k, match)));
          }
        }
        return jobOutputsFiles;
    }

    private boolean allChildJobCompleted(Job parentJob) {
        return parentJob.getSubJobs().stream().noneMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
    }

    private Multimap<String, String> listOutputFiles(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob,
            OsirisWorkerGrpc.OsirisWorkerBlockingStub worker, JobEnvironment jobEnvironment)
            throws Exception {
        OsirisService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob).setOutputsRootPath(jobEnvironment.getOutputDir()).build());
        List<String> relativePaths = outputFileList.getItemsList().stream()
                .map(OutputFileItem::getRelativePath).collect(toList());

        Multimap<String, String> outputsByRelativePath;
        if (service.getType() == OsirisService.Type.APPLICATION) {
            outputsByRelativePath = IntStream.range(0, relativePaths.size()).boxed()
            .collect(ArrayListMultimap::create, (mm,i) -> mm.put(Integer.toString(i+1), relativePaths.get(i)), Multimap::putAll);
            
        } else {
            // Ensure we have at least one file per expected output
            Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs()
                    .stream().map(OsirisServiceDescriptor.Parameter::getId).collect(toSet());
            outputsByRelativePath = ArrayListMultimap.create();
            
            for (String expectedOutputId : expectedServiceOutputIds) {
                List<String> relativePathValues = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutputId))
                        .collect(Collectors.toList());
                //TODO Check against user defined min/max occurs 
                //TODO Evaluate WPS compatibility issues with missing output
                if (relativePathValues.size() > 0) {
                    outputsByRelativePath.putAll(expectedOutputId, relativePathValues);
                } else {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.info("Service defined output with ID '{}' but no matching directory was found in the job outputs", expectedOutputId);
                    }
                }
            }
        }
        return outputsByRelativePath;
    }

    private Multimap<String, OsirisFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.osiris.rpc.Job rpcJob,
            OsirisWorkerGrpc.OsirisWorkerBlockingStub worker, JobEnvironment jobEnvironment,
            Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        Multimap<String, OsirisFile> outputFiles = ArrayListMultimap.create();
        OsirisWorkerGrpc.OsirisWorkerStub asyncWorker = OsirisWorkerGrpc.newStub(worker.getChannel());
        for (String outputId : outputsByRelativePath.keySet()) {
        	for (String relativePath : outputsByRelativePath.get(outputId)) {
        		GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString()).build();
        		OsirisFile osirisFile = outputIngestionService.repatriateAndIngestOutputFile(job, outputId, Paths.get(relativePath), f -> asyncWorker.getOutputFile(getOutputFileParam, f));
        		outputFiles.put(outputId, osirisFile);
    		}
        }
        return outputFiles;
    }
}
