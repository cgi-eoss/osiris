package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.LocalServiceLauncher;
import com.cgi.eoss.osiris.rpc.RestartSystematicProcessingParams;
import com.cgi.eoss.osiris.rpc.TerminateSystematicProcessingParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
@RequestMapping("/systematicProcessings")
@Transactional
@Log4j2
public class SystematicProcessingsApiExtension {


    private LocalServiceLauncher localServiceLauncher;
    
	@Autowired
    public SystematicProcessingsApiExtension(LocalServiceLauncher localServiceLauncher, SystematicProcessingDataService systematicProcessingDataService) {
	    this.localServiceLauncher = localServiceLauncher;
	}

   
    @PostMapping("/{systematicProcessingId}/terminate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#systematicProcessing, 'write')")
    public ResponseEntity terminate(@ModelAttribute("systematicProcessingId") SystematicProcessing systematicProcessing) {
    	localServiceLauncher.terminateSystematicProcessing(TerminateSystematicProcessingParams.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{systematicProcessingId}/restart")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#systematicProcessing, 'write')")
    public ResponseEntity restart(@ModelAttribute("systematicProcessingId") SystematicProcessing systematicProcessing) {
        localServiceLauncher.restartSystematicProcessing(RestartSystematicProcessingParams.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        return ResponseEntity.noContent().build();
    }

}