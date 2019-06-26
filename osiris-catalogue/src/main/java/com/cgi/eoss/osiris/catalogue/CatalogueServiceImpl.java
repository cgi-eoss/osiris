package com.cgi.eoss.osiris.catalogue;

import com.cgi.eoss.osiris.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.osiris.catalogue.files.OutputProductService;
import com.cgi.eoss.osiris.catalogue.files.ReferenceDataService;
import com.cgi.eoss.osiris.logging.Logging;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.DataSource;
import com.cgi.eoss.osiris.model.Databasket;
import com.cgi.eoss.osiris.model.GeoserverLayer;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFile.Type;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.internal.OutputFileMetadata;
import com.cgi.eoss.osiris.model.internal.OutputProductMetadata;
import com.cgi.eoss.osiris.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.osiris.persistence.service.CollectionDataService;
import com.cgi.eoss.osiris.persistence.service.DataSourceDataService;
import com.cgi.eoss.osiris.persistence.service.DatabasketDataService;
import com.cgi.eoss.osiris.persistence.service.GeoserverLayerDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFileDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFilesRelationDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.cgi.eoss.osiris.rpc.FileStream;
import com.cgi.eoss.osiris.rpc.FileStreamIOException;
import com.cgi.eoss.osiris.rpc.FileStreamServer;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.osiris.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.osiris.rpc.catalogue.OsirisFileUri;
import com.cgi.eoss.osiris.rpc.catalogue.UriDataSourcePolicies;
import com.cgi.eoss.osiris.rpc.catalogue.UriDataSourcePolicy;
import com.cgi.eoss.osiris.rpc.catalogue.Uris;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.base.Stopwatch;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.geojson.GeoJsonObject;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@GRpcService
@Log4j2
public class CatalogueServiceImpl extends CatalogueServiceGrpc.CatalogueServiceImplBase implements CatalogueService {
    
    private final OsirisFileDataService osirisFileDataService;
    private final CollectionDataService collectionDataService;
    private final DataSourceDataService dataSourceDataService;
    private final DatabasketDataService databasketDataService;
    private final OutputProductService outputProductService;
    private final ReferenceDataService referenceDataService;
    private final ExternalProductDataService externalProductDataService;
    private final OsirisSecurityService securityService;
    private final UserDataService userDataService;
    private final OsirisFilesRelationDataService osirisFilesRelationDataService;
    private final GeoserverLayerDataService geoserverLayerDataService;
    
    @Autowired
    public CatalogueServiceImpl(OsirisFileDataService osirisFileDataService, CollectionDataService collectionDataService, DataSourceDataService dataSourceDataService, DatabasketDataService databasketDataService, OutputProductService outputProductService, ReferenceDataService referenceDataService, ExternalProductDataService externalProductDataService, OsirisSecurityService securityService, UserDataService userDataService, OsirisFilesRelationDataService osirisFilesRelationDataService, GeoserverLayerDataService geoserverLayerDataService) {
        this.osirisFileDataService = osirisFileDataService;
        this.collectionDataService = collectionDataService;
        this.dataSourceDataService = dataSourceDataService;
        this.databasketDataService = databasketDataService;
        this.outputProductService = outputProductService;
        this.referenceDataService = referenceDataService;
        this.externalProductDataService = externalProductDataService;
        this.securityService = securityService;
        this.userDataService = userDataService;
        this.osirisFilesRelationDataService = osirisFilesRelationDataService;
        this.geoserverLayerDataService = geoserverLayerDataService;
    }

    @Override
    public OsirisFile ingestReferenceData(ReferenceDataMetadata referenceDataMetadata, MultipartFile file) throws IOException {
    	String collection = (String) referenceDataMetadata.getUserProperties().get("collection");
        if (collection == null) {
            collection = getDefaultReferenceDataCollection();
        }
        ensureReferenceDataCollectionExists(collection);
        OsirisFile osirisFile = referenceDataService.ingest(collection, referenceDataMetadata.getOwner(), referenceDataMetadata.getFilename(), referenceDataMetadata.getFiletype(), referenceDataMetadata.getUserProperties(), file);
        osirisFile.setDataSource(dataSourceDataService.getForRefData(osirisFile));
        osirisFile.setCollection(collectionDataService.getByIdentifier(collection));
        return osirisFileDataService.syncGeoserverLayersAndSave(osirisFile);
    }

