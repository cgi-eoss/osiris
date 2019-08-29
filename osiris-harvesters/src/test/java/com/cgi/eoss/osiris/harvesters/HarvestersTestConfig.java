package com.cgi.eoss.osiris.harvesters;


import com.cgi.eoss.osiris.harvesters.wps.WpsExecutionController;
import com.cgi.eoss.osiris.harvesters.wps.WpsJobController;
import com.cgi.eoss.osiris.queues.QueuesConfig;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.scheduledjobs.ScheduledJobsConfig;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import com.google.common.jimfs.Jimfs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.FileSystem;
import java.nio.file.Path;



@Configuration
@Import({QueuesConfig.class,
    ScheduledJobsConfig.class})
@ComponentScan(basePackageClasses = HarvestersTestConfig.class)
public class HarvestersTestConfig {

	@Bean
	public Path wpsInMemoryOutputPath(@Value("${osiris.harvesters.wps.resultsPath:/data/wpsOutputs}") String outputPathStr) {
		FileSystem fs = Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
        Path outputPath = fs.getPath(outputPathStr);
        return outputPath;
    }
	
	@Bean
	public WpsJobController wpsJobController(ScheduledJobService scheduledJobService, OsirisQueueService queueService, WpsExecutionController wpsExecutionController, @Qualifier("wpsInMemoryOutputPath") Path wpsOutputPath) {
        return new WpsJobController(scheduledJobService, queueService, wpsExecutionController, wpsOutputPath);
    }

}
