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

import cern.c2mon.server.cache.config.CacheModule;
import cern.c2mon.server.cache.dbaccess.config.CacheDbAccessModule;
import cern.c2mon.server.cache.loading.config.CacheLoadingModule;
import cern.c2mon.server.common.config.CommonModule;
import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.elasticsearch.ElasticsearchSuiteTest;
import cern.c2mon.server.elasticsearch.IndexManager;
import cern.c2mon.server.elasticsearch.IndexNameManager;
import cern.c2mon.server.elasticsearch.config.ElasticsearchModule;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.elasticsearch.util.EntityUtils;
import cern.c2mon.server.elasticsearch.util.IndexUtils;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Szymon Halastra
 * @author Justin Lewis Salmon
 */
@ContextConfiguration(classes = {
        CommonModule.class,
        CacheModule.class,
        CacheDbAccessModule.class,
        CacheLoadingModule.class,
        SupervisionModule.class,
        ElasticsearchModule.class,
        CachePopulationRule.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class TagConfigDocumentIndexerTests {

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

  private String indexName;
  private DataTagCacheObject tag;
  private TagConfigDocument document;

  @Before
  public void setUp() throws Exception {
    tag = (DataTagCacheObject) EntityUtils.createDataTag();
    document = converter.convert(tag).orElseThrow(()->new Exception("Tag conversion failed"));
    indexName = indexNameManager.indexFor(document);
  }

  @After
  public void tearDown() {
    EmbeddedElasticsearchManager.getEmbeddedNode().deleteIndex(indexName);
    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();
  }

  @Test
  public void addDataTag() throws Exception {
    indexer.indexTagConfig(document);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.",
            IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have one document inserted.", 1, indexData.size());
  }

  @Test
  public void updateDataTag() throws Exception {
    document.put("description", tag.getDescription() + " MODIFIED.");
    ((Map<String, Object>) document.get("metadata")).put("spam", "eggs");
    indexer.updateTagConfig(document);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created when trying to update non-existing one.",
            IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have one document inserted.", 1, indexData.size());

    JSONObject jsonObject = new JSONObject(indexData.get(0));

    assertEquals(tag.getDescription() + " MODIFIED.", jsonObject.getString("description"));
    assertEquals("eggs", jsonObject.getJSONObject("metadata").getString("spam"));
  }

  @Test
  public void removeDataTag() throws Exception {
    indexer.indexTagConfig(document);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have been created.", 1, indexData.size());

    indexer.removeTagConfigById(tag.getId());

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should exist after tag config deletion.",
            IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index documents should been deleted.", 0, indexData.size());
  }
}
