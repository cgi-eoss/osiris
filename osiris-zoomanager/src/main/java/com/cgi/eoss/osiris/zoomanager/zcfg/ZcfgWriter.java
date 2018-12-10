package com.cgi.eoss.osiris.zoomanager.zcfg;

import com.cgi.eoss.osiris.model.OsirisServiceDescriptor;

import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Utility to generate ZOO-Project zcfg files from {@link OsirisServiceDescriptor} objects.</p>
 */
public interface ZcfgWriter {

    /**
     * <p>Serialise the given {@link OsirisServiceDescriptor} object in zcfg format, for use by zoo_loader.cgi.</p>
     *
     * @param svc The service data to be serialised.
     * @param zcfg The path to the file to be generated. This will overwrite any existing file in the location.
     */
    void generateZcfg(OsirisServiceDescriptor svc, Path zcfg);

    /**
     * <p>Serialise the given {@link OsirisServiceDescriptor} objects in zcfg format, for use by zoo_loader.cgi.</p>
     *
     * @param services The service data to be serialised.
     * @param zcfgBasePath The path to a directory to contian the zcfg files. This will remove any existing .zcfg files
     * in the location.
     */
    void generateZcfgs(Set<OsirisServiceDescriptor> services, Path zcfgBasePath);

}
