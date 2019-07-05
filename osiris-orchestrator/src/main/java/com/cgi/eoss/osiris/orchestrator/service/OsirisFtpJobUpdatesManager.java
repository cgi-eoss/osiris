package com.cgi.eoss.osiris.orchestrator.service;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toSet;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.osiris.catalogue.util.GeoUtil;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobStep;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.OsirisFilesRelation.Type;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Parameter;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Relation;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata.OutputProductMetadataBuilder;
import com.cgi.eoss.osiris.model.internal.ParameterRelationTypeToFileRelationTypeUtil;
import com.cgi.eoss.osiris.model.internal.RetrievedOutputFile;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFilesRelationDataService;
import com.cgi.eoss.osiris.rpc.FileStream;
import com.cgi.eoss.osiris.rpc.FileStreamClient;
import com.cgi.eoss.osiris.rpc.LocalFtpHarvester;
import com.cgi.eoss.osiris.rpc.ftp.harvester.GetFileParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mysema.commons.lang.Pair;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Log4j2
public class OsirisFtpJobUpdatesManager {

	private final JobDataService jobDataService;
	private final CatalogueService catalogueService;
	private final OsirisFilesRelationDataService fileRelationDataService;
	private LocalFtpHarvester localFtpHarvester;
 
	@Autowired
	public OsirisFtpJobUpdatesManager(JobDataService jobDataService, CatalogueService catalogueService, OsirisFilesRelationDataService fileRelationDataService, LocalFtpHarvester localFtpHarvester) {
		this.jobDataService = jobDataService;
		this.catalogueService = catalogueService;
		this.fileRelationDataService = fileRelationDataService;
		this.localFtpHarvester = localFtpHarvester;
	}
	
	public void onJobStarted(Job job) {
		job.setStatus(Job.Status.RUNNING);
		job.setStage(JobStep.PROCESSING.getText());
		jobDataService.save(job);
		
	}

	public void onJobStopped(Job job) {
		job.setStatus(Job.Status.COMPLETED);
		job.setStage(JobStep.OUTPUT_LIST.getText());
		jobDataService.save(job);
		
	}

	public void onJobFtpFileAvailable(Job job, String ftpRoot, URI fileUri) throws IOException, InterruptedException {
		ingestFile(job, ftpRoot, fileUri);
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
		String outputId = getOutputId(job, ftpRoot, fileUri);
		// Repatriate output file
		OsirisFile osirisFile = repatriateAndIngestOutputFile(job, job.getConfig().getInputs(), outputId, fileUri);
		if(osirisFile == null) {
			throw new IOException("ingestion failed");
		}
		if (job.getOutputs() == null) {
			job.setOutputs(MultimapBuilder.hashKeys().hashSetValues().build());
		}
		job.getOutputs().put(outputId, osirisFile.getUri().toString());
		job.getOutputFiles().add(osirisFile);
		jobDataService.save(job);
	}

	private String getOutputId(Job job, String ftpRoot, URI fileUri) {
		Path path = Paths.get(fileUri.getPath());
		Path relativePath = Paths.get(ftpRoot).relativize(path);
		OsirisService service = job.getConfig().getService();
		Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs().stream()
				.map(OsirisServiceDescriptor.Parameter::getId).collect(toSet());
		Path pathRoot = relativePath.getParent();
		if (expectedServiceOutputIds.contains(pathRoot.toString())) {
			return pathRoot.toString();
		}
		return "1";
	}

	
	private OsirisFile repatriateAndIngestOutputFile(Job job, Multimap<String, String> inputs, String outputId,
			URI fileUri) throws IOException, InterruptedException {
		Map<String, GeoServerSpec> geoServerSpecs = getGeoServerSpecs(inputs);
		Map<String, String> collectionSpecs = getCollectionSpecs(inputs);
		OutputProductMetadata outputProduct = getOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);
		List<RetrievedOutputFile> retrievedOutputFiles = new ArrayList<>(1);
		Multimap<String, OsirisFile> outputFiles = ArrayListMultimap.create();
		// Retrieve the ftp file
		GetFileParams getFileParams = GetFileParams.newBuilder().setFileUri(fileUri.toString()).build();
		try (FileStreamClient<GetFileParams> fileStreamClient = new FileStreamClient<GetFileParams>() {
			private OutputFileMetadata outputFileMetadata;

			@Override
			public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
				LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(),
						fileMeta.getSize());

				OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();

				outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProduct).build();

