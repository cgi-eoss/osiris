package com.cgi.eoss.osiris.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

/**
 * <p>Representation of a user-defined incident.</p>
 */
@Data
@ToString
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "osiris_incidents",
        indexes = @Index(name = "osiris_incidents_owner_idx", columnList = "owner"))
@NoArgsConstructor
@Entity
public class Incident implements OsirisEntityWithOwner<Incident> {

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
     * <p>The incident type applicable to this incident.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type", nullable = false)
    private IncidentType type;

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
     * <p>A Well-known-text string representing the spatial extent of this incident.</p>
     */
    @Column(name = "aoi", nullable = false)
    private String aoi;

    /**
     * <p>The date and time at which this incident began.</p>
     */
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /**
     * <p>The date and time at which this incident ended (or will end).</p>
     */
    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    public Incident(User owner, IncidentType type, String title, String description, String aoi, Instant startDate, Instant endDate) {
        this.owner = owner;
        this.type = type;
        this.title = title;
        this.description = description;
        this.aoi = aoi;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public int compareTo(Incident o) {
        return title.compareTo(o.getTitle());
    }
}
