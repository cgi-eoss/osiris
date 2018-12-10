package com.cgi.eoss.osiris.util;

import lombok.Getter;

public enum OsirisPage {
    EXPLORER("/app/");

    @Getter
    private final String url;

    OsirisPage(String url) {
        this.url = url;
    }

}
