package com.cgi.eoss.osiris.model;

import org.springframework.hateoas.Identifiable;

public interface OsirisEntity<T> extends Comparable<T>, Identifiable<Long> {

    /**
     * @return The unique identifier of the entity.
     */
    Long getId();

    /**
     * @param id The unique identifier of the entity.
     */
    void setId(Long id);

}
