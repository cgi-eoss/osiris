package com.cgi.eoss.osiris.util;

import lombok.Getter;

public enum OsirisPanel {
    SEARCH("#sidenav i[uib-tooltip='Search']");

    @Getter
    private final String selector;

    OsirisPanel(String selector) {
        this.selector = selector;
    }

}
