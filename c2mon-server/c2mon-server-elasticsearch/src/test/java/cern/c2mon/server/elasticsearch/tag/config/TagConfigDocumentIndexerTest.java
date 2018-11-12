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

import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.elasticsearch.IndexManager;
import cern.c2mon.server.elasticsearch.IndexNameManager;
import cern.c2mon.server.elasticsearch.config.BaseElasticsearchIntegrationTest;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Szymon Halastra
 * @author Justin Lewis Salmon
 */
public class TagConfigDocumentIndexerTest extends BaseElasticsearchIntegrationTest {

  @Autowired
  private IndexNameManager indexNameManager;

  @Autowired
  private IndexManager indexManager;

  @Autowired
  private TagConfigDocumentIndexer indexer;

  @Autowired
  private TagConfigDocumentConverter converter;

  @Rule
  @Autowired
  public CachePopulationRule cachePopulationRule;

  private DataTagCacheObject tag;
  private TagConfigDocument document;

  @Before
  public void setUp() throws Exception {
    tag = (DataTagCacheObject) EntityUtils.createDataTag();
    document = converter.convert(tag).orElseThrow(()->new Exception("Tag conversion failed"));
    indexName = indexNameManager.indexFor(document);
  }

  @Test
  public void addDataTag() throws Exception {
    indexer.indexTagConfig(document);

    getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.", doesIndexExist(indexName));

    List<String> indexData = getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have one document inserted.", 1, indexData.size());
  }

  @Test
  public void updateDataTag() throws Exception {
    document.put("description", tag.getDescription() + " MODIFIED.");
    ((Map<String, Object>) document.get("metadata")).put("spam", "eggs");
    indexer.updateTagConfig(document);

    getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created when trying to update non-existing one.", doesIndexExist(indexName));

    List<String> indexData = getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have one document inserted.", 1, indexData.size());

    JSONObject jsonObject = new JSONObject(indexData.get(0));

    assertEquals(tag.getDescription() + " MODIFIED.", jsonObject.getString("description"));
    assertEquals("eggs", jsonObject.getJSONObject("metadata").getString("spam"));
  }

  @Test
  public void removeDataTag() throws Exception {
    indexer.indexTagConfig(document);

    getEmbeddedNode().refreshIndices();

    List<String> indexData = getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have been created.", 1, indexData.size());

    indexer.removeTagConfigById(tag.getId());

    getEmbeddedNode().refreshIndices();

    assertTrue("Index should exist after tag config deletion.", doesIndexExist(indexName));

    indexData = getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index documents should been deleted.", 0, indexData.size());
  }
}
