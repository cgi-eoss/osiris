package com.cgi.eoss.osiris.api.controllers;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.osiris.model.ApiKey;
import com.cgi.eoss.osiris.persistence.service.ApiKeyDataService;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.sun.jersey.core.util.Base64;

import lombok.extern.log4j.Log4j2;

/**
 * <p>Functionality for generating OSIRIS API keys</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/apiKeys")
@Log4j2
public class ApiKeysApi {

	private final OsirisSecurityService osirisSecurityService;
    private final ApiKeyDataService apiKeyDataService;

    @Autowired
    public ApiKeysApi(ApiKeyDataService apiKeyDataService, OsirisSecurityService osirisSecurityService) {
        this.apiKeyDataService = apiKeyDataService;
        this.osirisSecurityService = osirisSecurityService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') == false")
    public ResponseEntity<String> generate() {
    	if (apiKeyDataService.getByOwner(osirisSecurityService.getCurrentUser()) != null){
    		return new ResponseEntity<String>(HttpStatus.CONFLICT);
    	}
    	
    	String apiKeyString = generateApiKey(256);
    	String encryptedApiKeyString;
		try {
			encryptedApiKeyString = encryptApiKey(apiKeyString);
		} catch (NoSuchAlgorithmException e) {
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
    	ApiKey apiKey = new ApiKey(encryptedApiKeyString);
    	osirisSecurityService.updateOwnerWithCurrentUser(apiKey);
    	apiKeyDataService.save(apiKey);
    	return new ResponseEntity<String>(apiKeyString, HttpStatus.CREATED);
    }
    
    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') == false")
    public ResponseEntity<Boolean> exists() {
    	ApiKey apiKey = apiKeyDataService.getByOwner(osirisSecurityService.getCurrentUser());
    	return new ResponseEntity<Boolean>(apiKey != null , HttpStatus.OK);
    }
    
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') == false")
    public ResponseEntity<Void> delete() {
    	ApiKey apiKey = apiKeyDataService.getByOwner(osirisSecurityService.getCurrentUser());
    	if (apiKey  != null){
    		apiKeyDataService.delete(apiKey);
    	}
    	return new ResponseEntity<Void>(HttpStatus.OK);
    }
    
    @PostMapping("/regenerate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') == false")
    public ResponseEntity<String> regenerate() {
    	ApiKey apiKey = apiKeyDataService.getByOwner(osirisSecurityService.getCurrentUser());
    	if (apiKey  != null){
    		String apiKeyString = generateApiKey(256);
    		//TODO SHA-1 the api key 
    		String encryptedApiKeyString;
			try {
				encryptedApiKeyString = encryptApiKey(apiKeyString);
			} catch (NoSuchAlgorithmException e) {
				return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
    		apiKey.setApiKeyString(encryptedApiKeyString);
    		apiKeyDataService.save(apiKey);
    		return new ResponseEntity<String>(apiKeyString, HttpStatus.OK);
    	}
    	else {
    		return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    	}
    }
 
    private String generateApiKey(final int keyLen)  {
    	SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keyLen/8];
        random.nextBytes(bytes);
        return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    }

	private String encryptApiKey(String password) throws NoSuchAlgorithmException {
		return "{SHA}" + new String(Base64.encode(java.security.MessageDigest.getInstance("SHA1").digest(password.getBytes())));
	}
	
}
