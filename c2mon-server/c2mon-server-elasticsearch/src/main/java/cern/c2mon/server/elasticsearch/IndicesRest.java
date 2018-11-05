package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.client.rest.RestElasticSearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static utility singleton for working with Elasticsearch indices.
 *
 * @author Justin Lewis Salmon
 */
@Slf4j
@Singleton
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="true")
public class IndicesRest extends Indices {

  private final List<String> indexCache = new CopyOnWriteArrayList<>();

  @Getter
  private RestElasticSearchClient client;

  @Autowired
  public IndicesRest(RestElasticSearchClient client, ElasticsearchProperties properties) {
    super(properties);
    this.client = client;
  }

  /**
   * Create a new index with an empty mapping.
   *
   * @param indexName the name of the index to create
   *
   * @return true if the index was successfully created, false otherwise
   */
  public boolean create(String indexName) {
    return create(indexName, null, null);
  }

  /**
   * Create a new index with an initial mapping.
   *
   * @param indexName the name of the index to create
   * @param type      the mapping type
   * @param mapping   the mapping source
   *
   * @return true if the index was successfully created, false otherwise
   */
  public synchronized boolean create(String indexName, String type, String mapping) {
    if (exists(indexName)) {
      return true;
    }

    log.debug("Creating new index with name {}", indexName);

    try {
      Settings indexSettings = Settings.builder()
        .put("number_of_shards", properties.getShardsPerIndex())
        .put("number_of_replicas", properties.getReplicasPerShard())
        .build();

      XContentBuilder builder = XContentFactory.jsonBuilder()
          .startObject()
            .startObject("settings")
              .value(indexSettings)
            .endObject();

      if (mapping != null) {
        builder.startObject("mappings")
            .field(type, "MAPPINGR3PLAC3M3")
            //.startObject(type).value(mapping).endObject() does not work
            .endObject();
      }

      String payload = builder.endObject().string();

      if (mapping != null) {
        payload = payload.replace("\"MAPPINGR3PLAC3M3\"", mapping);
      }

      HttpEntity entity = new NStringEntity(payload, ContentType.APPLICATION_JSON);


//      Response response = client.getClient().performRequest("PUT", "/" + indexName, emptyMap(), entity);
//      if (response.getStatusLine().equals(RestStatus.OK)) {
//        indexCache.add(indexName);
//        return true;
//      } else {
//        return false;
//      }

    } catch (IOException e) {
      log.error("Could not create index {}", indexName, e);
      return false;
    }

    return false;
  }

  /**
   * Check if a given index exists.
   * <p>
   * The node-local index cache will be searched first before querying
   * Elasticsearch directly.
   *
   * @param indexName the name of the index
   *
   * @return true if the index exists, false otherwise
   */
  public boolean exists(String indexName) {
    if (indexCache.contains(indexName)) {
      return true;
    }

    final RestClient restClient = client.getRestClient();
    try {
      final Response response = restClient.performRequest("HEAD", "/" + indexName);
      return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    } catch (IOException e) {
      log.error("Error checking for existing of {} index", indexName, e);
      return false;
    }
  }

  @Override
  public boolean delete(String indexName) {
    return false;
  }
}
