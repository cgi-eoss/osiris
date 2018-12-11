package com.cgi.eoss.osiris.api.security;

import com.cgi.eoss.osiris.api.security.basic.HttpBasicRequestHeaderAuthenticationFilter;
import com.cgi.eoss.osiris.security.OsirisUserDetailsService;
import com.cgi.eoss.osiris.security.OsirisWebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalAuthentication
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApiSecurityConfig {
    @Bean
    @ConditionalOnProperty(value = "osiris.api.security.mode", havingValue = "SSO")
    public WebSecurityConfigurerAdapter ssoWebSecurityConfigurerAdapter(
            @Value("${osiris.api.security.username-request-header:REMOTE_USER}") String usernameRequestHeader,
            @Value("${osiris.api.security.email-request-header:REMOTE_EMAIL}") String emailRequestHeader) {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // Extracts the shibboleth user id from the request
                RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager());
                filter.setPrincipalRequestHeader(usernameRequestHeader);
                filter.setAuthenticationDetailsSource(new OsirisWebAuthenticationDetailsSource(emailRequestHeader));

                // Handles any authentication exceptions, and translates to a simple 403
                // There is no login redirection as we are expecting pre-auth
                ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

                httpSecurity
                        .addFilterBefore(exceptionTranslationFilter, RequestHeaderAuthenticationFilter.class)
                        .addFilter(filter)
                        .authorizeRequests()
                        .anyRequest().authenticated();
                httpSecurity
                        .csrf().disable();
                httpSecurity
                        .cors();
                httpSecurity
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "osiris.api.security.mode", havingValue = "HTTP_BASIC")
    public WebSecurityConfigurerAdapter httpBasicWebSecurityConfigurerAdapter() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // Extracts (pre-authenticated) HTTP Basic auth information from the request headers
                HttpBasicRequestHeaderAuthenticationFilter filter = new HttpBasicRequestHeaderAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager());
                filter.setAuthenticationDetailsSource(new OsirisWebAuthenticationDetailsSource(""));

                // Handles any authentication exceptions, and translates to a simple 403
                // There is no login redirection as we are expecting pre-auth
                ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

                httpSecurity
                        .addFilterBefore(exceptionTranslationFilter, RequestHeaderAuthenticationFilter.class)
                        .addFilter(filter)
                        .authorizeRequests()
                        .anyRequest().authenticated();
                httpSecurity
                        .csrf().disable();
                httpSecurity
                        .cors();
                httpSecurity
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

            }
        };
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, OsirisUserDetailsService osirisUserDetailsService) {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(osirisUserDetailsService);
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler(AclPermissionEvaluator aclPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(aclPermissionEvaluator);
        return expressionHandler;
    }

}
