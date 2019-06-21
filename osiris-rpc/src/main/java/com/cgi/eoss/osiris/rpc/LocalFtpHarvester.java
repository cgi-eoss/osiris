package com.cgi.eoss.osiris.rpc;


import org.springframework.scheduling.annotation.Async;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.DeleteFileResponse;
import com.cgi.eoss.osiris.rpc.ftp.harvester.FileList;
import com.cgi.eoss.osiris.rpc.ftp.harvester.GetFileParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.HarvestFilesParams;
import com.cgi.eoss.osiris.rpc.ftp.harvester.OsirisFtpHarvesterGrpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class LocalFtpHarvester {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalFtpHarvester(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }
    
    @Async
    public void asyncHarvestFile(HarvestFilesParams harvestFileParams, StreamObserver<FileList> responseObserver) {
        OsirisFtpHarvesterGrpc.OsirisFtpHarvesterStub ftpHarvester = OsirisFtpHarvesterGrpc.newStub(inProcessChannelBuilder.build());
        ftpHarvester.harvestFiles(harvestFileParams, responseObserver);
    }
    
    @Async
    public void asyncGetFile(GetFileParams getFileParams, StreamObserver<FileStream> responseObserver) {
        OsirisFtpHarvesterGrpc.OsirisFtpHarvesterStub ftpHarvester = OsirisFtpHarvesterGrpc.newStub(inProcessChannelBuilder.build());
        ftpHarvester.getFile(getFileParams, responseObserver);
    }
    
    @Async
    public void asyncDeleteFile(DeleteFileParams deleteFileParams, StreamObserver<DeleteFileResponse> responseObserver) {
        OsirisFtpHarvesterGrpc.OsirisFtpHarvesterStub ftpHarvester = OsirisFtpHarvesterGrpc.newStub(inProcessChannelBuilder.build());
        ftpHarvester.deleteFile(deleteFileParams, responseObserver);
    }

}
