package com.cgi.eoss.osiris.harvesters.wps;

public class WpsException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -5389004574271439948L;

    public WpsException(String message) {
        super(message);
    }
    
    public WpsException(Throwable cause) {
        super(cause);
    }
    
    public WpsException(String message, Throwable cause) {
        super(message, cause);
    }
}
