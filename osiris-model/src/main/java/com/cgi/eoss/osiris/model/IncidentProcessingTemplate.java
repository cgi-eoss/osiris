package com.cgi.eoss.osiris.model;

import com.cgi.eoss.osiris.model.converters.StringListMultimapYamlConverter;
import com.cgi.eoss.osiris.model.converters.StringMultimapYamlConverter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <p>Template for specifying processing to be associated with an incident.</p>
 */
@Data
@ToString
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "osiris_incident_processing_templates",
        indexes = @Index(name = "osiris_incident_processing_templates_owner_idx", columnList = "owner"))
@NoArgsConstructor
@Entity
public class IncidentProcessingTemplate implements OsirisEntityWithOwner<IncidentProcessingTemplate> {

    /**
     * <p>Internal unique identifier of the incident.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The user that owns this incident.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>A user-defined, human-readable name for this incident.</p>
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * <p>A user-defined description of this incident.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>The incident type for which this processing template is valid.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_type", nullable = false)
    private IncidentType incidentType;

    /**
     * <p>The service that will be launched by this processing template.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service", nullable = false)
    private OsirisService service;

    /**
     * <p>Tag to identify the input that will be dynamically getting values.</p>
     */
    @Column(name = "systematic_input")
    private String systematicInput = "parallelInput";

    /**
     * <p>The set of inputs that are fixed in this definition, and their values.</p>
     */
    @Lob
    @Convert(converter = StringMultimapYamlConverter.class)
    @Column(name = "fixed_inputs")
    private Multimap<String, String> fixedInputs = HashMultimap.create();

    /**
     * <p>The set of search parameters that are fixed in this definition, and their values.</p>
     */
    // TODO: startDate and stopDate are fixed by the Incident, have the way this is calculated be configurable?
    @Lob
    @Convert(converter = StringListMultimapYamlConverter.class)
    @Column(name = "search_parameters")
    private ListMultimap<String, String> searchParameters = ArrayListMultimap.create();

    @Override
    public int compareTo(IncidentProcessingTemplate o) {
        return title.compareTo(o.getTitle());
    }
}
