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

import cern.c2mon.server.elasticsearch.bulk.BulkProcessorProxy;
import cern.c2mon.server.elasticsearch.bulk.BulkProcessorProxyDummyImpl;
import cern.c2mon.server.elasticsearch.bulk.BulkProcessorProxyImpl;
import cern.c2mon.server.elasticsearch.bulk.rest.RestBulkProcessorProxy;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClient;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClientDummyImpl;
import cern.c2mon.server.elasticsearch.client.ElasticsearchClientImpl;
import cern.c2mon.server.elasticsearch.client.rest.RestElasticSearchClient;
import org.elasticsearch.node.NodeValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * This class is responsible for configuring the Spring context for the
 * Elasticsearch module.
 *
 * @author Justin Lewis Salmon
 * @author Alban Marguet
 */
@Configuration
@Import({
    ElasticsearchPersistenceConfig.class
})
@EnableConfigurationProperties(ElasticsearchProperties.class)
@ComponentScan("cern.c2mon.server.elasticsearch")
public class ElasticsearchModule {

  @Bean
  ElasticsearchClient getElasticSearchClient(@Autowired ElasticsearchProperties elasticsearchProperties) throws NodeValidationException {
    if (elasticsearchProperties.isEnabled()) {
      if (elasticsearchProperties.isRest()) {
        return new RestElasticSearchClient(elasticsearchProperties);
      } else {
        return new ElasticsearchClientImpl(elasticsearchProperties);
      }
    } else {
      return new ElasticsearchClientDummyImpl();
    }
  }

  @Bean
  BulkProcessorProxy getBulkProcessor(@Autowired ElasticsearchClient elasticsearchClient, @Autowired ElasticsearchProperties elasticsearchProperties) {
    if (elasticsearchProperties.isEnabled()) {
      if (elasticsearchProperties.isRest()) {
        return new RestBulkProcessorProxy((RestElasticSearchClient) elasticsearchClient, elasticsearchProperties);
      } else {
        return new BulkProcessorProxyImpl((ElasticsearchClientImpl) elasticsearchClient, elasticsearchProperties);
      }
    }
    return new BulkProcessorProxyDummyImpl();
  }
}
