package com.cgi.eoss.osiris.clouds.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Node {
    private final String id;
    private final String name;
    private final String tag;
    private final long creationEpochSecond;
    private final String dockerEngineUrl;
    private final String ipAddress;
}
