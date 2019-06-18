package com.cgi.eoss.osiris.search.creodias;

import com.cgi.eoss.osiris.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "osiris.search.creodias.enabled", havingValue = "true", matchIfMissing = true)
public class CreoDIASSearchConfiguration {

    @Value("${osiris.search.creodias.baseUrl:https://finder.creodias.eu/resto/}")
    private String baseUrl;
    @Value("${osiris.search.creodias.username:}")
    private String username;
    @Value("${osiris.search.creodias.password:}")
    private String password;
    @Value("${osiris.search.creodias.priority:0}")
    private int priority;

    @Bean
    public CreoDIASSearchProvider creodiasSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
        return new CreoDIASSearchProvider(priority,
                CreoDIASSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                externalProductService);
    }

}
