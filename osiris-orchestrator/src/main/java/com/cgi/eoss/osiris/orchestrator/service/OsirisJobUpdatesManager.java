package com.cgi.eoss.osiris.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.osiris.catalogue.util.GeoUtil;
import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.OsirisFilesRelation.Type;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Relation;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata;
import com.cgi.eoss.osiris.model.internal.RetrievedOutputFile;
import com.cgi.eoss.osiris.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata.OutputProductMetadataBuilder;
import com.cgi.eoss.osiris.model.internal.ParameterRelationTypeToFileRelationTypeUtil;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFilesRelationDataService;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.FileStream;
import com.cgi.eoss.osiris.rpc.FileStreamClient;
import com.cgi.eoss.osiris.rpc.OsirisServiceResponse;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.JobParam;
import com.cgi.eoss.osiris.rpc.worker.ContainerExit;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc;
import com.cgi.eoss.osiris.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.osiris.rpc.worker.JobEnvironment;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.rpc.worker.JobEvent;
import com.cgi.eoss.osiris.rpc.worker.JobEventType;
import com.cgi.eoss.osiris.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.osiris.rpc.worker.OutputFileItem;
import com.cgi.eoss.osiris.rpc.worker.OutputFileList;
import com.cgi.eoss.osiris.rpc.worker.PortBinding;
import com.cgi.eoss.osiris.rpc.worker.OsirisWorkerGrpc.OsirisWorkerBlockingStub;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.mysema.commons.lang.Pair;

