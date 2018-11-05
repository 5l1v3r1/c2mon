package cern.c2mon.server.elasticsearch.client.rest;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.threadpool.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
public class RestElasticSearchClient implements ElasticsearchClient<RestHighLevelClient, ClusterHealthStatus> {
    @Getter
    private ElasticsearchProperties properties;
    @Getter
    private RestClient restClient;
    @Getter
    private RestHighLevelClient restHighLevelClient;
    @Getter
    private ThreadPool threadPool;

    private EmbeddedElastic embeddedNode;

    @Autowired
    public RestElasticSearchClient(ElasticsearchProperties properties) throws NodeValidationException {
        this.properties = properties;

        if (properties.isEmbedded()) {
            startEmbeddedNode();
        }

        restClient = RestClient.builder(new HttpHost(properties.getHost(), properties.getHttpPort(), "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(10000)
                                .setSocketTimeout(120000))
                .setMaxRetryTimeoutMillis(60000)
                .build();

        restHighLevelClient = new RestHighLevelClient(restClient);

        try {
            if (!restHighLevelClient.ping()) {
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

    @Override
    public ClusterHealthStatus getClusterHealth() {
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
    public synchronized void startEmbeddedNode() throws NodeValidationException {

        if (this.embeddedNode != null) {
            log.info("Embedded Elasticsearch cluster already running");
            return;
        }

        log.info("Launching an embedded Elasticsearch cluster: {}", properties.getClusterName());

        //TODO: find a dynamic way of defining ES version
        embeddedNode = EmbeddedElastic.builder()
                .withElasticVersion("5.6.0")
                .withSetting(PopularProperties.TRANSPORT_TCP_PORT, properties.getHttpPort())
                .withSetting(PopularProperties.CLUSTER_NAME, properties.getClusterName())
                .build();

        try {
            embeddedNode.start();
        } catch (InterruptedException | IOException e) {
            log.error("An error occurred launching an embedded Elasticsearch cluster.", e);
        }
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
    public void closeEmbeddedNode() throws IOException {
        if (embeddedNode != null) {
            embeddedNode.stop();
            embeddedNode= null;
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
