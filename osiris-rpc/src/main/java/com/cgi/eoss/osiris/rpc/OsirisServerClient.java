package com.cgi.eoss.osiris.rpc;

import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class OsirisServerClient {
    private final DiscoveryClient discoveryClient;
    private final String osirisServerServiceId;

    public OsirisServerClient(DiscoveryClient discoveryClient, String osirisServerServiceId) {
        this.discoveryClient = discoveryClient;
        this.osirisServerServiceId = osirisServerServiceId;
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
        ServiceInstance osirisServer = Iterables.getOnlyElement(discoveryClient.getInstances(osirisServerServiceId));

        return ManagedChannelBuilder.forAddress(osirisServer.getHost(), Integer.parseInt(osirisServer.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();
    }

}
