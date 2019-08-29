package com.cgi.eoss.osiris.queues.service;

import java.util.Map;

public interface OsirisQueueService {

    final static String jobQueueName = "osiris-jobs";
    
    final static String jobUpdatesQueueName = "osiris-jobs-updates";

    final static String wpsJobQueueName = "osiris-wps-jobs";
    
    final static String wpsJobUpdatesQueueName = "osiris-wps-jobs-updates";

    static final String ftpJobQueueName = "osiris-ftp-jobs";
    
    static final String ftpJobUpdatesQueueName = "osiris-ftp-job-updates";
    
    static final String ftpJobUpdatesRetryQueueName = "osiris-ftp-job-updates-retry";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    public Object receiveObject(String queueName);
    
    public Object receiveObjectNoWait(String queueName);
   
    public Object receiveSelectedObject(String queueName, String messageSelector);
    
    public Object receiveSelectedObjectNoWait(String queueName, String messageSelector);
    
    public Message receiveSelected(String queueName, String messageSelector);
	
	public Message receive(String queueName);
	
	public Message receiveNoWait(String queueName);
	
	public Message receiveSelectedNoWait(String queueName, String messageSelector);

	public long getQueueLength(String queueName);
}
