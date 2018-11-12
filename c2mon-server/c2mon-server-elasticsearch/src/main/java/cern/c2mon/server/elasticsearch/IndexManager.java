package cern.c2mon.server.elasticsearch;

/**
 * Defines all supported Elasticsearch index-related operations.
 *
 * @author Serhiy Boychenko
 */
public interface IndexManager {

    /**
     * Type is being removed in Elasticsearch 6.x (check
     * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/removal-of-types.html">Elasticsearch
     * documentation</a> for more details).
     */
    static final String TYPE = "_doc";

    /**
     * Create a new index with an initial mapping.
     *
     * @param indexName the name of the index to create.
     * @param mapping   the mapping source.
     *
     * @return true if the index was successfully created, false otherwise.
     */
    boolean create(String indexName, String mapping);

    /**
     * Store document with relation to specific index.
     *
     * @param indexName to relate the document with.
     * @param source of the document.
     * @param routing representing particular shard.
     *
     * @return true if the document was successfully indexed, false otherwise.
     */
    boolean index(String indexName, String source, String routing);

    /**
     * Store document with relation to specific index.
     *
     * @param indexName to relate the document with.
     * @param source of the document.
     * @param id of the document.
     * @param routing representing particular shard.
     *
     * @return true if the document was successfully indexed, false otherwise.
     */
    boolean index(String indexName, String source, String id, String routing);

    /**
     * Check if a given index exists.
     * <p>
     * The node-local index cache will be searched first before querying
     * Elasticsearch directly.
     *
     * @param indexName the name of the index
     *
     * @return true if the index exists, false otherwise.
     */
    boolean exists(String indexName);

    /**
     * Check if a given index exists.
     * <p>
     * The node-local index cache will be searched first before querying
     * Elasticsearch directly.
     *
     * @param indexName to check if it exists.
     * @param routing representing particular shard.
     *
     * @return true if the index exists, false otherwise.
     */
    boolean exists(String indexName, String routing);

    /**
     * Update indexed document (document will be created if not existing).
     *
     * @param indexName to update its document.
     * @param source of the new document.
     * @param id of the old (existing) document.
     *
     * @return true if index was successfully updated, false otherwise.
     */
    boolean update(String indexName, String source, String id);

    /**
     * Delete an index in Elasticsearch.
     *
     * @param indexName to be deleted.
     *
     * @return true if index was successfully deleted, false otherwise.
     */
    boolean delete(String indexName, String id, String routing);

    /**
     * Removes all cached components from index cache.
     */
    void purgeIndexCache();
}
