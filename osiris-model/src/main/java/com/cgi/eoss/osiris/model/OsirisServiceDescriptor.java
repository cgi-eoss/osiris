package com.cgi.eoss.osiris.model;

import com.cgi.eoss.osiris.model.converters.OsirisServiceDescriptorYamlConverter;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
/**
 * <p>The detailed service configuration required to complete a WPS service definition file.</p>
 * <p>All fields are broadly aligned with the official WPS spec as configured via ZOO-Project zcfg files.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OsirisServiceDescriptor {

    private String id;

    private String title;

    private String description;

    private String version;

    private boolean storeSupported;

    private boolean statusSupported;

    private String serviceType;

    private String serviceProvider;

    private List<Parameter> dataInputs;

    private List<Parameter> dataOutputs;

    public String toYaml() {
        return OsirisServiceDescriptorYamlConverter.toYaml(this);
    }

    public static OsirisServiceDescriptor fromYaml(String yaml) {
        return OsirisServiceDescriptorYamlConverter.fromYaml(yaml);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {

        public enum DataNodeType {
            LITERAL, COMPLEX, BOUNDING_BOX
        }

        private String id;

        private String title;

        private String description;

        private int minOccurs;

        private int maxOccurs;

        private DataNodeType data;

        @JsonInclude(Include.NON_NULL)
        private String timeRegexp;
        
        private Map<String, String> defaultAttrs;

        @Singular
        private List<Map<String, String>> supportedAttrs;

    }

}
