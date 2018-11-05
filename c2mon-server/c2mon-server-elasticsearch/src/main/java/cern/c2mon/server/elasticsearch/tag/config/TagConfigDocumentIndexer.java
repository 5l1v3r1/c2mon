package cern.c2mon.server.elasticsearch.tag.config;

import cern.c2mon.server.cache.TagFacadeGateway;
import cern.c2mon.server.common.tag.Tag;
import org.elasticsearch.action.index.IndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;

public abstract class TagConfigDocumentIndexer {

    @Autowired
    private TagFacadeGateway tagFacadeGateway;
    @Autowired
    private TagConfigDocumentConverter converter;

    abstract void indexTagConfig(final TagConfigDocument tag);
    abstract void updateTagConfig(final TagConfigDocument tag);
    abstract void removeTagConfigById(final Long tagId);

    /**
     * Re-index all tag config documents from the cache.
     */
    @ManagedOperation(description = "Re-indexes all tag configs from the cache to Elasticsearch")
    public void reindexAllTagConfigDocuments() {
        if (tagFacadeGateway == null) {
            throw new IllegalStateException("Tag Facade Gateway is null");
        }

        for (Long id : tagFacadeGateway.getKeys()) {
            Tag tag = tagFacadeGateway.getTag(id);
            converter.convert(tag, tagFacadeGateway.getAlarms(tag)).ifPresent(this::updateTagConfig);
        }
    }

}
