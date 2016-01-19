/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * <p/>
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * <p/>
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.server.eslog.structure.queries;

import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.eslog.structure.converter.AlarmESLogConverter;
import cern.c2mon.server.eslog.structure.types.AlarmES;
import cern.c2mon.server.test.CacheObjectCreation;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Alban Marguet
 */
public class AlarmESQueryTest {
  private String jsonSource;
  AlarmESQuery query;
  Client client;
  AlarmESLogConverter alarmESLogConverter;
  Alarm alarm;
  AlarmES alarmES;

  @Before
  public void setup() {
    alarm = CacheObjectCreation.createTestAlarm1();
    alarmESLogConverter = new AlarmESLogConverter();
    alarmES = alarmESLogConverter.convertAlarmToAlarmES(alarm);

    query = new AlarmESQuery(client, alarmES);
    jsonSource = alarmES.toString();
  }

  @Test
  public void testCorrectOutput() {
    assertEquals(jsonSource, query.getJsonSource());
  }
}