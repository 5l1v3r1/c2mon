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
package cern.c2mon.server.laser.publication;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import cern.laser.source.alarmsysteminterface.ASIException;
import cern.laser.source.alarmsysteminterface.AlarmSystemInterface;
import cern.c2mon.server.cache.AlarmCache;
import cern.c2mon.server.cache.ClusterCache;
import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.test.CacheObjectCreation;

/**
 * Unit test of LaserBackupPublisher.
 * 
 * @author Mark Brightwell
 *
 */
public class LaserBackupPublisherTest {

  private LaserBackupPublisher laserBackupPublisher;
  
  private AlarmCache alarmCache;
  
  private LaserPublisher laserPublisher;
  
  private IMocksControl controller;
  
  private AlarmSystemInterface asi;
  
  private ReentrantReadWriteLock backupLock = new ReentrantReadWriteLock();
  
  public LaserBackupPublisherTest() {
    super();
    System.setProperty("log4j.configuration",System.getProperty("log4j.configuration", "cern/c2mon/server/laser/publication/log4j.properties"));
 // IMPORTNANT 
 // --- we use the laser test system when submitting an alarm.
     System.setProperty("laser.hosts", "laser-test");
     System.setProperty("cmw.mom.brokerlist", "jms-diamon-test:2506");
 // ---
  }

  @Before
  public void beforeTest() {
    controller = EasyMock.createNiceControl();
    alarmCache = controller.createMock(AlarmCache.class);
    laserPublisher = controller.createMock(LaserPublisher.class);
    asi = controller.createMock(AlarmSystemInterface.class);
    ClusterCache clusterCache = controller.createMock(ClusterCache.class);
    laserBackupPublisher = new LaserBackupPublisher(alarmCache, laserPublisher, clusterCache);    
    laserBackupPublisher.setNbBackupThreads(2);
    laserBackupPublisher.setBackupInterval(60000);
    laserBackupPublisher.init();
  }
  
  @Test
  public void testBackup() throws InterruptedException, ASIException {  
    Alarm alarm1 = CacheObjectCreation.createTestAlarm1();
    alarm1.hasBeenPublished(new Timestamp(System.currentTimeMillis()));
    Alarm alarm2 = CacheObjectCreation.createTestAlarm2();    
    alarm2.hasBeenPublished(new Timestamp(System.currentTimeMillis()));
    EasyMock.expect(laserPublisher.getAsi()).andReturn(asi).times(2);
    asi.pushActiveList(EasyMock.isA(List.class));
    EasyMock.expect(alarmCache.getKeys()).andReturn(Arrays.asList(alarm1.getId(), alarm2.getId()));
    EasyMock.expect(alarmCache.getCopy(alarm1.getId())).andReturn(alarm1);
    EasyMock.expect(alarmCache.getCopy(alarm2.getId())).andReturn(alarm2);
    
    controller.replay();
    laserBackupPublisher.start(); //sets state to running and starts thread (not needed for test)
    Thread.sleep(3000);
    laserBackupPublisher.run(); //call run directly
    controller.verify();
  }
  
}
