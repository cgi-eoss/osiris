package com.cgi.eoss.osiris.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class OsirisWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;

    public OsirisWebAuthenticationDetailsSource(String emailRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        OsirisWebAuthenticationDetails details = new OsirisWebAuthenticationDetails(context);
        details.setUserEmail(context.getHeader(emailRequestHeader));
        return details;
    }

}
