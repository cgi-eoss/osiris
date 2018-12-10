package com.cgi.eoss.osiris.zoomanager.service;

import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>{@link OsirisServiceDescriptorHandler} implementation to produce and consume YAML-format OSIRIS service descriptor
 * files.</p>
 */
@Component
public class YamlOsirisServiceDescriptorHandler implements OsirisServiceDescriptorHandler {

    private final ObjectMapper mapper;

    public YamlOsirisServiceDescriptorHandler() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public OsirisServiceDescriptor readFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return mapper.readValue(reader, OsirisServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor " + file, e);
        }
    }

    @Override
    public OsirisServiceDescriptor read(InputStream stream) {
        try (Reader reader = new InputStreamReader(stream)) {
            return mapper.readValue(reader, OsirisServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor from " + stream, e);
        }
    }

    @Override
    public void writeFile(OsirisServiceDescriptor svc, Path file) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + file, e);
        }
    }

    @Override
    public void write(OsirisServiceDescriptor svc, OutputStream stream) {
        try (Writer writer = new OutputStreamWriter(stream)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + stream, e);
        }
    }

}
