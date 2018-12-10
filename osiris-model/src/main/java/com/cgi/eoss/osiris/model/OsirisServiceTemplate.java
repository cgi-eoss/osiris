package com.cgi.eoss.osiris.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.cgi.eoss.osiris.model.OsirisService.Type;
import com.cgi.eoss.osiris.model.converters.OsirisServiceDescriptorYamlConverter;
import com.cgi.eoss.osiris.model.converters.OsirisServiceResourcesYamlConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = {"id", "templateFiles"})
@ToString(exclude = {"serviceDescriptor", "templateFiles"})
@Table(name = "osiris_service_templates",
        indexes = {@Index(name = "osiris_service_templates_name_idx", columnList = "name"), @Index(name = "osiris_service_templates_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class OsirisServiceTemplate implements OsirisEntityWithOwner<OsirisServiceTemplate>, Searchable {

    /**
     * <p>Internal unique identifier of the service template.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique name of the service template, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * <p>Human-readable descriptive summary of the service.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The type of the template, e.g. 'processor' or 'application'.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type = Type.PROCESSOR;

    /**
     * <p>The user owning the template, typically the template creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The partial (template) definition of the WPS service.</p>
     */
    @Lob
    @Convert(converter = OsirisServiceDescriptorYamlConverter.class)
    @Column(name = "wps_descriptor")
    private OsirisServiceDescriptor serviceDescriptor;

    /**
     * <p>The files required to build this service's docker image.</p>
     */
    @OneToMany(mappedBy = "serviceTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<OsirisServiceTemplateFile> templateFiles = new HashSet<>();

    @Lob
    @Convert(converter = OsirisServiceResourcesYamlConverter.class)
    @Column(name = "required_resources")
    OsirisServiceResources requiredResources;
    
    /**
     * <p>Create a new Template with the minimum required parameters.</p>
     *
     * @param name Name of the template.
     * @param owner The user owning the template.
     */
    public OsirisServiceTemplate(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public int compareTo(OsirisServiceTemplate o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

    public void setTemplateFiles(Set<OsirisServiceTemplateFile> templateFiles) {
    	templateFiles.forEach(f -> f.setServiceTemplate(this));
        this.templateFiles = templateFiles;
    }
}
