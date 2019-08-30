package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.rpc.FileStream;
import com.cgi.eoss.osiris.rpc.FileStreamServer;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileResponse;
import com.cgi.eoss.osiris.rpc.ftp.harvester.FileItem;
import com.cgi.eoss.osiris.rpc.ftp.harvester.FileList;
import com.cgi.eoss.osiris.rpc.ftp.harvester.GetFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.HarvestFilesParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.OsirisFtpHarvesterGrpc;
import com.google.common.base.Stopwatch;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@GRpcService
@Log4j2
public class FtpHarvesterGrpc extends OsirisFtpHarvesterGrpc.OsirisFtpHarvesterImplBase {

	private FtpHarvesterService ftpHarvesterService;

	@Autowired
	public FtpHarvesterGrpc(FtpHarvesterService ftpHarvesterService) {
		this.ftpHarvesterService = ftpHarvesterService;
	}

	@Override
	public void harvestFiles(HarvestFilesParams harvestFilesParams, StreamObserver<FileList> responseObserver) {
		try {
			List<com.cgi.eoss.osiris.harvesters.ftp.FileItem> fileUris = ftpHarvesterService.harvestFiles(URI.create(harvestFilesParams.getFtpRootUri()), Instant.now());
			responseObserver.onNext(FileList.newBuilder().addAllItems(fileUris.stream().map(f -> FileItem.newBuilder().setFileUri(f.getUri()).setTimestamp(GrpcUtil.timestampFromInstant(f.getTimestamp())).build()).collect(Collectors.toList())).build());
			responseObserver.onCompleted();
		} catch (FtpHarvesterException e) {
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}

	@Override
	public void getFile(GetFileParams getFileParams, StreamObserver<FileStream> responseObserver) {
		try {
			final FtpFileMeta ftpMeta = ftpHarvesterService.getFile(URI.create(getFileParams.getFileUri()));
			serveFtpFile(getFileParams.getFileUri(), responseObserver, ftpMeta);
		} catch (FtpHarvesterException e) {
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}
	
	@Override
	public void deleteFile(DeleteFileParams deleteFileParams, StreamObserver<DeleteFileResponse> responseObserver) {
		try {
			ftpHarvesterService.deleteFile(URI.create(deleteFileParams.getFileUri()));
			responseObserver.onNext(DeleteFileResponse.newBuilder().build());
			responseObserver.onCompleted();
		} catch (FtpHarvesterException e) {
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}

	private void serveFtpFile(String fileUri, StreamObserver<FileStream> responseObserver,
			final FtpFileMeta ftpMeta) {
		try (FileStreamServer fileStreamServer = new FileStreamServer(null, responseObserver) {
			@Override
			protected FileStream.FileMeta buildFileMeta() {
				return FileStream.FileMeta.newBuilder().setFilename(ftpMeta.getFileName())
						.setSize(ftpMeta.getFileSize()).build();
			}

			@Override
			protected ReadableByteChannel buildByteChannel() {
				return Channels.newChannel(ftpMeta.getFileInputStream());
			}
		}) {
			Stopwatch stopwatch = Stopwatch.createStarted();
			fileStreamServer.streamFile();
			LOG.info("Transferred output file {} ({} bytes) in {}", fileStreamServer.getFileMeta().getFilename(),
					fileStreamServer.getFileMeta().getSize(), stopwatch.stop().elapsed());
		} catch (IOException e) {
			LOG.error("Failed to collect output file {}: ", fileUri, e);
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		} catch (InterruptedException e) {
			// Restore interrupted state
			Thread.currentThread().interrupt();
			LOG.error("Failed to collect output file {}: ", fileUri, e);
			responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
		}
	}
}