				setOutputPath(catalogueService.provisionNewOutputProduct(outputProduct, getFileName(outputId, fileUri)));
				LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
				return new BufferedOutputStream(
						Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
			}

			private String getFileName(String outputId, URI fileUri) {
				return Paths.get(outputId).resolve(Paths.get(fileUri.getPath()).getFileName()).toString();
			}

			@Override
			public void onCompleted() {
				super.onCompleted();
				Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getServiceOutputParameter(
						job.getConfig().getService(), outputId).map(this::extractStartEndDateTimes)
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
								OffsetDateTime startEndDateTime = parseOffsetDateTime(m.group("startEnd"),
										LocalTime.MIDNIGHT);
								return new Pair<>(startEndDateTime, startEndDateTime);
							} else {
								OffsetDateTime start = null;
								OffsetDateTime end = null;
								if (regexp.contains("?<start>")) {
									start = parseOffsetDateTime(m.group("start"), LocalTime.MIDNIGHT);
								}

								if (regexp.contains("?<end>")) {
									end = parseOffsetDateTime(m.group("end"), LocalTime.MIDNIGHT);
								}
								return new Pair<>(start, end);
							}
						}
					}
				} catch (RuntimeException e) {
					LOG.error("Unable to parse date from regexp");
				}
				return new Pair<>(null, null);
			}

			private OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX]]");
				TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from,
						LocalDateTime::from, LocalDate::from);
				if (temporalAccessor instanceof OffsetDateTime) {
					return (OffsetDateTime) temporalAccessor;
				} else if (temporalAccessor instanceof LocalDateTime) {
					return ((LocalDateTime) temporalAccessor).atOffset(ZoneOffset.UTC);
				} else {
					return ((LocalDate) temporalAccessor).atTime(defaultTime).atOffset(ZoneOffset.UTC);
				}
			}
		}) {
			LOG.info("Fetching file {}", getFileParams.getFileUri());
			localFtpHarvester.asyncGetFile(getFileParams, fileStreamClient.getFileStreamObserver());
			fileStreamClient.getLatch().await();
		}
		postProcessOutputProducts(retrievedOutputFiles).forEach(Unchecked.consumer(retrievedOutputFile -> outputFiles
				.put(retrievedOutputFile.getOutputFileMetadata().getOutputProductMetadata().getOutputId(),
						catalogueService.ingestOutputProduct(retrievedOutputFile.getOutputFileMetadata(),
								retrievedOutputFile.getPath()))));
		processOutputFileRelations(outputFiles, job);

		Optional<Entry<String, OsirisFile>> entry = outputFiles.entries().stream().findFirst();
		if (entry.isPresent()) {
			return entry.get().getValue();
		}		
		return null;
	}

	private Optional<Parameter> getServiceOutputParameter(OsirisService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId))
				.findFirst();
	}

	private void processOutputFileRelations(Multimap<String, OsirisFile> outputFiles, Job job) {
		for (Entry<String, OsirisFile> entry : outputFiles.entries()) {
			processRelationsAsSource(entry.getKey(), entry.getValue(), job);
			processRelationsAsTarget(entry.getKey(), entry.getValue(), job);
		}
	}

	private void processRelationsAsSource(String outputId, OsirisFile sourceFile, Job job) {
		Parameter outputParameter = getOutputParameter(job.getConfig().getService(), outputId);
		if (outputParameter != null && outputParameter.getParameterRelations() != null) {
			for (Relation parameterRelation : outputParameter.getParameterRelations()) {
				Collection<OsirisFile> targetFiles = findJobOutputFilesById(
						job, parameterRelation.getTargetParameterId());
				for (OsirisFile targetFile : targetFiles) {
					Type relationType = ParameterRelationTypeToFileRelationTypeUtil
							.fromParameterRelationType(parameterRelation.getType());
					OsirisFilesRelation relation = new OsirisFilesRelation(sourceFile, targetFile, relationType);
					fileRelationDataService.save(relation);
				}
			}
		}
	}
	
	private void processRelationsAsTarget(String outputId, OsirisFile targetFile, Job job) {
		List<ImmutablePair<Parameter, Relation>> sourceParameterAndRelations = getSourceParametersAndRelations(job.getConfig().getService(), outputId);
		for (ImmutablePair<Parameter, Relation> sourceParameterAndRelation: sourceParameterAndRelations) {
			Parameter sourceParameter = sourceParameterAndRelation.getLeft();
			Relation parameterRelation = sourceParameterAndRelation.getRight();
			Collection<OsirisFile> sourceFiles = findJobOutputFilesById(
					job, sourceParameter.getId());
			for (OsirisFile sourceFile: sourceFiles) {
				Type relationType = ParameterRelationTypeToFileRelationTypeUtil
						.fromParameterRelationType(parameterRelation.getType());
				OsirisFilesRelation relation = new OsirisFilesRelation(sourceFile, targetFile, relationType);
				fileRelationDataService.save(relation);
			}
			
		}
	}

	private List<OsirisFile> findJobOutputFilesById(Job job, String outputId) {
		return job.getOutputs().get(outputId).stream()
				.map(output-> getFileByUri(job, output)).filter(Optional::isPresent)
				  .map(Optional::get)
				  .collect(Collectors.toList());
	}
	
	private Optional<OsirisFile> getFileByUri(Job job, String output){
		return job.getOutputFiles().stream().filter(f -> f.getUri().toString().equals(output)).findFirst();
	}

	private Parameter getOutputParameter(OsirisService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId))
				.findFirst().orElse(null);
	}
	
	private List<ImmutablePair<Parameter, Relation>> getSourceParametersAndRelations(OsirisService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().flatMap(p -> getRelationsWithOutput(p, outputId).stream().map(r -> ImmutablePair.of(p,  r))).collect(Collectors.toList());
	}
	
	private List<Relation> getRelationsWithOutput(Parameter p, String outputId) {
		List<Relation> parameterRelations = p.getParameterRelations();
		if (parameterRelations == null || parameterRelations.isEmpty()) {
			return Collections.emptyList();
		}
		return parameterRelations.stream().filter(r-> r.getTargetParameterId().equals(outputId)).collect(Collectors.toList());
	}

	private OutputProductMetadata getOutputMetadata(Job job, Map<String, GeoServerSpec> geoServerSpecs,
			Map<String, String> collectionSpecs, String outputId) {
		OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
				.owner(job.getOwner()).service(job.getConfig().getService()).outputId(outputId).jobId(job.getExtId());
		Builder<String, Object> propertiesBuilder = ImmutableMap.<String, Object>builder()
		.put("jobId", job.getExtId()).put("intJobId", job.getId())
		.put("serviceName", job.getConfig().getService().getName()).put("jobOwner", job.getOwner().getName());
		if (job.getStartTime() != null) {
			propertiesBuilder.put("jobStartTime", job.getStartTime().atOffset(ZoneOffset.UTC).toString());
		}
		if (job.getEndTime() != null) {
			propertiesBuilder.put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString());
		}
		HashMap<String, Object> properties = new HashMap<>(propertiesBuilder.build());

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

		getServiceOutputParameter(job.getConfig().getService(), outputId)
				.ifPresent(p -> addPlatformMetadata(extraParams, p));
		properties.put("extraParams", extraParams);

		return outputProductMetadataBuilder.productProperties(properties).build();
	}

	private void addPlatformMetadata(Map<String, String> extraParams, Parameter outputParameter) {
		if (outputParameter.getPlatformMetadata() != null && outputParameter.getPlatformMetadata().size() > 0) {
			extraParams.putAll(outputParameter.getPlatformMetadata());
		}
	}

	private List<RetrievedOutputFile> postProcessOutputProducts(List<RetrievedOutputFile> retrievedOutputFiles) {
		// Try to read CRS/AOI from all files - note that CRS/AOI may still be null
		// after this
		retrievedOutputFiles.forEach(retrievedOutputFile -> {
			retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
			retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
		});

		return retrievedOutputFiles;
	}

	private Map<String, GeoServerSpec> getGeoServerSpecs(Multimap<String, String> inputs)
			throws IOException {
		String geoServerSpecsStr = Iterables.getOnlyElement(inputs.get("geoServerSpec"), null);
		Map<String, GeoServerSpec> geoServerSpecs = new HashMap<>();
		if (geoServerSpecsStr != null && geoServerSpecsStr.length() > 0) {
			ObjectMapper mapper = new ObjectMapper();
			TypeFactory typeFactory = mapper.getTypeFactory();
			MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, GeoServerSpec.class);
			geoServerSpecs.putAll(mapper.readValue(geoServerSpecsStr, mapType));
		}
		return geoServerSpecs;
	}

	private Map<String, String> getCollectionSpecs(Multimap<String, String> inputs)
			throws IOException {
		String collectionsStr = Iterables.getOnlyElement(inputs.get("collection"), null);
		Map<String, String> collectionSpecs = new HashMap<>();
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
