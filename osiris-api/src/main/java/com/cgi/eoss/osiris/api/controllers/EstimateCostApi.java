package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import javax.servlet.http.HttpServletRequest;
import com.cgi.eoss.osiris.costing.CostingService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.search.api.SearchFacade;
import com.cgi.eoss.osiris.search.api.SearchParameters;
import com.cgi.eoss.osiris.search.api.SearchResults;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>Functionality for users to retrieve cost estimations for OSIRIS activities.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/estimateCost")
@Log4j2
public class EstimateCostApi {

    private final CostingService costingService;
    private final OsirisSecurityService osirisSecurityService;
    private final SearchFacade searchFacade;

    @Autowired
    public EstimateCostApi(CostingService costingService, OsirisSecurityService osirisSecurityService, SearchFacade searchFacade) {
        this.costingService = costingService;
        this.osirisSecurityService = osirisSecurityService;
        this.searchFacade = searchFacade;
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity estimateJobConfigCost(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        int walletBalance = osirisSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateJobCost(jobConfig);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }

    @GetMapping("/download/{osirisFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#osirisFile, 'read')")
    public ResponseEntity estimateDownloadCost(@ModelAttribute("osirisFileId") OsirisFile osirisFile) {
        int walletBalance = osirisSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateDownloadCost(osirisFile);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }
    
    @PostMapping("/systematic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfigTemplate.id == null) or hasPermission(#jobConfigTemplate, 'read')")
    public ResponseEntity estimateSystematicCost(HttpServletRequest request, @RequestBody JobConfig jobConfigTemplate) throws InterruptedException, JsonParseException, JsonMappingException, JsonProcessingException, IOException {
        int walletBalance = osirisSecurityService.getCurrentUser().getWallet().getBalance();
        int singleRunCost =  costingService.estimateSingleRunJobCost(jobConfigTemplate);
        
        Map<String, String[]> requestParameters = request.getParameterMap();
        ListMultimap<String, String> queryParameters = ArrayListMultimap.create(); 
        for (Map.Entry<String, String[]> entry: requestParameters.entrySet()) {
            queryParameters.putAll(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        
        //Run the query to assess the rough number of results for last month
        LocalDateTime start = LocalDateTime.now().minusDays(31);
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        
        String productDateStart = ZonedDateTime.of(start, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String productDateEnd = ZonedDateTime.of(end, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        queryParameters.replaceValues("productDateStart", Arrays.asList(new String[] {productDateStart}));
        queryParameters.replaceValues("productDateEnd", Arrays.asList(new String[] {productDateEnd}));
        
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setParameters(queryParameters);
        searchParameters.setRequestUrl(HttpUrl.parse("http://osiris-estimate"));
        
        SearchResults results = searchFacade.search(searchParameters);
        
        //Overwrite query params using last month
        int monthlyCost =  (int) results.getPage().getTotalElements() * singleRunCost;
        
        
        return ResponseEntity
                .status(monthlyCost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(monthlyCost).currentWalletBalance(walletBalance).build());

    }

    @Data
    @Builder
    private static class CostEstimationResponse {
        int estimatedCost;
        int currentWalletBalance;
    }

}
