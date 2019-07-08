package com.cgi.eoss.osiris.harvesters.ftp;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.JobFtpFileAvailable;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import com.cgi.eoss.osiris.scheduledjobs.service.PersistentScheduledJob;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class FtpScheduledJob extends PersistentScheduledJob {

    private static final String LATEST_TIMESTAMP_HARVESTED_FILES = "latestTimestampHarvested";

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
        Set<String> latestTimestampHarvested = (Set<String>) jobContext.getOrDefault(LATEST_TIMESTAMP_HARVESTED_FILES, new HashSet<>());
        Instant nextStart = start;
        try {
        	URI ftpRootUri = URI.create(ftpRootUriStr);
        	List<FileItem> fileItems = ftpHarvesterService.harvestFiles(ftpRootUri, start);
            ImmutableListMultimap<Instant, FileItem> filesByTimestamp = fileListToMultimapSortedByTimestamp(fileItems);
            for (Instant timestamp: filesByTimestamp.keySet()) {
            	if (!timestamp.equals(start)) {
            		nextStart = timestamp;
            		latestTimestampHarvested = new HashSet<>();
            	}
            	for (FileItem fileItemByTimestamp: filesByTimestamp.get(timestamp)){
            		if (!latestTimestampHarvested.contains(fileItemByTimestamp.getUri())) {
            			JobFtpFileAvailable jobFtpFileAvailable = JobFtpFileAvailable.newBuilder()
                    			.setJob(job)
                    			.setFileUri(fileItemByTimestamp.getUri())
                    			.setFtpRoot(ftpRootUri.getPath())
                    			.build();
                    	Map<String, Object> headers = new HashMap<>();
                    	headers.put("jobId", job.getIntJobId());
                    	headers.put("messageType", "FTPFileAvailable");
                    	queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, jobFtpFileAvailable);
                    	latestTimestampHarvested.add(fileItemByTimestamp.getUri());
                    	jobContext.put("start", nextStart);
                        jobContext.put(LATEST_TIMESTAMP_HARVESTED_FILES, latestTimestampHarvested);
            		}
            	}
            }
        } catch (IOException | FailedLoginException e) {
            LOG.error(e);
            Map<String, Object> headers = new HashMap<>();
        	headers.put("jobId", job.getIntJobId());
            queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, JobError.newBuilder().setErrorDescription(e.getMessage()).build());
        }
    }

	private ImmutableListMultimap<Instant, FileItem> fileListToMultimapSortedByTimestamp(List<FileItem> fileItems) {
		//Sort by timestamp
		fileItems.sort((item1, item2) -> item1.getTimestamp().compareTo(item2.getTimestamp()));
		//Index by timestamp
		return Multimaps.index(fileItems, FileItem::getTimestamp);
	}
}
