package com.cgi.eoss.osiris.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.google.common.collect.ComparisonChain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "osiris_default_service_templates",
        indexes = {@Index(name = "osiris_default_service_templates_type_template_idx", columnList = "type")}
)
@NoArgsConstructor
@Entity
public class DefaultServiceTemplate implements OsirisEntity<DefaultServiceTemplate> {

    
	@Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	/**
     * <p>Unique internal identifier of the costing expression.</p>
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_template", nullable = false)
    private OsirisServiceTemplate serviceTemplate;

    /**
     * <p>Which type of service the template is a default for.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OsirisService.Type serviceType;


    @Builder
    public DefaultServiceTemplate(OsirisService.Type serviceType, OsirisServiceTemplate serviceTemplate) {
        this.serviceType = serviceType;
        this.serviceTemplate = serviceTemplate;
    }

    @Override
    public int compareTo(DefaultServiceTemplate o) {
        return ComparisonChain.start().compare(id, o.id).result();
    }

 
}
