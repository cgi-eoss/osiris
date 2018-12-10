package com.cgi.eoss.osiris.api.mappings;

import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.projections.DetailedOsirisFile;
import com.cgi.eoss.osiris.model.projections.ShortOsirisFile;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * <p>HATEOAS resource processor for {@link OsirisFile}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OsirisFileResourceProcessor extends BaseResourceProcessor<OsirisFile> {

    private final RepositoryEntityLinks entityLinks;
    private final CatalogueService catalogueService;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<OsirisFile> getTargetClass() {
        return OsirisFile.class;
    }

    private void addDownloadLink(Resource resource, OsirisFile.Type type) {
        // TODO Check file datasource?
        if (type != OsirisFile.Type.EXTERNAL_PRODUCT) {
            // TODO Do this properly with a method reference
            resource.add(new Link(resource.getLink("self").getHref() + "/dl").withRel("download"));
        }
    }

    private void addWmsLink(Resource resource, OsirisFile.Type type, URI uri) {
        HttpUrl wmsLink = catalogueService.getWmsUrl(type, uri);
        if (wmsLink != null) {
            resource.add(new Link(wmsLink.toString()).withRel("wms"));
        }
    }

    private void addOsirisLink(Resource resource, URI osirisFileUri) {
        resource.add(new Link(osirisFileUri.toASCIIString()).withRel("osiris"));
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<OsirisFile>> {
        @Override
        public Resource<OsirisFile> process(Resource<OsirisFile> resource) {
            OsirisFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addOsirisLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedOsirisFile>> {
        @Override
        public Resource<DetailedOsirisFile> process(Resource<DetailedOsirisFile> resource) {
            DetailedOsirisFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addOsirisLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortOsirisFile>> {
        @Override
        public Resource<ShortOsirisFile> process(Resource<ShortOsirisFile> resource) {
            ShortOsirisFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());

            return resource;
        }
    }

}
