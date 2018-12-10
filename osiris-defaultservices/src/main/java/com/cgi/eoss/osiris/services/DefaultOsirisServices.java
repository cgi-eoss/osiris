package com.cgi.eoss.osiris.services;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.lambda.Unchecked;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.cgi.eoss.osiris.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;

/**
 * <p>Access to the default OSIRIS service collection as Java objects.</p>
 * <p>The services are read from classpath resources baked in at compile-time, and may be used to install or restore the
 * default service set during runtime.</p>
 */
@Component
@Log4j2
public class DefaultOsirisServices {

    private static final Map<String, OsirisService.Type> DEFAULT_SERVICES = ImmutableMap.<String, OsirisService.Type>builder()
//            .put("ForestChangeS2", OsirisService.Type.PROCESSOR)
//            .put("LandCoverS1", OsirisService.Type.PROCESSOR)
//            .put("LandCoverS2", OsirisService.Type.PROCESSOR)
//            .put("S1Biomass", OsirisService.Type.PROCESSOR)
//            .put("VegetationIndices", OsirisService.Type.PROCESSOR)
            .put("Monteverdi", OsirisService.Type.APPLICATION)
            .put("QGIS", OsirisService.Type.APPLICATION)
            .put("SNAP", OsirisService.Type.APPLICATION)
            .build();

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static Set<OsirisService> getDefaultServices() {
        return DEFAULT_SERVICES.keySet().stream().map(DefaultOsirisServices::importDefaultService).collect(Collectors.toSet());
    }

    private static OsirisService importDefaultService(String serviceId) {
        try {
            OsirisService service = new OsirisService(serviceId, User.DEFAULT, "osiris/" + serviceId.toLowerCase());
            service.setLicence(OsirisService.Licence.OPEN);
            service.setStatus(OsirisService.Status.AVAILABLE);
            service.setServiceDescriptor(getServiceDescriptor(service));
            service.setDescription(service.getServiceDescriptor().getDescription());
            service.setType(DEFAULT_SERVICES.get(serviceId));
            service.setContextFiles(getServiceContextFiles(service));
            return service;
        } catch (IOException e) {
            throw new RuntimeException("Could not load default OSIRIS Service " + serviceId, e);
        }
    }

    private static OsirisServiceDescriptor getServiceDescriptor(OsirisService service) throws IOException {
        try (Reader reader = new InputStreamReader(DefaultOsirisServices.class.getResourceAsStream("/" + service.getName() + ".yaml"))) {
            return YAML_MAPPER.readValue(reader, OsirisServiceDescriptor.class);
        }
    }

    private static Set<OsirisServiceContextFile> getServiceContextFiles(OsirisService service) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(DefaultOsirisServices.class.getClassLoader());
        Resource baseDir = resolver.getResource("classpath:/" + service.getName());
        Set<Resource> resources = ImmutableSet.copyOf(resolver.getResources("classpath:/" + service.getName() + "/**/*"));

        return resources.stream()
                .filter(Unchecked.predicate(r -> !r.getURI().toString().endsWith("/")))
                .map(Unchecked.function(r -> OsirisServiceContextFile.builder()
                        .service(service)
                        .filename(getRelativeFilename(r, baseDir))
                        .executable(r.getFilename().endsWith(".sh"))
                        .content(new String(ByteStreams.toByteArray(r.getInputStream())))
                        .build()
                ))
                .collect(Collectors.toSet());
    }

    private static String getRelativeFilename(Resource resource, Resource baseDir) throws IOException {
        return resource.getURI().toString().replaceFirst(baseDir.getURI().toString() + "/", "");
    }

}
