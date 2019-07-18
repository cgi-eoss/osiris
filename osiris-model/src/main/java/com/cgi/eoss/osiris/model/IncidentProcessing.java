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
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * <p>A systematic processing associated with an incident.</p>
 */
@Data
@ToString
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "osiris_incident_processings",
        indexes = @Index(name = "osiris_incident_processings_owner_idx", columnList = "owner"))
@NoArgsConstructor
@Entity
public class IncidentProcessing implements OsirisEntityWithOwner<IncidentProcessing> {

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
     * <p>The template on which this processing is based.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template", nullable = false)
    private IncidentProcessingTemplate template;

    /**
     * <p>The incident that this processing is associated with.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident", nullable = false)
    private Incident incident;

    /**
     * <p>The systematic processing launched for this incident processing.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "systematic_processing", nullable = true)
    private SystematicProcessing systematicProcessing;
    
    /**
     * <p>The single job launched for this incident processing, if present.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job", nullable = true)
    private Job job;

    /**
     * <p>The inputs associated with this processing, and their values.</p>
     */
    @Lob
    @Convert(converter = StringMultimapYamlConverter.class)
    @Column(name = "inputs")
    private Multimap<String, String> inputs = HashMultimap.create();

    /**
     * <p>The set of search parameters to be used in this processing, and their values.</p>
     */
    @Lob
    @Convert(converter = StringListMultimapYamlConverter.class)
    @Column(name = "search_parameters")
    private ListMultimap<String, String> searchParameters = ArrayListMultimap.create();

    /**
     * <p>The collection in which the output products of this processing are stored.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection")
    private Collection collection;

    @Override
    public int compareTo(IncidentProcessing o) {
        return incident.compareTo(o.getIncident());
    }
}
