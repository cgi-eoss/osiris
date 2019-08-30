package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FtpClientPools {
	
	
	private Map<String, GenericObjectPool<FTPClient>> clientPools = new HashMap<>();
	private OsirisServerClient osirisServerClient;
	
	private static final int DEFAULT_POOL_SIZE = 4;
	
	@Autowired
	public FtpClientPools(OsirisServerClient osirisServerClient) {
		this.osirisServerClient = osirisServerClient;
	}
	
	public FTPClient getFtpClient(URI ftpUri) throws FtpHarvesterException {
		GenericObjectPool<FTPClient> pool = getClientPool(ftpUri);
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			throw new FtpHarvesterException(e);
		}
	}

	private GenericObjectPool<FTPClient> getClientPool(URI ftpUri) {
		synchronized (this) {
			GenericObjectPool<FTPClient> pool = clientPools.get(ftpUri.getHost());
			if (pool == null) {
				pool = new GenericObjectPool<>(new OsirisFtpClientFactory(ftpUri, osirisServerClient), new FtpPoolConfig(DEFAULT_POOL_SIZE));
				clientPools.put(ftpUri.getHost(), pool);
			}
			return pool;
		}
	}

	public void releaseFtpClient(URI ftpUri, FTPClient ftpClient) {
		GenericObjectPool<FTPClient> pool = getClientPool(ftpUri);
		pool.returnObject(ftpClient);
	}
}