    @Override
    public Path provisionNewOutputProduct(OutputProductMetadata outputProduct, String filename) throws IOException {
        return outputProductService.provision(outputProduct.getJobId(), filename);
    }
    
    @Override
    public String getDefaultOutputProductCollection() {
        return outputProductService.getDefaultCollection();
    }
    
    @Override
    public String getDefaultReferenceDataCollection() {
        return referenceDataService.getDefaultCollection();
    }
    
    @Override
    public OsirisFile ingestOutputProduct(OutputFileMetadata outputFileMetadata, Path path) throws IOException {
        OutputProductMetadata outputProductMetadata = outputFileMetadata.getOutputProductMetadata();
        String collection = (String) outputProductMetadata.getProductProperties().get("collection");
        if (collection == null) {
            collection = getDefaultOutputProductCollection();
        }
        ensureOutputCollectionExists(collection);
        OsirisFile osirisFile = outputProductService.ingest(
                collection,
                outputProductMetadata.getOwner(),
                outputProductMetadata.getJobId(),
                outputFileMetadata.getCrs(),
                outputFileMetadata.getGeometry(),
                outputFileMetadata.getStartDateTime(),
                outputFileMetadata.getEndDateTime(),
                outputProductMetadata.getProductProperties(),
                path);
        osirisFile.setDataSource(dataSourceDataService.getForService(outputProductMetadata.getService()));
        osirisFile.setCollection(collectionDataService.getByIdentifier(collection));
        return osirisFileDataService.syncGeoserverLayersAndSave(osirisFile);
    }
    
    private void ensureOutputCollectionExists(String collectionIdentifier) {
        Collection collection = collectionDataService.getByIdentifier(collectionIdentifier);
        if (collection == null) {
            createOutputCollection(collectionIdentifier);
        }
    }
    
    private void ensureReferenceDataCollectionExists(String collectionIdentifier) {
        Collection collection = collectionDataService.getByIdentifier(collectionIdentifier);
        if (collection == null) {
            createReferenceDataCollection(collectionIdentifier);
        }
    }

    private void createOutputCollection(String collectionIdentifier) {
       if (collectionIdentifier.equals(getDefaultOutputProductCollection())) {
           Collection collection = new Collection(getDefaultOutputProductCollection(), userDataService.getDefaultUser());
           collection.setDescription("Output Products");
           collection.setProductsType("Misc");
           collection.setFileType(Type.OUTPUT_PRODUCT);
           collection.setIdentifier(getDefaultOutputProductCollection());
           collectionDataService.save(collection);
           securityService.publish(Collection.class, collection.getId());
       }
    }
    
    private void createReferenceDataCollection(String collectionIdentifier) {
        if (collectionIdentifier.equals(getDefaultReferenceDataCollection())) {
            Collection collection = new Collection(getDefaultReferenceDataCollection(), userDataService.getDefaultUser());
            collection.setDescription("Reference Data");
            collection.setProductsType("Misc");
            collection.setFileType(Type.REFERENCE_DATA);
            collection.setIdentifier(getDefaultReferenceDataCollection());
            collectionDataService.save(collection);
            securityService.publish(Collection.class, collection.getId());
        }
     }

    @Override
    public OsirisFile indexExternalProduct(GeoJsonObject geoJson) {
        // This will return an already-persistent object
        OsirisFile osirisFile = externalProductDataService.ingest(geoJson);
        osirisFile.setDataSource(dataSourceDataService.getForExternalProduct(osirisFile));
        return osirisFile;
    }

