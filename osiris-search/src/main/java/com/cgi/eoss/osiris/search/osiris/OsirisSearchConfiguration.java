package com.cgi.eoss.osiris.search.osiris;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.catalogue.resto.RestoService;
import com.cgi.eoss.osiris.persistence.service.CollectionDataService;
import com.cgi.eoss.osiris.persistence.service.OsirisFileDataService;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "osiris.search.osiris.enabled", havingValue = "true", matchIfMissing = true)
public class OsirisSearchConfiguration {

    @Value("${osiris.search.osiris.baseUrl:http://osiris-resto/resto}")
    private String baseUrl;
    @Value("${osiris.search.osiris.username:}")
    private String username;
    @Value("${osiris.search.osiris.password:}")
    private String password;

    @Bean
    public OsirisSearchProvider osirisSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, OsirisFileDataService osirisFileDataService, OsirisSecurityService securityService, CollectionDataService collectionDataService) {
        return new OsirisSearchProvider(0,
                OsirisSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                catalogueService,
                restoService,
                osirisFileDataService,
                securityService,
                collectionDataService);
    }

}
