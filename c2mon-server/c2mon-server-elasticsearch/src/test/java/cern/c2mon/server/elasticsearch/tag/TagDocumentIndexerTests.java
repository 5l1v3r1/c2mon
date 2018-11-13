/******************************************************************************
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
 *****************************************************************************/
package cern.c2mon.server.elasticsearch.tag;

import cern.c2mon.pmanager.persistence.exception.IDBPersistenceException;
import cern.c2mon.server.cache.config.CacheModule;
import cern.c2mon.server.cache.dbaccess.config.CacheDbAccessModule;
import cern.c2mon.server.cache.loading.config.CacheLoadingModule;
import cern.c2mon.server.common.config.CommonModule;
import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.elasticsearch.ElasticsearchSuiteTest;
import cern.c2mon.server.elasticsearch.IndexNameManager;
import cern.c2mon.server.elasticsearch.config.ElasticsearchModule;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.elasticsearch.util.EntityUtils;
import cern.c2mon.server.elasticsearch.util.IndexUtils;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alban Marguet
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
public class TagDocumentIndexerTests {

  @Autowired
  private TagDocumentIndexer indexer;

  @Autowired
  private IndexNameManager indexNameManager;

  @Rule
  @Autowired
  public CachePopulationRule cachePopulationRule;

  @Autowired
  private TagDocumentConverter converter;

  private String indexName;
  private TagDocument document;

  @Before
  public void setUp() {
    DataTagCacheObject tag = (DataTagCacheObject) EntityUtils.createDataTag();
    document = converter.convert(tag).orElseThrow(() -> new IllegalArgumentException("TagDocument conversion failed"));
    indexName = indexNameManager.indexFor(document);
  }

  @After
  public void tearDown() {
    EmbeddedElasticsearchManager.getEmbeddedNode().deleteIndex(indexName);
    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();
  }

  @Test
  public void indexSingleTagTest() throws IDBPersistenceException, InterruptedException, IOException {
    indexer.storeData(document);

    // Bulk flush operation seem to require more time
    Thread.sleep(1000l);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.",
            IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have one document inserted.", 1, indexData.size());
  }

  @Test
  public void indexMultipleTagsTest() throws IDBPersistenceException, InterruptedException, IOException {
    indexer.storeData(document);
    indexer.storeData(document);

    // Bulk flush operation seem to require more time
    Thread.sleep(1000l);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.",
            IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    assertEquals("Index should have two documents inserted.", 2, indexData.size());
  }
}
