package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.JobFtpFileAvailable;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.scheduledjobs.service.PersistentScheduledJob;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.FailedLoginException;

@Component
@Log4j2
public class FtpScheduledJob extends PersistentScheduledJob {

    @Autowired
    FtpHarvesterService ftpHarvesterService;
    
    @Autowired
    OsirisQueueService queueService;
    
    @Override
    public void executeJob(Map<String, Object> jobContext) {
        LOG.info("New ftp job received");
        Job job = (Job) jobContext.get("job");
        String ftpRootUriStr = (String) jobContext.get("ftpRootUri");
        Instant start = (Instant) jobContext.get("start");
        jobContext.put("start", OffsetDateTime.now().withSecond(0).withNano(0).toInstant());
        try {
        	URI ftpRootUri = URI.create(ftpRootUriStr);
            List<String> fileUris = ftpHarvesterService.harvestFiles(ftpRootUri, start);
            for (String fileUri: fileUris) {
            	JobFtpFileAvailable jobFtpFileAvailable = JobFtpFileAvailable.newBuilder()
            			.setJob(job)
            			.setFileUri(fileUri)
            			.setFtpRoot(ftpRootUri.getPath())
            			.build();
            	Map<String, Object> headers = new HashMap<>();
            	headers.put("jobId", job.getIntJobId());
            	headers.put("messageType", "FTPFileAvailable");
                queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, jobFtpFileAvailable);
            }
            
        } catch (IOException | FailedLoginException e) {
            LOG.error(e);
            Map<String, Object> headers = new HashMap<>();
        	headers.put("jobId", job.getIntJobId());
            queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, JobError.newBuilder().setErrorDescription(e.getMessage()).build());
        }
    }
}
