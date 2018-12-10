package com.cgi.eoss.osiris.server;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.catalogue.CatalogueConfig;
import com.cgi.eoss.osiris.orchestrator.OrchestratorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * <p>Application running the OSIRIS orchestrator and associated "master" services.</p>
 */
@Import({
        ApiConfig.class,
        CatalogueConfig.class,
        OrchestratorConfig.class
})
@SpringBootApplication(scanBasePackageClasses = OsirisServerApplication.class)
public class OsirisServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsirisServerApplication.class, args);
    }

}
