package cern.c2mon.server.elasticsearch.client;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;

/**
 * Defines an interface for Elasticsearch client-cluster communication.
 *
 * @param <T> type of the client to communicate with Elasticsearch cluster.
 */
public interface ElasticsearchClient<T> {

  /**
   * Block and wait for the cluster to become yellow.
   */
  void waitForYellowStatus();

  /**
   * Closes client connection.
   */
  void close();

  /**
   * @return properties used by client to communicate with Elasticsearch cluster.
   */
  ElasticsearchProperties getProperties();

  /**
   * @return client used to communicate with Elasticsearch cluster.
   */
  T getClient();

  /**
   * @return true if Elasticsearch cluster is healthy.
   */
  boolean isClusterYellow();
}
