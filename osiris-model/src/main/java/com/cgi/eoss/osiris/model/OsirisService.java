package com.cgi.eoss.osiris.model;

import com.cgi.eoss.osiris.model.converters.OsirisServiceDescriptorYamlConverter;
import com.cgi.eoss.osiris.model.converters.OsirisServiceDockerBuildInfoYamlConverter;
import com.cgi.eoss.osiris.model.converters.OsirisServiceResourcesYamlConverter;
import com.cgi.eoss.osiris.model.converters.UriStringConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude = {"id", "contextFiles"})
@ToString(exclude = {"serviceDescriptor", "contextFiles"})
@Table(name = "osiris_services",
        indexes = {@Index(name = "osiris_services_name_idx", columnList = "name"), @Index(name = "osiris_services_owner_idx", columnList = "owner")},
        uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@NoArgsConstructor
@Entity
public class OsirisService implements OsirisEntityWithOwner<OsirisService>, Searchable {

    private static final String DATA_SOURCE_NAME_PREFIX = "OSIRIS_";

    /**
     * <p>Internal unique identifier of the service.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>Unique name of the service, assigned by the owner.</p>
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * <p>Human-readable descriptive summary of the service.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The type of the service, e.g. 'processor' or 'application'.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type = Type.PROCESSOR;

    /**
     * <p>The user owning the service, typically the service creator.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>The docker container identifier to be used for running the service. It is expected that this is already
     * available on a worker.</p>
     */
    @Column(name = "docker_tag", nullable = false)
    private String dockerTag;
    
    /**
     * <p>If the proxy path has to be stripped prior to be forwarded to this service. </p>
     */
    @Column(name = "strip_proxy_path")
    private boolean stripProxyPath = true;

    /**
     * <p>Usage restriction of the service, e.g. 'open' or 'restricted'.</p>
     */
    @Column(name = "licence", nullable = false)
    @Enumerated(EnumType.STRING)
    private Licence licence = Licence.OPEN;

    /**
     * <p>Service availability status.</p>
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_DEVELOPMENT;

    /**
     * <p>The full definition of the WPS service, used to build ZOO-Kernel configuration.</p>
     */
    @Lob
    @Convert(converter = OsirisServiceDescriptorYamlConverter.class)
    @Column(name = "wps_descriptor")
    private OsirisServiceDescriptor serviceDescriptor;

    /**
     * <p>The files required to build this service's docker image.</p>
     */
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<OsirisServiceContextFile> contextFiles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="user_mount_id", table = "osiris_services_mounts")
    @Column(name="target_mount_path", table = "osiris_services_mounts")
    @CollectionTable(name="osiris_services_mounts", joinColumns=@JoinColumn(name="osiris_service_id"))
    private Map<Long, String> additionalMounts = new HashMap<>();
    
    @Lob
    @Convert(converter = OsirisServiceDockerBuildInfoYamlConverter.class)
    @Column(name = "docker_build_info")
    OsirisServiceDockerBuildInfo dockerBuildInfo;
    
    @Lob
    @Convert(converter = OsirisServiceResourcesYamlConverter.class)
    @Column(name = "required_resources")
    OsirisServiceResources requiredResources;
    
    
    @Column(name = "external_uri")
    @Convert(converter = UriStringConverter.class)
    private URI externalServiceUri;
    
    /**
     * <p>Create a new Service with the minimum required parameters.</p>
     *
     * @param name Name of the service.
     * @param owner The user owning the service.
     */
    public OsirisService(String name, User owner, String dockerTag) {
        this.name = name;
        this.owner = owner;
        this.dockerTag = dockerTag;
    }

    @Override
    public int compareTo(OsirisService o) {
        return ComparisonChain.start().compare(name, o.name).result();
    }

    public void setContextFiles(Set<OsirisServiceContextFile> contextFiles) {
        contextFiles.forEach(f -> f.setService(this));
        this.contextFiles = contextFiles;
    }

    public String getDataSourceName() {
        return DATA_SOURCE_NAME_PREFIX + this.name;
    }

    public enum Type {
        PROCESSOR, BULK_PROCESSOR, APPLICATION, PARALLEL_PROCESSOR, FTP_SERVICE, WPS_SERVICE
    }

    public enum Status {
        IN_DEVELOPMENT, AVAILABLE
    }

    public enum Licence {
        OPEN, RESTRICTED
    }

}
