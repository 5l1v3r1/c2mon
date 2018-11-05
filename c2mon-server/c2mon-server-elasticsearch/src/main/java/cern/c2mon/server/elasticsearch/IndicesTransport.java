package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientImpl;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Static utility singleton for working with Elasticsearch indices.
 *
 * @author Justin Lewis Salmon
 */
@Slf4j
@Singleton
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="false")
public class IndicesTransport extends Indices {

    private final List<String> indexCache = new CopyOnWriteArrayList<>();

    @Getter
    private ElasticsearchClientImpl client;

    @Autowired
    public IndicesTransport(ElasticsearchClientImpl client, ElasticsearchProperties properties) {
        super(properties);
        this.client = client;
    }

    /**
     * Create a new index with an empty mapping.
     *
     * @param indexName the name of the index to create
     *
     * @return true if the index was successfully created, false otherwise
     */
    @Override
    public boolean create(String indexName) {
        return create(indexName, null, null);
    }

    /**
     * Create a new index with an initial mapping.
     *
     * @param indexName the name of the index to create
     * @param type      the mapping type
     * @param mapping   the mapping source
     *
     * @return true if the index was successfully created, false otherwise
     */
    @Override
    public boolean create(String indexName, String type, String mapping) {
        synchronized (Indices.class) {
            if (exists(indexName)) {
                return true;
            }

            CreateIndexRequestBuilder builder = client.getClient().admin().indices().prepareCreate(indexName);
            builder.setSettings(Settings.builder()
                    .put("number_of_shards", properties.getShardsPerIndex())
                    .put("number_of_replicas", properties.getReplicasPerShard())
                    .build());

            if (mapping != null) {
                builder.addMapping(type, mapping, XContentType.JSON);
            }

            log.debug("Creating new index with name {}", indexName);
            boolean created;

            try {
                CreateIndexResponse response = builder.get();
                created = response.isAcknowledged();
            } catch (ResourceAlreadyExistsException ex) {
                created = true;
            }

            client.waitForYellowStatus();

            if (created) {
                indexCache.add(indexName);
            }

            return created;
        }
    }

    /**
     * Check if a given index exists.
     * <p>
     * The node-local index cache will be searched first before querying
     * Elasticsearch directly.
     *
     * @param indexName the name of the index
     *
     * @return true if the index exists, false otherwise
     */
    @Override
    public boolean exists(String indexName) {
        synchronized (Indices.class) {
            boolean exists = indexCache.contains(indexName);
            if (!exists) {
                client.waitForYellowStatus();
                IndexMetaData indexMetaData = client.getClient().admin().cluster()
                        .state(Requests.clusterStateRequest())
                        .actionGet()
                        .getState()
                        .getMetaData()
                        .index(indexName);

                if (indexMetaData != null) {
                    indexCache.add(indexName);
                    exists = true;
                }
            }
            return exists;
        }
    }

    /**
     * Delete an index in Elasticsearch.
     *
     * @param indexName
     *
     * @return true if the request was acknowledged.
     */
    @Override
    public boolean delete(String indexName) {
        synchronized (Indices.class) {
            try {
                DeleteIndexResponse response = client.getClient().admin().indices().delete(new DeleteIndexRequest(indexName)).get();
                if (response.isAcknowledged()) {
                    indexCache.remove(indexName);
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException|ExecutionException e) {
                log.error("Error while deleting index", e);
                return false;
            }
        }
    }
}
