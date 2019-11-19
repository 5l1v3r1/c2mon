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
package cern.c2mon.server.elasticsearch.config;

import cern.c2mon.server.cache.config.CacheModule;
import cern.c2mon.server.cache.dbaccess.config.CacheDbAccessModule;
import cern.c2mon.server.cache.loading.config.CacheLoadingModule;
import cern.c2mon.server.common.config.CommonModule;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EmbeddedElasticsearchManager;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    CommonModule.class,
    CacheModule.class,
    CacheDbAccessModule.class,
    CacheLoadingModule.class,
    SupervisionModule.class,
    ElasticsearchModule.class,
    CachePopulationRule.class
})
public abstract class BaseElasticsearchIntegrationTest {

  private static ElasticsearchProperties properties = new ElasticsearchProperties();

  protected String indexName;

  @After
  public void tearDown() {
    getEmbeddedNode().deleteIndex(indexName);
    getEmbeddedNode().refreshIndices();
  }

  protected EmbeddedElastic getEmbeddedNode() {
    return EmbeddedElasticsearchManager.getEmbeddedNode();
  }

  protected boolean doesIndexExist(String indexName) throws IOException {
    HttpHead httpRequest = new HttpHead(("http://" + properties.getHost() + ":" + properties.getHttpPort() + "/" + indexName));
    HttpClient httpClient = HttpClientBuilder.create().build();
    HttpResponse httpResponse = httpClient.execute(httpRequest);
    return httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
  }

  protected static ElasticsearchProperties getProperties() {
    return properties;
  }
}
