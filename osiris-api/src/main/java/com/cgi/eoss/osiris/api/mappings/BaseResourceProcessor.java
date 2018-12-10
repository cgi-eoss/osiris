package com.cgi.eoss.osiris.api.mappings;

import com.cgi.eoss.osiris.model.OsirisEntity;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Resource;

abstract class BaseResourceProcessor<T extends OsirisEntity<T>> {
    void addSelfLink(Resource resource, Identifiable<Long> entity) {
        if (resource.getLink("self") == null) {
            resource.add(getEntityLinks().linkToSingleResource(getTargetClass(), entity.getId()).withSelfRel().expand());
            resource.add(getEntityLinks().linkToSingleResource(getTargetClass(), entity.getId()));
        }
    }

    protected abstract EntityLinks getEntityLinks();

    protected abstract Class<T> getTargetClass();
}
