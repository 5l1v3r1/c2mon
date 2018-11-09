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

import cern.c2mon.server.cache.TagFacadeGateway;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.server.elasticsearch.IndexManager;
import cern.c2mon.server.elasticsearch.MappingFactory;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.stereotype.Component;

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

  private final String configIndex;

  private final IndexManager indexManager;

  @Autowired
  private TagFacadeGateway tagFacadeGateway;
  @Autowired
  private TagConfigDocumentConverter converter;

  @Autowired
  public TagConfigDocumentIndexer(final ElasticsearchProperties properties, final IndexManager indexManager) {
    this.indexManager = indexManager;
    this.configIndex = properties.getTagConfigIndex();
  }

  public void indexTagConfig(final TagConfigDocument tag) {
    if (!indexManager.exists(configIndex)) {
      indexManager.create(configIndex, TYPE, MappingFactory.createTagConfigMapping());
    }

    if (!indexManager.index(configIndex, TYPE, tag.toString(), tag.getId(), tag.getId())) {
      log.error("Could not index '{} #{}' to index '{}'.", TYPE, tag.getId(), configIndex);
    }
  }

  public void updateTagConfig(TagConfigDocument tag) {
    if (!indexManager.exists(configIndex)) {
      indexManager.create(configIndex, TYPE, MappingFactory.createTagConfigMapping());
    }

    indexManager.update(configIndex, TYPE, tag.toString(), tag.getId());
  }

  public void removeTagConfigById(final Long tagId) {
    if (!indexManager.exists(configIndex)) {
      return;
    }

    indexManager.delete(configIndex, TYPE, String.valueOf(tagId), String.valueOf(tagId));
  }

  @ManagedOperation(description = "Re-indexes all tag configs from the cache to Elasticsearch")
  public void reindexAllTagConfigDocuments() {
    if (tagFacadeGateway == null) {
      throw new IllegalStateException("Tag Facade Gateway is null");
    }

    for (Long id : tagFacadeGateway.getKeys()) {
      Tag tag = tagFacadeGateway.getTag(id);
      converter.convert(tag, tagFacadeGateway.getAlarms(tag)).ifPresent(this::updateTagConfig);
    }
  }
}
