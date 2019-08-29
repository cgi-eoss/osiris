package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.rpc.FileStream;
import com.cgi.eoss.osiris.rpc.FileStreamServer;
import com.cgi.eoss.osiris.rpc.wps.controller.DeleteWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.DeleteWpsOutputFileResponse;
import com.cgi.eoss.osiris.rpc.wps.controller.GetWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.ListWpsOutputFilesParam;
import com.cgi.eoss.osiris.rpc.wps.controller.OsirisWpsResultsManagerGrpc.OsirisWpsResultsManagerImplBase;
import com.cgi.eoss.osiris.rpc.wps.controller.WpsOutputFileItem;
import com.cgi.eoss.osiris.rpc.wps.controller.WpsOutputFileList;
import com.google.common.base.Stopwatch;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@GRpcService
@Log4j2
public class WpsResultsManagerGrpc extends OsirisWpsResultsManagerImplBase {

	private Path outputPath;

	@Autowired
    public WpsResultsManagerGrpc(@Value("${osiris.harvesters.wps.resultsPath:/home/acuomo/wpsOutputs/}") Path outputPath) {
       this.outputPath = outputPath;
    }

	
	@Override
	public void listOutputFiles(ListWpsOutputFilesParam request, StreamObserver<WpsOutputFileList> responseObserver) {
	    try {
            Path outputDir = outputPath.resolve(request.getJob().getId());
            LOG.debug("Listing outputs from job {} in path: {}", request.getJob().getId(), outputDir);

            WpsOutputFileList.Builder responseBuilder = WpsOutputFileList.newBuilder();

            try (Stream<Path> outputDirContents = Files.walk(outputDir, 3, FileVisitOption.FOLLOW_LINKS)) {
                outputDirContents.filter(Files::isRegularFile)
                        .map(Unchecked.function(outputDir::relativize))
                        .map(relativePath -> WpsOutputFileItem.newBuilder().setPath(relativePath.toString()).build())
                        .forEach(responseBuilder::addItems);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list output files: {}", request.toString(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }
	
	@Override
	public void getFile(GetWpsOutputFileParams getWpsFileParam, StreamObserver<FileStream> responseObserver) {
		Path outputDir = outputPath.resolve(getWpsFileParam.getJob().getId());
		serveWpsFile(outputDir.resolve(getWpsFileParam.getPath()), responseObserver);
	}
	
	@Override
	public void deleteFile(DeleteWpsOutputFileParams deleteWpsFileParams, StreamObserver<DeleteWpsOutputFileResponse> responseObserver) {
		Path outputDir = outputPath.resolve(deleteWpsFileParams.getJob().getId());
		try {
			Files.delete(outputDir.resolve(deleteWpsFileParams.getPath()));
			responseObserver.onNext(DeleteWpsOutputFileResponse.newBuilder().build());
			responseObserver.onCompleted();
		} catch (IOException e) {
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}

	private void serveWpsFile(Path filePath, StreamObserver<FileStream> responseObserver) {
		final File f = filePath.toFile();
		try (FileStreamServer fileStreamServer = new FileStreamServer(null, responseObserver) {
			@Override
			protected FileStream.FileMeta buildFileMeta() {
				return FileStream.FileMeta.newBuilder().setFilename(f.getName())
						.setSize(f.length()).build();
			}

			@Override
			protected ReadableByteChannel buildByteChannel() {
				try {
					return Channels.newChannel(new FileInputStream(f));
				} catch (IOException e) {
					throw new RuntimeException("Cannot open channel for file");
				}
			}
		}) {
			Stopwatch stopwatch = Stopwatch.createStarted();
			fileStreamServer.streamFile();
			LOG.info("Transferred output file {} ({} bytes) in {}", fileStreamServer.getFileMeta().getFilename(),
					fileStreamServer.getFileMeta().getSize(), stopwatch.stop().elapsed());
		} catch (IOException e) {
			LOG.error("Failed to collect output file {}: ", filePath, e);
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		} catch (InterruptedException e) {
			// Restore interrupted state
			Thread.currentThread().interrupt();
			LOG.error("Failed to collect output file {}: ", filePath, e);
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}
}
