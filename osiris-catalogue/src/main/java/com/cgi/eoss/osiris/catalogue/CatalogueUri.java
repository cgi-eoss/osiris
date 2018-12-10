package com.cgi.eoss.osiris.catalogue;

import org.apache.commons.text.StrSubstitutor;

import java.net.URI;
import java.util.Map;

public enum CatalogueUri {
    REFERENCE_DATA("osiris://refData/${ownerId}/${filename}"),
    OUTPUT_PRODUCT("osiris://outputProduct/${jobId}/${filename}"),
    DATABASKET("osiris://databasket/${id}");

    private final String internalUriPattern;

    CatalogueUri(String internalUriPattern) {
        this.internalUriPattern = internalUriPattern;
    }

    public URI build(Map<String, String> values) {
        return URI.create(StrSubstitutor.replace(internalUriPattern, values));
    }
}
