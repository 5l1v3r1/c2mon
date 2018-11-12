package cern.c2mon.server.elasticsearch.util;

import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Allows to start embedded Elasticsearch server. Should be used for <b>TESTING</b> purposes only.
 *
 * @author Serhiy Boychenko
 */
@Slf4j
public class EmbeddedElasticsearchManager {

    private static EmbeddedElastic embeddedNode;

    private EmbeddedElasticsearchManager() {
        /* Only static methods below. */
    }

    /**
     * Starts embedded Elasticsearch server (if it is not already running, does nothing otherwise).
     *
     * @param properties to setup the instance.
     */
    public static void start(ElasticsearchProperties properties) {
        synchronized (EmbeddedElasticsearchManager.class) {
            if (embeddedNode == null) {
                log.info("********** TESTING PURPOSE ONLY *********");
                log.info("Starting embedded Elasticsearch instance!");

                embeddedNode = EmbeddedElastic.builder()
                        .withElasticVersion(properties.getVersion())
                        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, properties.getPort())
                        .withSetting(PopularProperties.HTTP_PORT, properties.getHttpPort())
                        .withSetting(PopularProperties.CLUSTER_NAME, properties.getClusterName())
                        .withStartTimeout(1, TimeUnit.MINUTES)
                        .build();

                try {
                    embeddedNode.start();
                } catch (IOException | InterruptedException e) {
                    log.error("An error occurred starting embedded Elasticsearch instance!", e);
                }
            }
        }
    }

    /**
     * Stops embedded Elasticsearch server.
     */
    public static void stop() {
        synchronized (EmbeddedElasticsearchManager.class) {
            if (embeddedNode != null) {
                embeddedNode.stop();
                embeddedNode = null;
            }
        }
    }

    /**
     * @return an instance of running embedded Elasticsearch server.
     */
    public static EmbeddedElastic getEmbeddedNode() {
        if (embeddedNode == null) {
            throw new IllegalStateException("Embedded Elasticsearh instance must be started first!");
        }
        return embeddedNode;
    }
}
