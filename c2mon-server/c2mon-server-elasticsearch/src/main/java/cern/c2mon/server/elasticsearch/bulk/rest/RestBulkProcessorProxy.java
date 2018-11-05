package cern.c2mon.server.elasticsearch.bulk.rest;

import cern.c2mon.server.elasticsearch.bulk.BulkProcessorProxy;
import cern.c2mon.server.elasticsearch.client.rest.RestElasticSearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

@Slf4j
public class RestBulkProcessorProxy implements BulkProcessorProxy, BulkProcessor.Listener {

    private BulkProcessor bulkProcessor;

    @Autowired
    public RestBulkProcessorProxy(final RestElasticSearchClient client, final ElasticsearchProperties properties) {
        bulkProcessor = new BulkProcessor.Builder(client.getClient()::bulkAsync, this, client.getThreadPool())
                .setBulkActions(properties.getBulkActions())
                .setBulkSize(new ByteSizeValue(properties.getBulkSize(), ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(properties.getBulkFlushInterval()))
                .setConcurrentRequests(properties.getConcurrentRequests())
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
