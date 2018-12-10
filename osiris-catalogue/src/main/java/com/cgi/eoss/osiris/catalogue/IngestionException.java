package com.cgi.eoss.osiris.catalogue;

public class IngestionException extends CatalogueException {
    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(Throwable cause) {
        super(cause);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
