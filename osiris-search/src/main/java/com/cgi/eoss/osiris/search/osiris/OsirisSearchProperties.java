package com.cgi.eoss.osiris.search.osiris;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
class OsirisSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;

}