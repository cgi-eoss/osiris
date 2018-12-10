package com.cgi.eoss.osiris.model.converters;

import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@Log4j2
public class OsirisServiceDescriptorYamlConverter implements AttributeConverter<OsirisServiceDescriptor, String> {

    private static final TypeReference<OsirisServiceDescriptor> OSIRIS_SERVICE_DESCRIPTOR = new TypeReference<OsirisServiceDescriptor>() { };

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public OsirisServiceDescriptorYamlConverter() {
    }

    @Override
    public String convertToDatabaseColumn(OsirisServiceDescriptor attribute) {
        return toYaml(attribute);
    }

    @Override
    public OsirisServiceDescriptor convertToEntityAttribute(String dbData) {
        return fromYaml(dbData);
    }

    public static String toYaml(OsirisServiceDescriptor osirisServiceDescriptor) {
        try {
            return MAPPER.writeValueAsString(osirisServiceDescriptor);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to convert OsirisServiceDescriptor to YAML string: {}", osirisServiceDescriptor);
            throw new IllegalArgumentException("Could not convert OsirisServiceDescriptor to YAML string", e);
        }
    }

    public static OsirisServiceDescriptor fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, OSIRIS_SERVICE_DESCRIPTOR);
        } catch (IOException e) {
            LOG.error("Failed to convert YAML string to OsirisServiceDescriptor: {}", yaml);
            throw new IllegalArgumentException("Could not convert YAML string to OsirisServiceDescriptor", e);
        }
    }

}

