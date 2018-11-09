package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.client.ElasticsearchClientRest;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
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
public class IndexManagerRest implements IndexManager {

  private final List<String> indexCache = new CopyOnWriteArrayList<>();

  private ElasticsearchProperties properties;
  private ElasticsearchClientRest client;

  @Autowired
  public IndexManagerRest(ElasticsearchClientRest client) {
    this.client = client;
    this.properties = client.getProperties();
  }

  @Override
  public boolean create(String indexName, String type, String mapping) {
    synchronized (IndexManager.class) {
      if (exists(indexName)) {
        return true;
      }

      CreateIndexRequest request = new CreateIndexRequest(indexName);

      request.settings(Settings.builder()
              .put("index.number_of_shards", properties.getShardsPerIndex())
              .put("index.number_of_replicas", properties.getReplicasPerShard())
      );

      request.mapping("_doc", mapping, XContentType.JSON);

      CreateIndexResponse createIndexResponse = client.getClient().indices().create(request, RequestOptions.DEFAULT);

      XContentBuilder builder;
      try {
        Settings indexSettings = Settings.builder()
                .put("number_of_shards", properties.getShardsPerIndex())
                .put("number_of_replicas", properties.getReplicasPerShard())
                .build();

        String json = mapping.substring(mapping.indexOf("{") + 1, mapping.lastIndexOf("}"));

        StreamInput targetStream = StreamInput.wrap(json.getBytes());

        builder = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("settings")
                .value(indexSettings)
                .endObject();


        if (mapping != null) {
          builder.startObject("mappings")
                  .rawValue(targetStream.readBytesReference(json.length()), XContentType.JSON)
                  .endObject();
        }

        builder.endObject();

      } catch (IOException e) {
        log.error("Error processing '{}' index mapping.", indexName, e);
        return false;
      }

      client.getClient().

      IndexRequest indexRequest = new IndexRequest(indexName);
      indexRequest.type(type);
      indexRequest.source(builder);
      indexRequest.create(true);

      boolean created = false;
      try {
        IndexResponse indexResponse = client.getClient().index(indexRequest);
        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
          created = true;
        }
      } catch (IOException e) {
        log.error("Error creating '{}' index on Elasticsearch.", indexName, e);
      }

      client.waitForYellowStatus();

      if (created) {
        indexCache.add(indexName);
      }

      return created;
    }
  }

  @Override
  public boolean index(String indexName, String type, String source, String routing) {
    return index(indexName, type, source, "", routing);
  }

  @Override
  public boolean index(String indexName, String type, String source, String id, String routing) {
    synchronized (IndexManagerRest.class) {
      IndexRequest indexRequest = new IndexRequest(indexName, type);
      if (id != null && !id.isEmpty()) {
        indexRequest.id(id);
      }
      indexRequest.source(source, XContentType.JSON);
      indexRequest.routing(routing);

      boolean indexed = false;
      try {
        IndexResponse indexResponse = client.getClient().index(indexRequest);
        indexed = indexResponse.status().equals(RestStatus.CREATED) || indexResponse.status().equals(RestStatus.OK);
      } catch (IOException e) {
        log.error("Could not index '{} #{}' to index '{}'.", type, routing, indexName, e);
      }

      client.waitForYellowStatus();

      return indexed;
    }
  }

  @Override
  public boolean exists(String indexName) {
    return exists(indexName, "");
  }

  @Override
  public boolean exists(String indexName, String routing) {
    synchronized (IndexManager.class) {
      if (indexCache.contains(indexName)) {
        return true;
      }

      SearchRequest searchRequest = new SearchRequest(indexName);
      searchRequest.routing(routing);

      boolean exists = false;
      try {
        SearchResponse searchResponse = client.getClient().search(searchRequest);
        exists = searchResponse.status().equals(RestStatus.OK);
      } catch (ElasticsearchStatusException e) {
        if (!RestStatus.NOT_FOUND.equals(e.status())) {
          log.error("Error checking '{}' index existence on Elasticsearch, unexpected status: ", e);
        }
      } catch (IOException e) {
        log.error("Error checking '{}' index existence on Elasticsearch.", indexName, e);
      }

      if (exists) {
        indexCache.add(indexName);
      }

      return exists;
    }
  }

  @Override
  public boolean update(String indexName, String type, String source, String id) {
    synchronized (IndexManagerRest.class) {
      UpdateRequest updateRequest = new UpdateRequest(indexName, type, id);
      updateRequest.doc(source, XContentType.JSON);
      updateRequest.routing(id);

      IndexRequest indexRequest = new IndexRequest(indexName, type, id);
      indexRequest.source(source, XContentType.JSON);
      indexRequest.routing(id);

      updateRequest.upsert(indexRequest);

      boolean updated = false;
      try {
        UpdateResponse updateResponse = client.getClient().update(updateRequest);
        updated = updateResponse.status().equals(RestStatus.OK);
      } catch (IOException e) {
        log.error("Error updating index '{}'.", indexName, e);
      }

      client.waitForYellowStatus();

      return updated;
    }
  }

  @Override
  public boolean delete(String indexName, String type, String id, String routing) {
    synchronized (IndexManagerRest.class) {

      boolean deleted = false;
      try {
        indexCache.remove(indexName);

        DeleteRequest deleteRequest = new DeleteRequest(indexName, type, id);
        deleteRequest.routing(routing);

        DeleteResponse deleteResponse = client.getClient().delete(deleteRequest);
        deleted = deleteResponse.status().equals(RestStatus.OK);
      } catch (IOException e) {
        log.error("Error deleting '{}' index from ElasticSearch.", indexName, e);
      }

      client.waitForYellowStatus();

      return deleted;
    }
  }

  @Override
  public void purgeIndexCache() {
    synchronized (IndexManagerRest.class) {
      indexCache.clear();
    }
  }
}
