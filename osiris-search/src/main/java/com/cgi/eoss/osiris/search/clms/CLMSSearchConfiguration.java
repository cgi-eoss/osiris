package com.cgi.eoss.osiris.search.clms;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cgi.eoss.osiris.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

@Configuration
@ConditionalOnProperty(value = "osiris.search.clms.enabled", havingValue = "true", matchIfMissing = true)
public class CLMSSearchConfiguration {

    @Value("${osiris.search.clms.baseUrl:https://land.copernicus.vgt.vito.be/openSearch/}")
    private String baseUrl;
 
    @Bean
    public CLMSSearchProvider CLMSSearchProvider(OkHttpClient httpClient, ExternalProductDataService externalProductDataService) {
        return new CLMSSearchProvider(HttpUrl.parse(baseUrl), httpClient, new XmlMapper(), new ObjectMapper());
    }

}
