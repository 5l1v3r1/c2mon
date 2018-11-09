package cern.c2mon.server.elasticsearch.client;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.threadpool.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="true")
public class ElasticsearchClientRest implements ElasticsearchClient<RestHighLevelClient> {
    @Getter
    private ElasticsearchProperties properties;

    private RestHighLevelClient restHighLevelClient;

    @Getter
    private ThreadPool threadPool;

    @Autowired
    public ElasticsearchClientRest(ElasticsearchProperties properties) throws NodeValidationException {
        this.properties = properties;

        RestClientBuilder restClientBuilder =
                RestClient.builder(new HttpHost(properties.getHost(), properties.getHttpPort(), "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(10000)
                                .setSocketTimeout(120000))
                .setMaxRetryTimeoutMillis(60000);

        restHighLevelClient = new RestHighLevelClient(restClientBuilder);

        try {
            if (!restHighLevelClient.ping(RequestOptions.DEFAULT)) {
                log.error("Error pinging to the Elasticsearch cluster at {}:{}", properties.getHost(), properties.getHttpPort());
            }
        } catch (IOException e) {
            log.error("IOError connecting to the Elasticsearch cluster at {}:{}", properties.getHost(), properties.getHttpPort(), e);
        }

        Settings threadPoolSettings = Settings.builder()
                .put("node.name", properties.getNodeName())
                .build();

        threadPool = new ThreadPool(threadPoolSettings);
    }

    @Override
    public void waitForYellowStatus() {
        Map<String, String> parameters = new HashMap() {{
            put("wait_for_status", "yellow");
        }};

        restClient.performRequestAsync("GET", "/_cluster/health", parameters, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                log.info("Waiting for Elasticsearch yellow status completed successfully. ");
            }

            @Override
            public void onFailure(Exception e) {
                log.error("Exception when waiting for yellow status", e);
                throw new RuntimeException("Timeout when waiting for Elasticsearch yellow status!");
            }
        });
    }

    private ClusterHealthStatus getClusterHealth() {
        Map<String, String> parameters = new HashMap() {{
            put("wait_for_status", "yellow");
        }};

        Response response;
        try {
            response = restClient.performRequest("GET", "/_cluster/health", parameters);
        } catch (IOException e) {
            log.error("There was a problem executing Elasticsearch cluster health check request.", e);
            return null;
        }

        return getClusterHealthStatus(response).orElse(ClusterHealthStatus.RED);
    }

    private Optional<ClusterHealthStatus> getClusterHealthStatus(Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            return Optional.of(ClusterHealthStatus.fromString((String) map.get("status")));
        } catch (IOException e) {
            log.error("There was a problem processing Elasticsearch cluster health check response.", e);
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        if (restClient != null) {
            try {
                restClient.close();
                log.info("Closed Elasticsearch client.");
            } catch (IOException e) {
                log.error("Error closing Elasticsearch client.", e);
                return ;
            } finally {
                restClient = null;
                restHighLevelClient = null;
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
