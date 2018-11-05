package cern.c2mon.server.elasticsearch.client;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import org.elasticsearch.node.NodeValidationException;

import java.io.IOException;

public interface ElasticsearchClient<T, CH> {
  void waitForYellowStatus();

  CH getClusterHealth();

  //@TODO "using Node directly within an application is not officially supported"
  //https://www.elastic.co/guide/en/elasticsearch/reference/5.5/breaking_50_java_api_changes.html
  //@TODO Embedded ES is no longer supported
  void startEmbeddedNode() throws NodeValidationException;

  void close();

  void closeEmbeddedNode() throws IOException;

  ElasticsearchProperties getProperties();

  T getClient();

  boolean isClusterYellow();
}
