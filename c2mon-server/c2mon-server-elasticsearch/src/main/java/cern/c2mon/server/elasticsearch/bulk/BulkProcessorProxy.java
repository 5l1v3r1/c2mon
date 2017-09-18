package cern.c2mon.server.elasticsearch.bulk;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;

/**
 * Wrapper around {@link BulkProcessor}. If a bulk operation fails, this class
 * will throw a {@link RuntimeException}.
 *
 * @author Justin Lewis Salmon
 */
@Slf4j
@Component
public class BulkProcessorProxy implements BulkProcessor.Listener {

  private final BulkProcessor bulkProcessor;

  @Autowired
  public BulkProcessorProxy(final ElasticsearchClient client, final ElasticsearchProperties properties) {
    this.bulkProcessor = BulkProcessor.builder(client.getClient(), this)
        .setBulkActions(properties.getBulkActions())
        .setBulkSize(new ByteSizeValue(properties.getBulkSize(), ByteSizeUnit.MB))
        .setFlushInterval(TimeValue.timeValueSeconds(properties.getBulkFlushInterval()))
        .setConcurrentRequests(properties.getConcurrentRequests())
        .build();
  }

  public void add(IndexRequest request) {
    Assert.notNull(request, "IndexRequest must not be null!");
    bulkProcessor.add(request);
  }

  public void flush() {
    bulkProcessor.flush();
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
