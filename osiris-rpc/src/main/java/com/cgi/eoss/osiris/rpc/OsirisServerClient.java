package com.cgi.eoss.osiris.rpc;

import com.cgi.eoss.osiris.rpc.CredentialsServiceGrpc.CredentialsServiceBlockingStub;
import com.cgi.eoss.osiris.rpc.ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc.CatalogueServiceBlockingStub;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc.CatalogueServiceStub;
import io.grpc.Channel;

public class OsirisServerClient {
	
	private final Channel channel;
	private ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub; 
	private CredentialsServiceBlockingStub credentialsServiceBlockingStub;
	private CatalogueServiceBlockingStub catalogueServiceBlockingStub;
	private CatalogueServiceStub catalogueServiceStub;
	
	
    public OsirisServerClient(ManagedChannelProvider managedChannelProvider) {
       channel = managedChannelProvider.getChannel();
    }

    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub() {
    	if (serviceContextFilesServiceBlockingStub == null) {
    		serviceContextFilesServiceBlockingStub = ServiceContextFilesServiceGrpc.newBlockingStub(channel);
        } 	
		return serviceContextFilesServiceBlockingStub;
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsServiceBlockingStub() {
        if (credentialsServiceBlockingStub == null) {
        	credentialsServiceBlockingStub = CredentialsServiceGrpc.newBlockingStub(channel);
        } 	
		return credentialsServiceBlockingStub;
    }

    public CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub() {
    	if (catalogueServiceBlockingStub == null) {
    		catalogueServiceBlockingStub = CatalogueServiceGrpc.newBlockingStub(channel);
        } 	
		return catalogueServiceBlockingStub;
    }
    
    public CatalogueServiceGrpc.CatalogueServiceStub catalogueServiceStub() {
    	if (catalogueServiceStub == null) {
    		catalogueServiceStub = CatalogueServiceGrpc.newStub(channel);
        } 	
		return catalogueServiceStub;
    }

}
