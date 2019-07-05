package com.cgi.eoss.osiris.queues.listener;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

@Log4j2
public class RateLimitedMessageListenerContainer extends DefaultMessageListenerContainer implements InitializingBean {

    private RateLimiter rateLimiter;
    private boolean rateLimiterActive;
    private double permitsPerSecond;

    @Override
    protected boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer) throws JMSException {
        if (rateLimiterActive) {
            LOG.info("Rate limiting listener because of previous failure");
            double secondsSlept = rateLimiter.acquire(); // may wait
            LOG.info("Slept for seconds:" + secondsSlept);
            rateLimiterActive = false;
        }
        boolean messageReceived;
        try {
        	messageReceived = super.receiveAndExecute(invoker, session, consumer);
        }
        catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception err) {
			throw err;
		}
        return messageReceived;
    }
  
    @Override
    protected void handleListenerException(Throwable ex) {
    	rateLimiterActive = true;
    	super.handleListenerException(ex);
    }
    
    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void setPermitsPerSecond(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        this.rateLimiter = RateLimiter.create(permitsPerSecond);

    }
}