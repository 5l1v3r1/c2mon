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
import cern.c2mon.server.elasticsearch.IndicesRest;
import cern.c2mon.server.elasticsearch.MappingFactory;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.supervision.config.SupervisionModule;
import org.elasticsearch.client.Response;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

  //the embedded ES node will start
  //when the client is instantiatied (magically by Spring)
  //we don't shutdown the embedded server at the end of each
  //test because it may be used by other tests.
  @Autowired
  protected ElasticsearchClient client;

  @Before
  public void waitForElasticSearch() throws InterruptedException, ExecutionException {
    try {
      CompletableFuture<Void> nodeReady = CompletableFuture.runAsync(() -> {
        //client.waitForYellowStatus();
        ElasticsearchProperties elasticsearchProperties = this.client.getProperties();
        try {
          Response response = this.client.getLowLevelRestClient().performRequest("DELETE", "/" + elasticsearchProperties.getTagConfigIndex());
        } catch (IOException e) {
          e.printStackTrace();
        }

//        client.getClient().admin().indices().delete(new DeleteIndexRequest(elasticsearchProperties.getTagConfigIndex()));
        IndicesRest.create(elasticsearchProperties.getTagConfigIndex(), "tag_config", MappingFactory.createTagConfigMapping());

      });
      nodeReady.get(120, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException("Timeout when waiting for embedded elasticsearch!");
    }
  }
}
