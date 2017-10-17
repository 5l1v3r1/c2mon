package cern.c2mon.server.elasticsearch.bulk;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link BulkProcessor}. If a bulk operation fails, this class
 * will throw a {@link RuntimeException}.
 *
 * @author James Hamilton
 */
@Slf4j
public class BulkProcessorProxyRestImpl implements BulkProcessorProxy {

  private BulkRequest request = new BulkRequest();

  private RestHighLevelClient restClient;

  @Autowired
  public BulkProcessorProxyRestImpl(final ElasticsearchClient client, final ElasticsearchProperties properties) {
    restClient = client.getRestClient();
  }

  public void add(IndexRequest request) {
    Assert.notNull(request, "IndexRequest must not be null!");
    this.request.add(request);
  }

  @Override
  public boolean flush() {
    try {
      BulkResponse response = this.restClient.bulk(this.request);

      if (!response.hasFailures()) {
        return true;
      } else {
        return false;
      }
    } catch (IOException e) {
      log.error("Could not send bulk requests", e);
      return false;
    }
  }
}
