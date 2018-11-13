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
package cern.c2mon.server.elasticsearch.alarm;

import cern.c2mon.pmanager.persistence.exception.IDBPersistenceException;
import cern.c2mon.server.cache.config.CacheModule;
import cern.c2mon.server.cache.dbaccess.config.CacheDbAccessModule;
import cern.c2mon.server.cache.loading.config.CacheLoadingModule;
import cern.c2mon.server.common.alarm.AlarmCacheObject;
import cern.c2mon.server.common.config.CommonModule;
import cern.c2mon.server.elasticsearch.ElasticsearchSuiteTest;
import cern.c2mon.server.elasticsearch.IndexNameManager;
import cern.c2mon.server.elasticsearch.config.ElasticsearchModule;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.elasticsearch.util.EntityUtils;
import cern.c2mon.server.elasticsearch.util.IndexUtils;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

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
public class AlarmDocumentIndexerTests {

  @Autowired
  private IndexNameManager indexNameManager;

  @Autowired
  private AlarmDocumentIndexer indexer;

  private String indexName;
  private AlarmDocument document;

  @Before
  public void setUp() {
    AlarmCacheObject alarm = (AlarmCacheObject) EntityUtils.createAlarm();
    alarm.setTimestamp(new Timestamp(0));
    document = new AlarmValueDocumentConverter().convert(alarm);
    indexName = indexNameManager.indexFor(document);
  }

  @After
  public void tearDown() {
    EmbeddedElasticsearchManager.getEmbeddedNode().deleteIndex(indexName);
    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();
  }

  @Test
  public void indexSingleAlarmTest() throws IDBPersistenceException, IOException {
    indexer.storeData(document);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.", IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    Assert.assertEquals("Index should have one document inserted.", 1, indexData.size());
  }

  @Test
  public void indexMultipleAlarmTest() throws IDBPersistenceException, IOException {
    indexer.storeData(document);
    indexer.storeData(document);

    EmbeddedElasticsearchManager.getEmbeddedNode().refreshIndices();

    assertTrue("Index should have been created.", IndexUtils.doesIndexExist(indexName, ElasticsearchSuiteTest.getProperties()));

    List<String> indexData = EmbeddedElasticsearchManager.getEmbeddedNode().fetchAllDocuments(indexName);
    Assert.assertEquals("Index should have two documents inserted.", 2, indexData.size());
  }
}