    @Override
    public Resource getAsResource(OsirisFile file) {
        switch (file.getType()) {
            case REFERENCE_DATA:
                return referenceDataService.resolve(file);
            case OUTPUT_PRODUCT:
                return outputProductService.resolve(file);
            case EXTERNAL_PRODUCT:
                return externalProductDataService.resolve(file);
            default:
                throw new UnsupportedOperationException("Unable to materialise OsirisFile: " + file);
        }
    }

    @Override
    public void delete(OsirisFile file) throws IOException {
        switch (file.getType()) {
            case REFERENCE_DATA:
                referenceDataService.delete(file);
                break;
            case OUTPUT_PRODUCT:
                outputProductService.delete(file);
                break;
            case EXTERNAL_PRODUCT:
                externalProductDataService.delete(file);
                break;
        }
        osirisFileDataService.delete(file);
        for (GeoserverLayer geoserverLayer: file.getGeoserverLayers()) {
        	evaluateLayerDeletion(geoserverLayer);
        }
    }
    
    private void evaluateLayerDeletion(GeoserverLayer geoserverLayer) {
    	//Only single geotiff layers can be deleted - mosaics are shared and postgis is not fully manage by geoserver
    	switch (geoserverLayer.getStoreType()) {
			case GEOTIFF: 
				geoserverLayerDataService.delete(geoserverLayer);
				return;
			case MOSAIC:
			case POSTGIS:
			default:
				return;
		}
	}

    @Override
    public Set<Link> getOGCLinks(OsirisFile osirisFile) {
    	Set<Link> links = new HashSet<>();
        switch (osirisFile.getType()) {
            case OUTPUT_PRODUCT:
            	links.addAll(outputProductService.getOGCLinks(osirisFileDataService.refreshFull(osirisFile)));
            	for (OsirisFilesRelation relation: osirisFilesRelationDataService.findByTargetFileAndType(osirisFile, OsirisFilesRelation.Type.VISUALIZATION_OF)) {
            		links.addAll(getOGCLinks(relation.getSourceFile()));
            	}
            	break;
            case REFERENCE_DATA:
            	links.addAll(referenceDataService.getOGCLinks(osirisFileDataService.refreshFull(osirisFile)));
            	for (OsirisFilesRelation relation: osirisFilesRelationDataService.findByTargetFileAndType(osirisFile, OsirisFilesRelation.Type.VISUALIZATION_OF)) {
            		links.addAll(getOGCLinks(relation.getSourceFile()));
            	}
            	break;
            default: break;
        }
        return links;
    }

    @Override
    public boolean canUserRead(User user, URI uri) {
        if (uri.getScheme().equals("osiris") && uri.getHost().equals("databasket")) {
            Databasket databasket = getDatabasketFromUri(uri.toASCIIString());

            if (!securityService.hasUserPermission(user, OsirisPermission.READ, Databasket.class, databasket.getId())) {
                logAccessFailure(uri);
                return false;
            }

            return databasket.getFiles().stream().allMatch(osirisFile -> canUserRead(user, osirisFile.getUri()));
        } else {
            OsirisFile osirisFile = osirisFileDataService.getByUri(uri);

            if (osirisFile != null) {
                if (!securityService.hasUserPermission(user, OsirisPermission.READ, OsirisFile.class, osirisFile.getId())) {
                    logAccessFailure(osirisFile.getUri());
                    return false;
                }
            }

            return true;
        }
    }
    
    @Override
    public boolean canUserWrite(User user, String collectionIdentifier) {
        Collection collection = collectionDataService.getByIdentifier(collectionIdentifier);
        return securityService.hasUserPermission(user, OsirisPermission.WRITE, Collection.class, collection.getId());
    }

    private void logAccessFailure(URI uri) {
        try (CloseableThreadContext.Instance ctc = Logging.userLoggingContext()) {
            LOG.info("Access denied to OSIRIS resource: {}", uri);
        }
    }

