package com.cgi.eoss.osiris.persistence;

import com.cgi.eoss.osiris.model.OsirisEntity;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
})
@EnableJpaRepositories(basePackageClasses = OsirisEntityDao.class,
        excludeFilters = {@ComponentScan.Filter(SpringJpaRepositoryIgnore.class)})
@EnableTransactionManagement
@EntityScan(basePackageClasses = OsirisEntity.class)
@ComponentScan(basePackageClasses = PersistenceConfig.class)
public class PersistenceConfig {

}
