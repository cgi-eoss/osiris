package com.cgi.eoss.osiris.rpc;

import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
import io.grpc.ManagedChannel;

public class OsirisServerClient {
	
	private final ManagedChannelProvider managedChannelProvider;

    public OsirisServerClient(ManagedChannelProvider managedChannelProvider) {
        this.managedChannelProvider = managedChannelProvider;
    }

    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub() {
        return ServiceContextFilesServiceGrpc.newBlockingStub(getChannel());
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsServiceBlockingStub() {
        return CredentialsServiceGrpc.newBlockingStub(getChannel());
    }

    public CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub() {
        return CatalogueServiceGrpc.newBlockingStub(getChannel());
    }
    
    public CatalogueServiceGrpc.CatalogueServiceStub catalogueServiceStub() {
        return CatalogueServiceGrpc.newStub(getChannel());
    }

    private ManagedChannel getChannel() {
    	return managedChannelProvider.getChannel();
    }
}
