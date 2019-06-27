package com.cgi.eoss.osiris.worker;

import com.cgi.eoss.osiris.clouds.CloudsConfig;
import com.cgi.eoss.osiris.clouds.service.NodeFactory;
import com.cgi.eoss.osiris.io.ServiceInputOutputManager;
import com.cgi.eoss.osiris.io.ServiceInputOutputManagerImpl;
import com.cgi.eoss.osiris.io.download.CachingSymlinkDownloaderFacade;
import com.cgi.eoss.osiris.io.download.Downloader;
import com.cgi.eoss.osiris.io.download.DownloaderFacade;
import com.cgi.eoss.osiris.io.download.UnzipStrategy;
import com.cgi.eoss.osiris.queues.QueuesConfig;
import com.cgi.eoss.osiris.rpc.DiscoveryClientManagedChannelProvider;
import com.cgi.eoss.osiris.rpc.InProcessRpcConfig;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.cgi.eoss.osiris.worker.worker.OsirisWorkerNodeManager;
import com.cgi.eoss.osiris.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ComponentScan(
        basePackageClasses = {WorkerConfig.class, Downloader.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = OsirisWorkerApplication.class)
)
@Import({
		QueuesConfig.class,
        CloudsConfig.class,
        InProcessRpcConfig.class
})
@EnableEurekaClient
@EnableScheduling
@EnableConfigurationProperties(UnzipBySchemeProperties.class)
public class WorkerConfig {

    @Bean
    public Path cacheRoot(@Value("${osiris.worker.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }

    @Bean
    public Boolean unzipAllDownloads(@Value("${ftep.worker.io.unzipAllDownloads:true}") boolean unzipAllDownloads) {
        return unzipAllDownloads;
    }
   
    @Bean
    public Integer cacheConcurrencyLevel(@Value("${osiris.worker.cache.concurrency:4}") int concurrencyLevel) {
        return concurrencyLevel;
    }

    @Bean
    public Integer cacheMaxWeight(@Value("${osiris.worker.cache.maxWeight:1024}") int maximumWeight) {
        return maximumWeight;
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${osiris.worker.jobEnv.baseDir:/data/cache/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }
    
    @Bean
    public Integer maxJobsPerNode(@Value("${osiris.worker.maxJobsPerNode:2}") int maxJobsPerNode) {
        return maxJobsPerNode;
    }

    @Bean
    public Integer minWorkerNodes(@Value("${osiris.worker.minWorkerNodes:1}") int minWorkerNodes) {
        return minWorkerNodes;
    }

    @Bean
    public Integer maxWorkerNodes(@Value("${osiris.worker.maxWorkerNodes:1}") int maxWorkerNodes) {
        return maxWorkerNodes;
    }
    
    @Bean
    public Long minSecondsBetweenScalingActions(@Value("${osiris.worker.minSecondsBetweenScalingActions:600}") long minSecondsBetweenScalingActions) {
        return minSecondsBetweenScalingActions;
    }
    
    @Bean
    public Long minimumHourFractionUptimeSeconds(@Value("${osiris.worker.minimumHourFractionUptimeSeconds:3000}") long minimumHourFractionUptimeSeconds) {
        return minimumHourFractionUptimeSeconds;
    }

    @Bean
    public String workerId(@Value("${eureka.instance.metadataMap.workerId:workerId}") String workerId) {
        return workerId;
    }
    
    @Bean
    @ConditionalOnProperty("osiris.worker.dockerRegistryUrl")
    public DockerRegistryConfig dockerRegistryConfig(
            @Value("${osiris.worker.dockerRegistryUrl}") String dockerRegistryUrl,
            @Value("${osiris.worker.dockerRegistryUsername}") String dockerRegistryUsername,
            @Value("${osiris.worker.dockerRegistryPassword}") String dockerRegistryPassword) {
        return new DockerRegistryConfig(dockerRegistryUrl, dockerRegistryUsername, dockerRegistryPassword);
    }

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // If an http_proxy is set in the current environment, add it to the client
        String httpProxy = System.getenv("http_proxy");
        if (!Strings.isNullOrEmpty(httpProxy)) {
            URI proxyUri = URI.create(httpProxy);
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }

        return builder.build();
    }

    @Bean
    public OsirisServerClient osirisServerClient(DiscoveryClient discoveryClient,
                                               @Value("${osiris.worker.server.eurekaServiceId:osiris server}") String osirisServerServiceId) {
        return new OsirisServerClient(new DiscoveryClientManagedChannelProvider(discoveryClient, osirisServerServiceId));
    }

    @Bean
    public DownloaderFacade downloaderFacade(@Qualifier("cacheRoot") Path cacheRoot,
                                             @Qualifier("unzipAllDownloads") Boolean unzipAllDownloads,
                                             UnzipBySchemeProperties unzipBySchemeProperties,
                                             @Qualifier("cacheConcurrencyLevel") Integer concurrencyLevel,
                                             @Qualifier("cacheMaxWeight") Integer maximumWeight) {
        return new CachingSymlinkDownloaderFacade(cacheRoot, UnzipStrategy.UNZIP_IN_SAME_FOLDER, unzipBySchemeProperties.getUnzipByScheme(), concurrencyLevel, maximumWeight);
    }

    @Bean
    public ServiceInputOutputManager serviceInputOutputManager(OsirisServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        return new ServiceInputOutputManagerImpl(ftepServerClient, downloaderFacade);
    }
    
    @Bean
    public OsirisWorkerNodeManager workerNodeManager(NodeFactory nodeFactory, @Qualifier("cacheRoot") Path dataBaseDir, JobEnvironmentService jobEnvironmentService,
            @Qualifier("maxJobsPerNode") Integer maxJobsPerNode) {
        OsirisWorkerNodeManager workerNodeManager = new OsirisWorkerNodeManager(nodeFactory, dataBaseDir, maxJobsPerNode);
        return workerNodeManager;
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }



}
