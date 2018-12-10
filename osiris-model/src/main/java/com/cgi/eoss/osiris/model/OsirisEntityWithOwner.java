package com.cgi.eoss.osiris.model;

public interface OsirisEntityWithOwner<T> extends OsirisEntity<T> {

    /**
     * @return The user who owns the entity.
     */
    User getOwner();

    /**
     * @param owner The new owner of the entity.
     */
    void setOwner(User owner);

}
