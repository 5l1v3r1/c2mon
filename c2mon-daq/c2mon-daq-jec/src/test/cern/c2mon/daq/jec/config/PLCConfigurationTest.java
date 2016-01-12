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
package cern.c2mon.daq.jec.config;

import static org.junit.Assert.*;

import org.junit.Test;


public class PLCConfigurationTest {
    
    private PLCConfiguration plcConfiguration = new PLCConfiguration();
    
    private String addressString = "plc_name=plcstaa05,plcstaa06;" +
    		"Protocol=SiemensISO;Time_sync=Jec;Port=102;S_tsap=TCP-1;" +
    		"D_tsap=TCP-1;Alive_handler_period=5000;" +
    		"Dp_slave_address=4,5,6,7,8,9;";
    
    @Test
    public void testConfig() throws Exception {
        assertEquals(PLCConfiguration.DEFAULT_HANDLER_PERIOD, plcConfiguration.getHandlerPeriod());
        plcConfiguration.parsePLCAddress(addressString);
        assertEquals("plcstaa05", plcConfiguration.getPlcName());
        assertEquals("plcstaa06", plcConfiguration.getPlcNameRed());
        assertEquals("SiemensISO", plcConfiguration.getProtocol());
        assertEquals("Jec", plcConfiguration.getTimeSync());
        assertEquals(102, plcConfiguration.getPort());
        assertEquals("TCP-1", plcConfiguration.getsTsap());
        assertEquals("TCP-1", plcConfiguration.getdTsap());
        assertEquals(5000, plcConfiguration.getHandlerPeriod());
        assertEquals(6, plcConfiguration.getDpSlaveAddresses().size());
        boolean containsAllDpAddresses = false;
        for (int i = 4; i < 10; i++) {
            int dpSlaveAddress = plcConfiguration.getDpSlaveAddresses().get(i - 4);
            containsAllDpAddresses = dpSlaveAddress == i;
            if (!containsAllDpAddresses)
                break;
        }
        assertTrue(containsAllDpAddresses);
    }

}
