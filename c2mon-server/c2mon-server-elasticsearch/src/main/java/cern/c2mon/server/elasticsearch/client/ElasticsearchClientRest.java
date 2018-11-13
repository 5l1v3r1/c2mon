package cern.c2mon.server.elasticsearch.client;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Wrapper around {@link RestHighLevelClient}. Connects asynchronously, but also provides
 * methods to block until a healthy connection is established.
 *
 * @author Serhiy Boychenko
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="true")
public class ElasticsearchClientRest implements ElasticsearchClient<RestHighLevelClient> {
    @Getter
    private ElasticsearchProperties properties;

    private RestHighLevelClient restHighLevelClient;

    /**
     * @param properties to initialize REST client.
     */
    @Autowired
    public ElasticsearchClientRest(ElasticsearchProperties properties) {
        this.properties = properties;

        RestClientBuilder restClientBuilder =
                RestClient.builder(new HttpHost(properties.getHost(), properties.getHttpPort(), "http"));

        restHighLevelClient = new RestHighLevelClient(restClientBuilder);

        try {
            if (!restHighLevelClient.ping(RequestOptions.DEFAULT)) {
                log.error("Error pinging to the Elasticsearch cluster at {}:{}", properties.getHost(), properties.getHttpPort());
            }
        } catch (IOException e) {
            log.error("IOError connecting to the Elasticsearch cluster at {}:{}", properties.getHost(), properties.getHttpPort(), e);
        }
    }

    @Override
    public void waitForYellowStatus() {
        ClusterHealthRequest request = new ClusterHealthRequest();
        request.timeout("60s");
        request.waitForYellowStatus();

        restHighLevelClient.cluster().healthAsync(request, RequestOptions.DEFAULT, new ActionListener<ClusterHealthResponse>() {
            @Override
            public void onResponse(ClusterHealthResponse response) {
                log.info("Waiting for Elasticsearch yellow status completed successfully. ");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("Exception when waiting for yellow status", e);
                throw new IllegalStateException("Timeout when waiting for Elasticsearch yellow status!");
            }
        });
    }

    private ClusterHealthStatus getClusterHealth() {
        ClusterHealthRequest request = new ClusterHealthRequest();
        request.timeout("60s");
        request.waitForYellowStatus();

        try {
            return restHighLevelClient.cluster().health(request, RequestOptions.DEFAULT).getStatus();
        } catch (IOException e) {
            log.error("There was a problem executing Elasticsearch cluster health check request.", e);
        }

        return ClusterHealthStatus.RED;
    }

    @Override
    public void close() {
        if (restHighLevelClient != null) {
            try {
                restHighLevelClient.close();
                log.info("Closed Elasticsearch client.");
            } catch (IOException e) {
                log.error("Error closing Elasticsearch client.", e);
                return ;
            }
        }
    }

    @Override
    public RestHighLevelClient getClient() {
        return restHighLevelClient;
    }

    @Override
    public boolean isClusterYellow() {
        byte status = getClusterHealth().value();
        return status == ClusterHealthStatus.YELLOW.value() || status == ClusterHealthStatus.GREEN.value();
    }
}
