package cern.c2mon.server.elasticsearch.client;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import org.elasticsearch.node.NodeValidationException;

import java.io.IOException;

public interface ElasticsearchClient<T> {
  void waitForYellowStatus();

  void close();

  ElasticsearchProperties getProperties();

  T getClient();

  boolean isClusterYellow();
}
