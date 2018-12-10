package com.cgi.eoss.osiris.model.converters;

import com.cgi.eoss.osiris.model.OsirisServiceResources;
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
public class OsirisServiceResourcesYamlConverter implements AttributeConverter<OsirisServiceResources, String> {

    private static final TypeReference<OsirisServiceResources> OSIRIS_SERVICE_RESOURCES = new TypeReference<OsirisServiceResources>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public OsirisServiceResourcesYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(OsirisServiceResources attribute) {
        return toYaml(attribute);
    }

    @Override
    public OsirisServiceResources convertToEntityAttribute(String dbData) {
        return dbData!=null?fromYaml(dbData):null;
    }

    public static String toYaml(OsirisServiceResources osirisServiceResources) {
        try {
            return MAPPER.writeValueAsString(osirisServiceResources);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert OsirisServiceResources to YAML string: {}", osirisServiceResources);
            throw new IllegalArgumentException("Could not convert OsirisServiceResources to YAML string", e);
        }
    }

    public static OsirisServiceResources fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, OSIRIS_SERVICE_RESOURCES);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to OsirisServiceResources: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to OsirisServiceResources", e);
        }
    }

}

