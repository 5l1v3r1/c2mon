package cern.c2mon.server.elasticsearch.bulk;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientRest;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.function.BiConsumer;

/**
 * Wrapper around {@link BulkProcessor}. If a bulk operation fails, this class
 * will throw a {@link RuntimeException}.
 *
 * @author Serhiy Boychenko
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="true")
public class BulkProcessorProxyRest implements BulkProcessorProxy, BulkProcessor.Listener {

    private BulkProcessor bulkProcessor;

    /**
     * @param client to be used to communicate with Elasticsearch cluster.
     */
    @Autowired
    public BulkProcessorProxyRest(final ElasticsearchClientRest client) {
        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) -> client.getClient().bulkAsync(request, RequestOptions.DEFAULT, bulkListener);

        bulkProcessor = BulkProcessor.builder(bulkConsumer, this)
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
