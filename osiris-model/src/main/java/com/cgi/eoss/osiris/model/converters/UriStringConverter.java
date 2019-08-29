package com.cgi.eoss.osiris.model.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;

@Converter
public class UriStringConverter implements AttributeConverter<URI, String> {
    @Override
    public String convertToDatabaseColumn(URI attribute) {
    	return attribute != null? attribute.toString(): null;
    }

    @Override
    public URI convertToEntityAttribute(String dbData) {
        return dbData != null? URI.create(dbData): null;
    }
}
