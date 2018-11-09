package cern.c2mon.server.elasticsearch;

public interface IndexManager {
    boolean create(String indexName, String type, String mapping);
    boolean index(String indexName, String type, String source, String routing);
    boolean index(String indexName, String type, String source, String id, String routing);
    boolean exists(String indexName);
    boolean exists(String indexName, String routing);
    boolean update(String indexName, String type, String source, String id);
    boolean delete(String indexName, String type, String id, String routing);
    void purgeIndexCache();
}
