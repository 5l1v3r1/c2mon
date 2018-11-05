/*******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 *
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 *
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package cern.c2mon.server.elasticsearch.tag.config;

import cern.c2mon.server.elasticsearch.Indices;
import cern.c2mon.server.elasticsearch.IndicesRest;
import cern.c2mon.server.elasticsearch.MappingFactory;
import cern.c2mon.server.elasticsearch.client.rest.RestElasticSearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.elasticsearch.action.DocWriteRequest.OpType;

/**
 * This class manages the indexing of {@link TagConfigDocument} instances to
 * the Elasticsearch cluster.
 *
 * @author Szymon Halastra
 * @author Justin Lewis Salmon
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "c2mon.server.elasticsearch.rest", havingValue="true")
public class TagConfigDocumentIndexerRest extends TagConfigDocumentIndexer {

  private static final String TYPE = "tag_config";

  private final RestElasticSearchClient client;
  private final IndicesRest indicesRest;

  private final String configIndex;

  @Autowired
  public TagConfigDocumentIndexerRest(final RestElasticSearchClient client, final ElasticsearchProperties properties, final IndicesRest indicesRest) {
    this.client = client;
    this.indicesRest = indicesRest;
    this.configIndex = properties.getTagConfigIndex();
  }

  @Override
  void indexTagConfig(final TagConfigDocument tag) {
    if (!indicesRest.exists(configIndex)) {
      indicesRest.create(configIndex, TYPE, MappingFactory.createTagConfigMapping());
    }

    IndexRequest request = new IndexRequest(configIndex);

    request.source(tag.toString(), XContentType.JSON);
    request.id(tag.getId());
    request.type("supervision");
    request.opType(OpType.CREATE);

    try {
      IndexResponse response = client.getClient().index(request);
      if (!response.status().equals(RestStatus.CREATED)) {
       log.error("Error occurred while indexing the config for tag #{}", tag.getId());
      }
    } catch (IOException e) {
      log.error("Could not index supervision event #{} to index {}", tag.getId(), configIndex, e);
    }
  }

  @Override
  void updateTagConfig(TagConfigDocument tag) {
    IndexRequest request = new IndexRequest(configIndex);

    request.source(tag.toString(), XContentType.JSON);
    request.id(tag.getId());
    request.type("supervision");

    try {
      IndexResponse response = client.getClient().index(request);
      if (!response.status().equals(RestStatus.OK)) {
        log.error("Error occurred while updating the config for tag #{}", tag.getId());
      }
    } catch (ResponseException e) {
      if (e.getResponse().getStatusLine().equals(RestStatus.NOT_FOUND)) {
        log.error("Tag #{} not found in index {}", tag.getId(), configIndex, e);
      } else {
        log.error("Error updating tag #{} in index {}", tag.getId(), configIndex, e);
      }
    } catch (IOException e) {
      log.error("Could not update supervision event #{} to index {}", tag.getId(), configIndex, e);
    }
  }

  @Override
  void removeTagConfigById(final Long tagId) {
    if (!indicesRest.exists(this.configIndex)) {
      return;
    }

    DeleteRequest deleteRequest = new DeleteRequest(configIndex, TYPE, String.valueOf(tagId)).routing(String.valueOf(tagId));
    try {
      DeleteResponse deleteResponse = client.getClient().delete(deleteRequest);
      if (deleteResponse.status().equals(RestStatus.NOT_FOUND)) {
        log.warn("Tag {} not found for delete request", tagId);
      }
     } catch (ElasticsearchException e) {
      if (e.status() == RestStatus.CONFLICT) {
        log.error("Conflict when deleting tag config {}", tagId, e);
      } else {
        log.error("Error when deleting tag config {}", tagId, e);
      }
    } catch (Exception e) {
      log.error("Error occurred while deleting the config for tag #{}", tagId, e);
    }
  }
}
