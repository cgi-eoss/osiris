package com.cgi.eoss.osiris.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Representation of a user-defined incident type.</p>
 */
@Data
@ToString
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "osiris_incident_types",
        indexes = @Index(name = "osiris_incident_types_owner_idx", columnList = "owner"))
@NoArgsConstructor
@Entity
public class IncidentType implements OsirisEntityWithOwner<IncidentType> {

    /**
     * <p>Internal unique identifier of the incident type.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>The user that owns this incident type.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    /**
     * <p>A user-defined, human-readable name for this incident type.</p>
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * <p>A user-defined description of this incident type.</p>
     */
    @Column(name = "description")
    private String description;

    /**
     * <p>An internal identifier of the icon to be used to represent this incident type.</p>
     */
    @Column(name = "icon_id")
    private String iconId;

    /**
     * <p>A set of {@link IncidentProcessingTemplate}s associated with this incident type, to be suggested for child instances.</p>
     */
    @OneToMany(mappedBy = "incidentType", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("title DESC")
    private List<IncidentProcessingTemplate> incidentProcessingTemplates = new ArrayList<>();


    public IncidentType(User owner, String title, String description, String iconId) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.iconId = iconId;
    }

    @Override
    public int compareTo(IncidentType o) {
        return title.compareTo(o.getTitle());
    }
}
