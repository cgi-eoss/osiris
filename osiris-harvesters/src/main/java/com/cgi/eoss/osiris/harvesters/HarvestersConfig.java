package com.cgi.eoss.osiris.harvesters;


import com.cgi.eoss.osiris.queues.QueuesConfig;
import com.cgi.eoss.osiris.scheduledjobs.ScheduledJobsConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;



@Configuration
@Import({QueuesConfig.class,
        ScheduledJobsConfig.class})
@ComponentScan(basePackageClasses = HarvestersConfig.class)
public class HarvestersConfig {

    

}
