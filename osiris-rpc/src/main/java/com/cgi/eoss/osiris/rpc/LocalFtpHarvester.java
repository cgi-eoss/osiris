package com.cgi.eoss.osiris.rpc;


import org.springframework.scheduling.annotation.Async;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileResponse;
import com.cgi.eoss.osiris.rpc.ftp.harvester.FileList;
import com.cgi.eoss.osiris.rpc.ftp.harvester.GetFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.HarvestFilesParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.OsirisFtpHarvesterGrpc;
import com.cgi.eoss.osiris.rpc.ftp.harvester.OsirisFtpHarvesterGrpc.OsirisFtpHarvesterStub;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalFtpHarvester {
	
	OsirisFtpHarvesterStub ftpHarvester;
    
	public LocalFtpHarvester(ManagedChannelBuilder inProcessChannelBuilder) {
		ftpHarvester = OsirisFtpHarvesterGrpc.newStub(inProcessChannelBuilder.build());
    }
    
    @Async
    public void asyncHarvestFile(HarvestFilesParams harvestFileParams, StreamObserver<FileList> responseObserver) {
        ftpHarvester.harvestFiles(harvestFileParams, responseObserver);
    }
    
    @Async
    public void asyncGetFile(GetFileParams getFileParams, StreamObserver<FileStream> responseObserver) {
        ftpHarvester.getFile(getFileParams, responseObserver);
    }
    
    @Async
    public void asyncDeleteFile(DeleteFileParams deleteFileParams, StreamObserver<DeleteFileResponse> responseObserver) {
        ftpHarvester.deleteFile(deleteFileParams, responseObserver);
    }

}
