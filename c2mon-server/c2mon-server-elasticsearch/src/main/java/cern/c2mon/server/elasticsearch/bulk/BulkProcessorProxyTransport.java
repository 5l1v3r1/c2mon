package cern.c2mon.server.elasticsearch.bulk;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;

/**
 * Wrapper around {@link BulkProcessor}. If a bulk operation fails, this class
 * will throw a {@link RuntimeException}.
 *
 * @author Justin Lewis Salmon
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="false")
public class BulkProcessorProxyTransport implements BulkProcessor.Listener, BulkProcessorProxy {

  private final BulkProcessor bulkProcessor;

  /**
   * @param client to be used to communicate with Elasticsearch cluster.
   */
  @Autowired
  public BulkProcessorProxyTransport(final ElasticsearchClientTransport client) {
    this.bulkProcessor = BulkProcessor.builder(client.getClient(), this)
          .setBulkActions(client.getProperties().getBulkActions())
          .setBulkSize(new ByteSizeValue(client.getProperties().getBulkSize(), ByteSizeUnit.MB))
          .setFlushInterval(TimeValue.timeValueSeconds(client.getProperties().getBulkFlushInterval()))
          .setConcurrentRequests(client.getProperties().getConcurrentRequests())
          .build();
  }

  @Override
  public void add(IndexRequest request) {
    Assert.notNull(request, "IndexRequest must not be null!");
    bulkProcessor.add(request);
  }

  @Override
  public boolean flush() {
    bulkProcessor.flush();
    return true;
  }

  @Override
  public void beforeBulk(long executionId, BulkRequest request) {
    log.debug("Going to execute new bulk operation composed of {} actions", request.numberOfActions());
  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
    log.debug("Executed bulk operation composed of {} actions", request.numberOfActions());
  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
    log.warn("Error executing bulk operation", failure);
    throw new RuntimeException(failure);
  }
}
