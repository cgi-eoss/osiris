package com.cgi.eoss.osiris.orchestrator;

import com.cgi.eoss.osiris.catalogue.CatalogueConfig;
import com.cgi.eoss.osiris.costing.CostingConfig;
import com.cgi.eoss.osiris.orchestrator.service.AutowiringSpringBeanJobFactory;
import com.cgi.eoss.osiris.orchestrator.service.CachingWorkerFactory;
import com.cgi.eoss.osiris.orchestrator.service.OsirisServiceLauncher;
import com.cgi.eoss.osiris.persistence.PersistenceConfig;
import com.cgi.eoss.osiris.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.osiris.queues.QueuesConfig;
import com.cgi.eoss.osiris.rpc.InProcessRpcConfig;
import com.cgi.eoss.osiris.search.SearchConfig;
import com.cgi.eoss.osiris.security.SecurityConfig;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import javax.sql.DataSource;
/**
 * <p>Spring configuration for the OSIRIS Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides the {@link OsirisServiceLauncher} RPC service.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
        QueuesConfig.class,
        InProcessRpcConfig.class,
        PersistenceConfig.class,
        SearchConfig.class,
        SecurityConfig.class
})
@EnableEurekaClient
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public ExpressionParser workerLocatorExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    public CachingWorkerFactory workerFactory(DiscoveryClient discoveryClient,
                                       @Value("${osiris.orchestrator.worker.eurekaServiceId:osiris worker}") String workerServiceId,
                                       ExpressionParser workerLocatorExpressionParser,
                                       WorkerLocatorExpressionDataService workerLocatorExpressionDataService,
                                       @Value("${osiris.orchestrator.worker.defaultWorkerExpression:\"LOCAL\"}") String defaultWorkerExpression) {
        return new CachingWorkerFactory(discoveryClient, workerServiceId, workerLocatorExpressionParser, workerLocatorExpressionDataService, defaultWorkerExpression);
    }
    
    
    
    @Bean
    @DependsOn("dataSource")
    public Scheduler scheduler(ApplicationContext applicationContext, DataSource datasource, PlatformTransactionManager transactionManager) throws Exception {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(datasource);
        Properties quartzProperties = quartzProperties().entrySet().stream()
                        .map(e -> new AbstractMap.SimpleEntry<String, Object>("org.quartz" + "." + e.getKey(), e.getValue()))
                        .flatMap(this::flattenToQuartzProperties)
                        .collect(Properties::new,
                                (properties, entry) -> properties.put(entry.getKey(), entry.getValue()),
                                (properties, properties2) -> properties2.forEach(properties::put));
        factory.setTransactionManager(transactionManager);
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        factory.setJobFactory(jobFactory);
        factory.setQuartzProperties(quartzProperties);
        factory.afterPropertiesSet();
        factory.getScheduler().start();
        return factory.getObject();
    }

    private Stream<Map.Entry<String, String>> flattenToQuartzProperties(Map.Entry<String, Object> e) {
        if (e.getValue() instanceof Map) {
            return ((Map<?, ?>) e.getValue()).entrySet().stream()
                    .map(e1 -> new AbstractMap.SimpleEntry<String, Object>(e.getKey() + "." + e1.getKey(), e1.getValue()))
                    .flatMap(this::flattenToQuartzProperties);
        }
        return Stream.of(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString()));
    }
    
    @Bean
    @ConfigurationProperties(prefix="org.quartz")
    public Map<String, Object> quartzProperties() {
        return new HashMap<>();
    }

}
