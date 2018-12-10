package com.cgi.eoss.osiris.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OsirisAccess {
    private final boolean published;
    private final boolean publishRequested;
    private final OsirisPermission currentLevel;
}