    @Override
    public void downloadOsirisFile(OsirisFileUri request, StreamObserver<FileStream> responseObserver) {
        OsirisFile file = osirisFileDataService.getByUri(request.getUri());
        Resource fileResource = getAsResource(file);

        try (FileStreamServer fileStreamServer = new FileStreamServer(null, responseObserver) {
            @Override
            protected FileStream.FileMeta buildFileMeta() {
                try {
                    return FileStream.FileMeta.newBuilder()
                            .setFilename(fileResource.getFilename())
                            .setSize(fileResource.contentLength())
                            .build();
                } catch (IOException e) {
                    throw new FileStreamIOException(e);
                }
            }

            @Override
            protected ReadableByteChannel buildByteChannel() {
                try {
                    return Channels.newChannel(fileResource.getInputStream());
                } catch (IOException e) {
                    throw new FileStreamIOException(e);
                }
            }
        }) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            fileStreamServer.streamFile();
            LOG.info("Transferred OsirisFile {} ({} bytes) in {}", fileResource.getFilename(), fileResource.contentLength(), stopwatch.stop().elapsed());
        } catch (IOException e) {
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        } catch (InterruptedException e) {
            // Restore interrupted state
            Thread.currentThread().interrupt();
            LOG.error("Failed to serve file download for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }

    }

    @Override
    public void getDatabasketContents(com.cgi.eoss.osiris.rpc.catalogue.Databasket request, StreamObserver<DatabasketContents> responseObserver) {
        try {
            // TODO Extract databasket ID from CatalogueUri pattern
            Databasket databasket = getDatabasketFromUri(request.getUri());

            DatabasketContents.Builder responseBuilder = DatabasketContents.newBuilder();
            databasket.getFiles().forEach(f -> responseBuilder.addFiles(
                    com.cgi.eoss.osiris.rpc.catalogue.OsirisFile.newBuilder()
                            .setFilename(f.getFilename())
                            .setUri(OsirisFileUri.newBuilder().setUri(f.getUri().toASCIIString()).build())
                            .build()
                    )
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list databasket contents for {}", request.getUri(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getDataSourcePolicies(Uris request, StreamObserver<UriDataSourcePolicies> responseObserver) {
        try {
            UriDataSourcePolicies.Builder responseBuilder = UriDataSourcePolicies.newBuilder();

            for (OsirisFileUri fileUri : request.getFileUrisList()) {
                OsirisFile osirisFile = osirisFileDataService.getByUri(fileUri.getUri());
                DataSource dataSource;

                if (osirisFile != null) {
                    dataSource = osirisFile.getDataSource() != null ? osirisFile.getDataSource() :
                            dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                } else {
                    dataSource = dataSourceDataService.getByName(URI.create(fileUri.getUri()).getScheme());
                }

                LOG.debug("Inferred DataSource {} from OsirisFile: {}", dataSource, fileUri.getUri());

                // Default to CACHE mode
                DataSource.Policy policy = dataSource != null ? dataSource.getPolicy() : DataSource.Policy.CACHE;

                responseBuilder.addPolicies(UriDataSourcePolicy.newBuilder()
                        .setUri(fileUri)
                        .setPolicy(UriDataSourcePolicy.Policy.valueOf(policy.toString()))
                        .build());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list datasource access policies contents for {}", request.getFileUrisList(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new CatalogueException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId)).orElseThrow(() -> new CatalogueException("Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

    @Override
    public void createCollection(Collection collection) throws IOException {
    	switch(collection.getFileType()) {
    		case OUTPUT_PRODUCT: 
    			outputProductService.createCollection(collection);
    			return;
    		case REFERENCE_DATA:
    			referenceDataService.createCollection(collection);
    			return;
    		case EXTERNAL_PRODUCT:
    		default:
    			return;
    	}
    }
    
    @Override
    public void deleteCollection(Collection collection) throws IOException {
    	collection = collectionDataService.refreshFull(collection);
    	switch(collection.getFileType()) {
    		case OUTPUT_PRODUCT: 
    			outputProductService.deleteCollection(collection);
    			return;
    		case REFERENCE_DATA:
    			referenceDataService.deleteCollection(collection);
    			return;
    		case EXTERNAL_PRODUCT:
    		default:
    			return;
    	}
    }
}
