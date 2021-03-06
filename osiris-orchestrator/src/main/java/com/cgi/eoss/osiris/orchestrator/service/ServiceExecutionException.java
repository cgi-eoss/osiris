package com.cgi.eoss.osiris.orchestrator.service;

/**
 * <p>Signals that an exception occurred in the execution of an OSIRIS Service.</p>
 */
public class ServiceExecutionException extends RuntimeException {

    /**
     * <p>Constructs a new service execution exception with the given detail message.</p>
     *
     * @param message
     */
    public ServiceExecutionException(String message) {
        super(message);
    }

}
