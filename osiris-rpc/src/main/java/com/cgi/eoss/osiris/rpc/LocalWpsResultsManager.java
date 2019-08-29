package com.cgi.eoss.osiris.rpc;


import com.cgi.eoss.osiris.rpc.wps.controller.DeleteWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.DeleteWpsOutputFileResponse;
import com.cgi.eoss.osiris.rpc.wps.controller.GetWpsOutputFileParams;
import com.cgi.eoss.osiris.rpc.wps.controller.ListWpsOutputFilesParam;
import com.cgi.eoss.osiris.rpc.wps.controller.OsirisWpsResultsManagerGrpc;
import com.cgi.eoss.osiris.rpc.wps.controller.OsirisWpsResultsManagerGrpc.OsirisWpsResultsManagerBlockingStub;
import com.cgi.eoss.osiris.rpc.wps.controller.OsirisWpsResultsManagerGrpc.OsirisWpsResultsManagerStub;
import com.cgi.eoss.osiris.rpc.wps.controller.WpsOutputFileList;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.scheduling.annotation.Async;

public class LocalWpsResultsManager {
	
	OsirisWpsResultsManagerBlockingStub wpsResultsManagerBlockingStub;
	
	OsirisWpsResultsManagerStub wpsResultsManagerStub;
    
    public LocalWpsResultsManager(ManagedChannelBuilder inProcessChannelBuilder) {
        this.wpsResultsManagerBlockingStub = OsirisWpsResultsManagerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        this.wpsResultsManagerStub = OsirisWpsResultsManagerGrpc.newStub(inProcessChannelBuilder.build());
    }
    
    
    public WpsOutputFileList listOutputFiles(ListWpsOutputFilesParam listWpsOutputFilesParam) {
        return wpsResultsManagerBlockingStub.listOutputFiles(listWpsOutputFilesParam);
    }
    
    @Async
    public void asyncGetFile(GetWpsOutputFileParams getWpsFileParams, StreamObserver<FileStream> responseObserver) {
        wpsResultsManagerStub.getFile(getWpsFileParams, responseObserver);
    }
    
    public DeleteWpsOutputFileResponse deleteFile(DeleteWpsOutputFileParams deleteWpsFileParams) {
    	return wpsResultsManagerBlockingStub.deleteFile(deleteWpsFileParams);
    }

}
