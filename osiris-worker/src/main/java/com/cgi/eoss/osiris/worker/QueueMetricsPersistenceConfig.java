package com.cgi.eoss.osiris.worker;

import com.cgi.eoss.osiris.worker.metrics.QueueMetric;
import com.cgi.eoss.osiris.worker.metrics.QueueMetricsRepository;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(name="osiris.worker.autoscaler.enabled", havingValue="true", matchIfMissing = false)
@EnableJpaRepositories(basePackageClasses = QueueMetricsRepository.class,
entityManagerFactoryRef = "queueMetricsEntityManager", 
transactionManagerRef = "queueMetricsTransactionManager")
@EntityScan(basePackageClasses = QueueMetric.class)
public class QueueMetricsPersistenceConfig {
 
    @Bean
    @ConfigurationProperties("osiris.worker.queuemetrics.datasource")
    public DataSourceProperties queueMetricsDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean
    public DataSource queueMetricsDataSource() {
        return queueMetricsDataSourceProperties().initializeDataSourceBuilder().build();
    }
    
    
    @Bean
    public LocalContainerEntityManagerFactoryBean queueMetricsEntityManager() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();

        factoryBean.setDataSource(queueMetricsDataSource());
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setPackagesToScan(QueueMetricsPersistenceConfig.class.getPackage().getName());

        return factoryBean;
    }
    
    @Bean
    public PlatformTransactionManager queueMetricsTransactionManager() {
        JpaTransactionManager transactionManager
          = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
          queueMetricsEntityManager().getObject());
        return transactionManager;
    }

}
