package com.cgi.eoss.osiris.util;

import lombok.Getter;

public enum OsirisFormField {
    NEW_PROJECT_NAME("#item-dialog[aria-label='Create Project dialog'] md-input-container input");

    @Getter
    private final String selector;

    OsirisFormField(String selector) {
        this.selector = selector;
    }

}