import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class OsirisJobUpdatesManager {
	
	private final JobDataService jobDataService;
    private final DynamicProxyService dynamicProxyService;
    private final OsirisGuiServiceManager guiService;
    private final CachingWorkerFactory workerFactory;
    private final CatalogueService catalogueService;
    private final OsirisSecurityService securityService;
    private final OsirisFilesRelationDataService fileRelationDataService;
    
	 @Autowired
	    public OsirisJobUpdatesManager(JobDataService jobDataService, 
	    		DynamicProxyService dynamicProxyService, 
	    		OsirisGuiServiceManager guiService, 
	    		CachingWorkerFactory workerFactory,
	    		CatalogueService catalogueService,
	    		OsirisSecurityService securityService,
	    		OsirisFilesRelationDataService fileRelationDataService) {
	        this.jobDataService = jobDataService;
	        this.dynamicProxyService = dynamicProxyService;
	        this.guiService = guiService;
	        this.workerFactory = workerFactory;
	        this.catalogueService = catalogueService;
	        this.securityService = securityService;
	        this.fileRelationDataService = fileRelationDataService;
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
        Multimap<String, OsirisFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker,
                job.getConfig().getInputs(), jobEnvironment, outputsByRelativePath);
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
        else {
            StreamObserver<OsirisServiceResponse> responseObserver;
            List<JobParam> outputs = outputFiles.entries().stream().map(e -> JobParam.newBuilder().setParamName(e.getKey()).addParamValue(e.getValue().getUri().toASCIIString()).build()).collect(toList());
        }
     }

    private void completeParentJob(Job parentJob) {
        StreamObserver<OsirisServiceResponse> responseObserver;
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
        List<JobParam> outputs = jobOutputFiles.asMap().entrySet().stream()
                .map(e -> JobParam.newBuilder().setParamName(e.getKey())
                .addAllParamValue(
                e.getValue().stream().map(f -> f.getUri().toASCIIString()).collect(toSet()))
                .build())
                .collect(toList());
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
        return !parentJob.getSubJobs().stream().anyMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
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
            // Ensure we have one file per expected output
            Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs()
                    .stream().map(OsirisServiceDescriptor.Parameter::getId).collect(toSet());
            outputsByRelativePath = ArrayListMultimap.create();
            
            for (String expectedOutputId : expectedServiceOutputIds) {
                List<String> relativePathValues = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutputId + "/"))
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
            OsirisWorkerGrpc.OsirisWorkerBlockingStub worker, Multimap<String, String> inputs, JobEnvironment jobEnvironment,
            Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        List<RetrievedOutputFile> retrievedOutputFiles = new ArrayList<RetrievedOutputFile>(outputsByRelativePath.size());

        Multimap<String, OsirisFile> outputFiles = ArrayListMultimap.create();
        Map<String, GeoServerSpec> geoServerSpecs = getGeoServerSpecs(inputs);
        Map<String, String> collectionSpecs = getCollectionSpecs(inputs);

        for (String outputId : outputsByRelativePath.keySet()) {
            OutputProductMetadata outputProduct = getOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);

            for (String relativePath : outputsByRelativePath.get(outputId)) {
                GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString()).build();

                OsirisWorkerGrpc.OsirisWorkerStub asyncWorker = OsirisWorkerGrpc.newStub(worker.getChannel());

                try (FileStreamClient<GetOutputFileParam> fileStreamClient = new FileStreamClient<GetOutputFileParam>() {
                    private OutputFileMetadata outputFileMetadata;

                    @Override
                    public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
                        LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(),
                                fileMeta.getSize());

                        OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();

                        outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProduct)
                                .build();

                        setOutputPath(catalogueService.provisionNewOutputProduct(outputProduct, relativePath.toString()));
                        LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
                        return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
                    }

                    @Override
                    public void onCompleted() {
                        super.onCompleted();
                        Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getServiceOutputParameter(job.getConfig().getService(), outputId).map(this::extractStartEndDateTimes)
                                        .orElseGet(() -> new Pair<>(null, null));
                        outputFileMetadata.setStartDateTime(startEndDateTimes.getFirst());
                        outputFileMetadata.setEndDateTime(startEndDateTimes.getSecond());
                        retrievedOutputFiles.add(new RetrievedOutputFile(outputFileMetadata, getOutputPath()));
                    }

					private Pair<OffsetDateTime, OffsetDateTime> extractStartEndDateTimes(Parameter outputParameter) {
						try {
                            String regexp = outputParameter.getTimeRegexp();
	                        if (regexp != null) {
	                        	Pattern p = Pattern.compile(regexp);
	                        	Matcher m = p.matcher(getOutputPath().getFileName().toString());
	                        	if (m.find()) {
	                        		if (regexp.contains("?<startEnd>")) {
	                        			OffsetDateTime startEndDateTime = parseOffsetDateTime(m.group("startEnd"), LocalTime.MIDNIGHT);
	                        			return new Pair<OffsetDateTime, OffsetDateTime>(startEndDateTime, startEndDateTime);
	                        		}
	                        		else {
	                        			OffsetDateTime start = null, end = null;
	                        			if (regexp.contains("?<start>")) {
	                            			start = parseOffsetDateTime(m.group("start"), LocalTime.MIDNIGHT);
	                            		}
	                        			
	                        			if (regexp.contains("?<end>")) {
	                            			end = parseOffsetDateTime(m.group("end"), LocalTime.MIDNIGHT);
	                            		}
	                        			return new Pair<OffsetDateTime, OffsetDateTime>(start, end);
	                        		}
	                            }
	                        }
                        } catch(RuntimeException e) {
                        	LOG.error("Unable to parse date from regexp");
                        }
						return new Pair<OffsetDateTime, OffsetDateTime> (null, null);
					}
					
					private OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX]]");
						TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
						if (temporalAccessor instanceof OffsetDateTime) {
							return (OffsetDateTime) temporalAccessor;
						} 
						else if (temporalAccessor instanceof LocalDateTime){
							return ((LocalDateTime) temporalAccessor).atOffset(ZoneOffset.UTC);
						} 
						else
						{
							return ((LocalDate) temporalAccessor).atTime(defaultTime).atOffset(ZoneOffset.UTC);
						}
					}
                }) {
                    asyncWorker.getOutputFile(getOutputFileParam, fileStreamClient.getFileStreamObserver());
                    fileStreamClient.getLatch().await();
                }
            }
        }
        
        postProcessOutputProducts(retrievedOutputFiles).forEach( Unchecked.consumer(retrievedOutputFile -> outputFiles.put(retrievedOutputFile.getOutputFileMetadata().getOutputProductMetadata().getOutputId(), catalogueService.ingestOutputProduct(retrievedOutputFile.getOutputFileMetadata(), retrievedOutputFile.getPath()))));
        buildOutputFileRelations(outputFiles, job.getConfig().getService());
        
        return outputFiles;
    }
    
    private Optional<Parameter> getServiceOutputParameter(OsirisService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId)).findFirst();
	}
    

    private void buildOutputFileRelations(Multimap<String, OsirisFile> outputFiles, OsirisService service) {
		for ( Parameter output: service.getServiceDescriptor().getDataOutputs()) {
			if (output.getParameterRelations() != null){
				for (Relation parameterRelation: output.getParameterRelations()) {
					Collection<OsirisFile> filesForSourceOutput = outputFiles.get(output.getId());
					Collection<OsirisFile> filesForTargetOutput = outputFiles.get(parameterRelation.getTargetParameterId());
					for (OsirisFile sourceFile: filesForSourceOutput) {
						for (OsirisFile targetFile: filesForTargetOutput) {
							Type relationType = ParameterRelationTypeToFileRelationTypeUtil.fromParameterRelationType(parameterRelation.getType());
							OsirisFilesRelation relation = new OsirisFilesRelation(sourceFile, targetFile, relationType);
							fileRelationDataService.save(relation);
						}
					}
				}
			}
		}
	}

	private OutputProductMetadata getOutputMetadata(Job job, Map<String, GeoServerSpec> geoServerSpecs,
            Map<String, String> collectionSpecs, String outputId) {
        OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
                .owner(job.getOwner())
                .service(job.getConfig().getService())
                .outputId(outputId)
                .jobId(job.getExtId());
                
        
        HashMap<String, Object> properties = new HashMap<>(ImmutableMap.<String, Object>builder()
                .put("jobId", job.getExtId()).put("intJobId", job.getId())
                .put("serviceName", job.getConfig().getService().getName())
                .put("jobOwner", job.getOwner().getName())
                .put("jobStartTime",
                        job.getStartTime().atOffset(ZoneOffset.UTC).toString())
                .put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString())
                .build());
        
        GeoServerSpec geoServerSpecForOutput = geoServerSpecs.get(outputId);
        if (geoServerSpecForOutput != null) {
            properties.put("geoServerSpec", geoServerSpecForOutput);
        }
        
        String collectionSpecForOutput = collectionSpecs.get(outputId);
        if (collectionSpecForOutput != null) {
            properties.put("collection", collectionSpecForOutput);
        }
        
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("outputId", outputId);
        
        getServiceOutputParameter(job.getConfig().getService(), outputId).ifPresent(p -> addPlatformMetadata(extraParams, p));
        properties.put("extraParams", extraParams);
        
        OutputProductMetadata outputProduct = outputProductMetadataBuilder.productProperties(properties).build();
        return outputProduct;
    }
	
	private void addPlatformMetadata(Map<String, String> extraParams, Parameter outputParameter) {
	    if (outputParameter.getPlatformMetadata() != null && outputParameter.getPlatformMetadata().size() > 0 ) {
	        extraParams.putAll(outputParameter.getPlatformMetadata());
        }
	}

        
    private List<RetrievedOutputFile> postProcessOutputProducts(List<RetrievedOutputFile> retrievedOutputFiles) throws IOException {
        // Try to read CRS/AOI from all files - note that CRS/AOI may still be null after this
        retrievedOutputFiles.forEach(retrievedOutputFile -> {
            retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
            retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
        });

        return retrievedOutputFiles;
    }

    
    private Map<String, GeoServerSpec> getGeoServerSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String geoServerSpecsStr = Iterables.getOnlyElement(inputs.get("geoServerSpec"), null);
        Map<String, GeoServerSpec> geoServerSpecs = new HashMap<String, GeoServerSpec>();
        if (geoServerSpecsStr != null && geoServerSpecsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, GeoServerSpec.class);
                geoServerSpecs.putAll(mapper.readValue(geoServerSpecsStr, mapType));
        }
        return geoServerSpecs;
    }
    
    private Map<String, String> getCollectionSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String collectionsStr = Iterables.getOnlyElement(inputs.get("collection"), null);
        Map<String, String> collectionSpecs = new HashMap<String, String>();
        if (collectionsStr != null && collectionsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
                collectionSpecs.putAll(mapper.readValue(collectionsStr, mapType));
        }
        return collectionSpecs;
    }

    private String getOutputCrs(Path outputPath) {
        try {
            return GeoUtil.extractEpsg(outputPath);
        } catch (Exception e) {
            return null;
        }
    }

    private String getOutputGeometry(Path outputPath) {
        try {
            return GeoUtil.geojsonToWkt(GeoUtil.extractBoundingBox(outputPath));
        } catch (Exception e) {
            return null;
        }
    }

}
