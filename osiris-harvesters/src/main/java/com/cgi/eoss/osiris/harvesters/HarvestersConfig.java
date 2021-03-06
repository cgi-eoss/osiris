package com.cgi.eoss.osiris.harvesters;


import com.cgi.eoss.osiris.harvesters.wps.WpsExecutionController;
import com.cgi.eoss.osiris.harvesters.wps.WpsJobController;
import com.cgi.eoss.osiris.queues.QueuesConfig;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.scheduledjobs.ScheduledJobsConfig;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;
import java.nio.file.Paths;



@Configuration
@Import({QueuesConfig.class,
        ScheduledJobsConfig.class})
@ComponentScan(basePackageClasses = HarvestersConfig.class)
public class HarvestersConfig {

	@Bean
    public Path wpsOutputPath(@Value("${osiris.harvesters.wps.resultsPath:/deta/wpsOutputs}") String outputPath) {
        return Paths.get(outputPath);
    }
	
	@Bean
    public WpsJobController wpsJobController(ScheduledJobService scheduledJobService, OsirisQueueService queueService, WpsExecutionController wpsExecutionController, @Qualifier("wpsOutputPath") Path wpsOutputPath) {
        return new WpsJobController(scheduledJobService, queueService, wpsExecutionController, wpsOutputPath);
    }

}
