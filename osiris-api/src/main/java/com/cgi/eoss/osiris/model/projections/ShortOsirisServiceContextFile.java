package com.cgi.eoss.osiris.model.projections;

import com.cgi.eoss.osiris.model.OsirisServiceContextFile;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link OsirisServiceContextFile}s. Embeds the service as a ShortService, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceFile", types = {OsirisServiceContextFile.class})
public interface ShortOsirisServiceContextFile extends Identifiable<Long> {
    ShortOsirisService getService();
    String getFilename();
    boolean isExecutable();
}
