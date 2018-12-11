package com.cgi.eoss.osiris.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class OsirisWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;

    public OsirisWebAuthenticationDetailsSource(String emailRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        OsirisWebAuthenticationDetails details = new OsirisWebAuthenticationDetails(context);
        Optional.ofNullable(context.getHeader(emailRequestHeader)).ifPresent(details::setUserEmail);
        return details;
    }

}
