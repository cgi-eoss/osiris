package com.cgi.eoss.osiris.model.projections;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.osiris.model.OsirisServiceTemplateFile;

/**
 * <p>Default JSON projection for embedded {@link OsirisServiceTemplateFile}s. Embeds the service as a ShortServiceTemplate, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceTemplateFile", types = {OsirisServiceTemplateFile.class})
public interface ShortOsirisServiceTemplateFile extends Identifiable<Long> {
    ShortOsirisServiceTemplate getServiceTemplate();
    String getFilename();
    boolean isExecutable();
}
