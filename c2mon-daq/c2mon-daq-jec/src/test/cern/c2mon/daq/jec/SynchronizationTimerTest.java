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
package cern.c2mon.daq.jec;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;

import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.jec.PLCObjectFactory;
import cern.c2mon.daq.jec.SynchronizationTimer;
import cern.c2mon.daq.jec.config.PLCConfiguration;
import cern.c2mon.daq.jec.plc.StdConstants;
import cern.c2mon.daq.jec.plc.TestPLCDriver;

public class SynchronizationTimerTest {

    private SynchronizationTimer synchronizationTimer;
    private PLCObjectFactory plcFactory;
    
    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        EquipmentLogger equipmentLogger =  new EquipmentLogger("asd", "asd", "asd");
        PLCConfiguration plcConfiguration = new PLCConfiguration();
        plcConfiguration.setProtocol("TestPLCDriver");
        plcFactory = new PLCObjectFactory(plcConfiguration);
        synchronizationTimer = new SynchronizationTimer(equipmentLogger, plcFactory);
    }
    
    @Test
    public void testAdjustment() {
        if (new GregorianCalendar().getTimeZone().inDaylightTime(new Date())) {
            GregorianCalendar calendar = new GregorianCalendar(2010, 12, 1);
            Date winterDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(winterDate);
        }
        else {
            GregorianCalendar calendar = new GregorianCalendar(2010, 6, 1);
            Date summerDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(summerDate);
        }
        TestPLCDriver driver = (TestPLCDriver) plcFactory.getPLCDriver();
        assertEquals(driver.getLastSend().getMsgID(), StdConstants.SET_TIME_MSG);
    }
    
    @Test
    public void testNoAdjustment() {
        if (new GregorianCalendar().getTimeZone().inDaylightTime(new Date())) {
            GregorianCalendar calendar = new GregorianCalendar(2010, 6, 1);
            Date summerDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(summerDate);
        }
        else {
            GregorianCalendar calendar = new GregorianCalendar(2010, 12, 1);
            Date winterDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(winterDate);
        }
        TestPLCDriver driver = (TestPLCDriver) plcFactory.getPLCDriver();
        assertNull(driver.getLastSend());
    }
}
