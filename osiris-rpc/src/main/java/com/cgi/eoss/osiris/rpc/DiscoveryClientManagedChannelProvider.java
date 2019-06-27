package com.cgi.eoss.osiris.rpc;

import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class DiscoveryClientManagedChannelProvider implements ManagedChannelProvider {

	private final DiscoveryClient discoveryClient;
	private final String osirisServerServiceId;

	public DiscoveryClientManagedChannelProvider(DiscoveryClient discoveryClient, String osirisServerServiceId) {
		this.discoveryClient = discoveryClient;
		this.osirisServerServiceId = osirisServerServiceId;
	}

	@Override
	public ManagedChannel getChannel() {
		ServiceInstance osirisServer = Iterables.getOnlyElement(discoveryClient.getInstances(osirisServerServiceId));

		return ManagedChannelBuilder
				.forAddress(osirisServer.getHost(), Integer.parseInt(osirisServer.getMetadata().get("grpcPort")))
				.usePlaintext(true).build();
	}

}
