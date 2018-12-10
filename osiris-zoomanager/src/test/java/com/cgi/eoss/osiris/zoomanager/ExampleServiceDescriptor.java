package com.cgi.eoss.osiris.zoomanager;

import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public final class ExampleServiceDescriptor {
    private static final List<OsirisServiceDescriptor.Parameter> INPUTS = ImmutableList.of(
            OsirisServiceDescriptor.Parameter.builder()
                    .id("inputfile")
                    .title("Input File 1")
                    .description("The input data file")
                    .minOccurs(1)
                    .maxOccurs(1)
                    .data(OsirisServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string")
                            .build())
                    .build()
    );
    private static final List<OsirisServiceDescriptor.Parameter> OUTPUTS = ImmutableList.of(
            OsirisServiceDescriptor.Parameter.builder()
                    .id("result")
                    .title("URL to service output")
                    .description("see title")
                    .data(OsirisServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string").build())
                    .build()
    );

    private static final OsirisServiceDescriptor EXAMPLE_SVC = OsirisServiceDescriptor.builder()
            .id("TestService1")
            .title("Test Service for ZCFG Generation")
            .description("This service tests the OSIRIS automatic zcfg file generation")
            .version("1.0")
            .serviceProvider("osiris_service_wrapper")
            .serviceType("python")
            .storeSupported(false)
            .statusSupported(false)
            .dataInputs(INPUTS)
            .dataOutputs(OUTPUTS)
            .build();


    public static OsirisServiceDescriptor getExampleSvc() {
        return EXAMPLE_SVC;
    }
}
