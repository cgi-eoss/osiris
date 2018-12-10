package com.cgi.eoss.osiris.model.converters;

import com.cgi.eoss.osiris.model.OsirisServiceDockerBuildInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;

@Converter
@Log4j2
public class OsirisServiceDockerBuildInfoYamlConverter implements AttributeConverter<OsirisServiceDockerBuildInfo, String> {

    private static final TypeReference<OsirisServiceDockerBuildInfo> OSIRIS_SERVICE_DOCKER_BUILD_INFO = new TypeReference<OsirisServiceDockerBuildInfo>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public OsirisServiceDockerBuildInfoYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(OsirisServiceDockerBuildInfo attribute) {
        return toYaml(attribute);
    }

    @Override
    public OsirisServiceDockerBuildInfo convertToEntityAttribute(String dbData) {
        return dbData!=null?fromYaml(dbData):null;
    }

    public static String toYaml(OsirisServiceDockerBuildInfo osirisServiceDockerBuildInfo) {
        try {
            return MAPPER.writeValueAsString(osirisServiceDockerBuildInfo);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert OsirisServiceDockerBuildInfo to YAML string: {}", osirisServiceDockerBuildInfo);
            throw new IllegalArgumentException("Could not convert OsirisServiceDockerBuildInfo to YAML string", e);
        }
    }

    public static OsirisServiceDockerBuildInfo fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, OSIRIS_SERVICE_DOCKER_BUILD_INFO);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to OsirisServiceDockerBuildInfo: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to OsirisServiceDockerBuildInfo", e);
        }
    }

}

