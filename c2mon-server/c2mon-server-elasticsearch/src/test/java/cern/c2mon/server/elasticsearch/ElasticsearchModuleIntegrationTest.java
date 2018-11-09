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
package cern.c2mon.server.elasticsearch;

import cern.c2mon.server.elasticsearch.config.BaseElasticsearchIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Alban Marguet
 */
@Slf4j
public class ElasticsearchModuleIntegrationTest extends BaseElasticsearchIntegrationTest {

  @Test
  public void testModuleStartup() throws IOException {
    List<String> indexData = getEmbeddedNode().fetchAllDocuments();
    assertEquals("Embedded node should not contain any documents before each test and start successfuly.",
            0, indexData.size());
  }
}
