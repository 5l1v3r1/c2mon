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

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cern.c2mon.server.elasticsearch.Indices;
import cern.c2mon.server.elasticsearch.MappingFactory;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;

import java.io.IOException;

import static org.elasticsearch.action.DocWriteRequest.*;

/**
 * This class manages the indexing of {@link TagConfigDocument} instances to
 * the Elasticsearch cluster.
 *
 * @author Szymon Halastra
 * @author Justin Lewis Salmon
 */
@Slf4j
@Component
public class TagConfigDocumentIndexer {

  private static final String TYPE = "tag_config";

  private final ElasticsearchClient client;

  private final String configIndex;

  @Autowired
  public TagConfigDocumentIndexer(final ElasticsearchClient client, final ElasticsearchProperties properties) {
    this.client = client;
    this.configIndex = properties.getTagConfigIndex();
  }

  public void indexTagConfig(TagConfigDocument tag) {
    if (!Indices.exists(configIndex)) {
      Indices.create(configIndex, TYPE, MappingFactory.createTagConfigMapping());
    }

    IndexRequest request = new IndexRequest(configIndex);

    request.source(tag.toString(), XContentType.JSON);
    request.id(tag.getId());
    request.type("supervision");
    request.opType(OpType.CREATE);

    RestHighLevelClient restClient = this.client.getRestClient();
    try {
      IndexResponse response = restClient.index(request);
      if (!response.status().equals(RestStatus.CREATED)) {
       log.error("Error occurred while indexing the config for tag #{}", tag.getId());
      }
    } catch (IOException e) {
      log.error("Could not index supervision event #{} to index {}", tag.getId(), configIndex, e);
    }
  }

  public void updateTagConfig(TagConfigDocument tag) {
    IndexRequest request = new IndexRequest(configIndex);

    request.source(tag.toString(), XContentType.JSON);
    request.id(tag.getId());
    request.type("supervision");

    RestHighLevelClient restClient = this.client.getRestClient();
    try {
      IndexResponse response = restClient.index(request);
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

  public void removeTagConfig(TagConfigDocument tag) {
    if (!Indices.exists(this.configIndex)) {
      return;
    }

    DeleteRequest deleteRequest = new DeleteRequest(configIndex, TYPE, tag.getId()).routing(tag.getId());
    try {
      DeleteResponse deleteResponse = this.client.getRestClient().delete(deleteRequest);
      if (deleteResponse.status().equals(RestStatus.NOT_FOUND)) {
        log.warn("Tag {} not found for delete request", tag.getId());
      }
     } catch (ElasticsearchException e) {
      if (e.status() == RestStatus.CONFLICT) {
        log.error("Conflict when deleting tag config {}", tag.getId(), e);
      } else {
        log.error("Error when deleting tag config {}", tag.getId(), e);
      }
    } catch (Exception e) {
      log.error("Error occurred while deleting the config for tag #{}", tag.getId(), e);
    }
  }
}
