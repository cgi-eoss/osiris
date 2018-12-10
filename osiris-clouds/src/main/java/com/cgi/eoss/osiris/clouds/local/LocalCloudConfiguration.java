package com.cgi.eoss.osiris.clouds.local;

import com.cgi.eoss.osiris.clouds.service.NodeFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "osiris.clouds.local.enabled", havingValue = "true", matchIfMissing = true)
public class LocalCloudConfiguration {

    @Value("${osiris.clouds.local.dockerHostUrl:unix:///var/run/docker.sock}")
    private String dockerHostUrl;

    @Value("${osiris.clouds.local.maxPoolSize:10}")
    private int maxPoolSize;

    @Bean
    @ConditionalOnMissingBean(NodeFactory.class)
    public LocalNodeFactory localNodeFactory() {
        return new LocalNodeFactory(maxPoolSize, dockerHostUrl);
    }

}
