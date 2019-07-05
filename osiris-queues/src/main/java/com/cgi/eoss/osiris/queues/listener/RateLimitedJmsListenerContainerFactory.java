package com.cgi.eoss.osiris.queues.listener;

import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class RateLimitedJmsListenerContainerFactory extends DefaultJmsListenerContainerFactory {

    @Override
    public DefaultMessageListenerContainer createContainerInstance() {
        return new RateLimitedMessageListenerContainer();
    }

    @Override
    public DefaultMessageListenerContainer createListenerContainer(JmsListenerEndpoint endpoint) {
    	RateLimitedMessageListenerContainer listenerContainer = (RateLimitedMessageListenerContainer) super.createListenerContainer(endpoint);
        listenerContainer.setPermitsPerSecond(0.1);
        return listenerContainer;
    }
}