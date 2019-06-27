package com.cgi.eoss.osiris.rpc;

import io.grpc.ManagedChannel;
import io.netty.channel.Channel;

public interface ManagedChannelProvider {

	public ManagedChannel getChannel();
	
}
