package com.cgi.eoss.osiris.zoomanager.service;

import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * <p>Utility to read and write {@link OsirisServiceDescriptor} objects.</p>
 */
public interface OsirisServiceDescriptorHandler {

    /**
     * <p>Deserialise the given OSIRIS service descriptor from the given file, according to the interface
     * implementation.</p>
     *
     * @param file The file to be read.
     * @return The OSIRIS service as described in the given file.
     */
    OsirisServiceDescriptor readFile(Path file);

    /**
     * <p>Deserialise the given OSIRIS service descriptor from the given stream, according to the interface
     * implementation.</p>
     *
     * @param stream The byte stream representing a service descriptor.
     * @return The OSIRIS service as described in the given stream.
     */
    OsirisServiceDescriptor read(InputStream stream);

    /**
     * <p>Serialise the given OSIRIS service descriptor to the given file, according to the interface implementation.</p>
     *
     * @param svc The OSIRIS service descriptor to be serialised.
     * @param file The target file to write.
     */
    void writeFile(OsirisServiceDescriptor svc, Path file);

    /**
     * <p>Serialise the given OSIRIS service descriptor to the given stream, according to the interface
     * implementation.</p>
     *
     * @param svc The OSIRIS service descriptor to be serialised.
     * @param stream The destination for the byte stream.
     */
    void write(OsirisServiceDescriptor svc, OutputStream stream);

}
