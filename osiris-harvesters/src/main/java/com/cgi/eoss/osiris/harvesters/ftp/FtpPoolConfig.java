package com.cgi.eoss.osiris.harvesters.ftp;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class FtpPoolConfig extends GenericObjectPoolConfig {

    public FtpPoolConfig(int maxTotal) {
    	setMaxTotal(maxTotal);
        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(60000);
        setTimeBetweenEvictionRunsMillis(30000);
        setNumTestsPerEvictionRun(-1);
    }
}
