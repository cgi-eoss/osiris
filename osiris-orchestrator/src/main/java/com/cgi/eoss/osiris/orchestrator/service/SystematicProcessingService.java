package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.model.SystematicProcessing;
import com.cgi.eoss.osiris.persistence.service.SystematicProcessingDataService;
import com.cgi.eoss.osiris.rpc.GrpcUtil;
import com.cgi.eoss.osiris.rpc.JobParam;
import com.cgi.eoss.osiris.rpc.SystematicProcessingRequest;
import com.cgi.eoss.osiris.rpc.SystematicProcessingResponse;
import com.cgi.eoss.osiris.rpc.SystematicProcessingServiceGrpc;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

@Service
@Log4j2
@GRpcService
public class SystematicProcessingService extends SystematicProcessingServiceGrpc.SystematicProcessingServiceImplBase {

    private final SystematicProcessingDataService systematicProcessingDataService;

    public SystematicProcessingService(SystematicProcessingDataService systematicProcessingDataService) {
        this.systematicProcessingDataService = systematicProcessingDataService;
    }

    @Override
    public void launch(SystematicProcessingRequest request, StreamObserver<SystematicProcessingResponse> responseObserver) {
        ListMultimap<String, String> searchParams = request.getSearchParameterList().stream()
                .collect(Multimaps.flatteningToMultimap(JobParam::getParamName,
                        sp -> sp.getParamValueList().stream(),
                        MultimapBuilder.linkedHashKeys().arrayListValues()::build));

        //Put the necessary parameters for systematic processing
        searchParams.put("sortOrder", "ascending");
        searchParams.put("sortParam", "updated");

        Instant dateStart = searchParams.get("productDateStart").stream()
                .map(Instant::parse)
                .findFirst()
                .orElse(Instant.now());
        searchParams.replaceValues("productDateStart", Collections.singleton(dateStart.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)));

        SystematicProcessing systematicProcessing = systematicProcessingDataService.buildNew(UUID.randomUUID().toString(),
                request.getUserId(), request.getServiceId(), request.getJobConfigLabel(), request.getSystematicParameter(),
                GrpcUtil.paramsListToMap(request.getInputList()), searchParams, LocalDateTime.ofInstant(dateStart, ZoneOffset.UTC));

        LOG.info("Systematic processing {} saved", systematicProcessing.getId());

        responseObserver.onNext(SystematicProcessingResponse.newBuilder().setSystematicProcessingId(systematicProcessing.getId()).build());
        responseObserver.onCompleted();
    }
}
