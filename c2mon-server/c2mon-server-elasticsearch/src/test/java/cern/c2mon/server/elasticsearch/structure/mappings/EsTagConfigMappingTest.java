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

package cern.c2mon.server.elasticsearch.structure.mappings;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import cern.c2mon.server.elasticsearch.structure.types.tag.EsTag;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Szymon Halastra
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class EsTagConfigMappingTest {

  private final String expectedMapping = "{\n" +
          "  \"properties\": {\n" +
          "    \"id\": {\n" +
          "      \"type\": \"long\"\n" +
          "    },\n" +
          "    \"name\": {\n" +
          "      \"type\": \"string\",\n" +
          "      \"index\": \"not_analyzed\"\n" +
          "    },\n" +
          "    \"type\": {\n" +
          "      \"type\": \"string\",\n" +
          "      \"index\": \"not_analyzed\"\n" +
          "    },\n" +
          "    \"unit\": {\n" +
          "      \"type\": \"string\",\n" +
          "      \"index\": \"not_analyzed\"\n" +
          "    },\n" +
          "    \"c2mon\": {\n" +
          "      \"dynamic\": \"false\",\n" +
          "      \"type\": \"object\",\n" +
          "      \"properties\": {\n" +
          "        \"daqTimestamp\": {\n" +
          "          \"type\": \"date\",\n" +
          "          \"format\": \"epoch_millis\"\n" +
          "        },\n" +
          "        \"process\": {\n" +
          "          \"type\": \"string\",\n" +
          "          \"index\": \"not_analyzed\"\n" +
          "        },\n" +
          "        \"subEquipment\": {\n" +
          "          \"type\": \"string\",\n" +
          "          \"index\": \"not_analyzed\"\n" +
          "        },\n" +
          "        \"serverTimestamp\": {\n" +
          "          \"type\": \"date\",\n" +
          "          \"format\": \"epoch_millis\"\n" +
          "        },\n" +
          "        \"sourceTimestamp\": {\n" +
          "          \"type\": \"date\",\n" +
          "          \"format\": \"epoch_millis\"\n" +
          "        },\n" +
          "        \"dataType\": {\n" +
          "          \"type\": \"string\",\n" +
          "          \"index\": \"not_analyzed\"\n" +
          "        },\n" +
          "        \"equipment\": {\n" +
          "          \"type\": \"string\",\n" +
          "          \"index\": \"not_analyzed\"\n" +
          "        }\n" +
          "      }\n" +
          "    },\n" +
          "    \"metadata\": {\n" +
          "      \"dynamic\": \"true\",\n" +
          "      \"type\": \"nested\"\n" +
          "    }\n" +
          "  }\n" +
          "}";

  @Test
  public void testOutput() {
    EsTagConfigMapping mapping = new EsTagConfigMapping(EsTag.TYPE_STRING, String.class.getName());
    assertEquals(expectedMapping, mapping.getMapping());

    EsTagConfigMapping mapping2 = new EsTagConfigMapping(EsTag.TYPE_STRING, "String");
    assertEquals(expectedMapping, mapping2.getMapping());
  }
}
