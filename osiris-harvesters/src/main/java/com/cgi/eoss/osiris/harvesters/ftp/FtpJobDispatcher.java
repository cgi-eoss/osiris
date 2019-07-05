package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.FtpJobSpec;
import com.cgi.eoss.osiris.rpc.FtpJobStarted;
import com.cgi.eoss.osiris.rpc.FtpJobStopped;
import com.cgi.eoss.osiris.rpc.StopFtpJob;
import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

/**
 * <p>
 * Service to control the dispatch of FTP Jobs 
 * </p>
 */
@Log4j2
@Service
public class FtpJobDispatcher {
    
    private static final String FTP_SCHEDULED_JOB_GROUP = "osiris-ftp";

    private static final String FTP_SCHEDULED_JOB_PREFIX = "ftp-job-";

    private ScheduledJobService scheduledJobService;
    
    OsirisQueueService queueService;

    //Keep this over a minute to avoid issues with limited precision of FTP timestamps
    private static final int FTP_CHECK_FREQUENCY_SEC = 70;
    

    @Autowired
    public FtpJobDispatcher(ScheduledJobService scheduledJobService, OsirisQueueService queueService) {
       this.scheduledJobService = scheduledJobService;
       this.queueService = queueService;
    }

    @JmsListener(destination = OsirisQueueService.ftpJobQueueName)
    public void receiveFtpJob(@Payload ObjectMessage objectMessage) throws JMSException {
        LOG.debug("Checking for available jobs in the queue");
        dispatchMessage(objectMessage.getObject());
    }


    private void dispatchMessage(Object message) {
        if (message instanceof FtpJobSpec) {
            FtpJobSpec ftpJobSpec = (FtpJobSpec) message;
            LOG.info("Scheduling ftp job");
            scheduleFTPJob(ftpJobSpec);
            Map<String, Object> headers = new HashMap<>();
        	headers.put("jobId", ftpJobSpec.getJob().getIntJobId());
        	queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, FtpJobStarted.newBuilder().setJob(ftpJobSpec.getJob()).build());
        }
        else if (message instanceof StopFtpJob) {
            StopFtpJob stopFtpJob = (StopFtpJob) message;
            String ftpJobIdentity = FTP_SCHEDULED_JOB_PREFIX + stopFtpJob.getJob().getIntJobId();
            scheduledJobService.unscheduleJob(ftpJobIdentity, FTP_SCHEDULED_JOB_GROUP);
            scheduledJobService.deleteJob(ftpJobIdentity, FTP_SCHEDULED_JOB_GROUP);
            Map<String, Object> headers = new HashMap<>();
        	headers.put("jobId", stopFtpJob.getJob().getIntJobId());
        	queueService.sendObject(OsirisQueueService.ftpJobUpdatesQueueName, headers, FtpJobStopped.newBuilder().setJob(stopFtpJob.getJob()).build());
        }
    }


    private void scheduleFTPJob(FtpJobSpec ftpJobSpec) {
        Map<String, Object> jobContext = new HashMap<>();
        String ftpRootUri = ftpJobSpec.getFtpRootUri();
        jobContext.put("ftpRootUri", ftpRootUri);
        jobContext.put("job", ftpJobSpec.getJob());
        String ftpJobIdentity = FTP_SCHEDULED_JOB_PREFIX + ftpJobSpec.getJob().getIntJobId();
        scheduledJobService.scheduleJobEveryNSeconds(FtpScheduledJob.class, ftpJobIdentity, FTP_SCHEDULED_JOB_GROUP, jobContext, FTP_CHECK_FREQUENCY_SEC);
    }

}
