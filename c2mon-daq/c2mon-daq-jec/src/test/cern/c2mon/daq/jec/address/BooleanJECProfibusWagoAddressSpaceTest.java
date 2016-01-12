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
package cern.c2mon.daq.jec.address;

import org.junit.Test;

import cern.c2mon.daq.jec.address.BooleanJECProfibusWagoAddressSpace;
import static org.junit.Assert.*;

public class BooleanJECProfibusWagoAddressSpaceTest {

    private BooleanJECProfibusWagoAddressSpace addressSpace = new BooleanJECProfibusWagoAddressSpace();
    
    @Test
    public void testWordAndBitId() {
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 1, 10, 5));
        assertEquals(10, addressSpace.getMaxWordId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 0, 2, 3));
        assertEquals(10, addressSpace.getMaxWordId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 1, 11, 4));
        assertEquals(11, addressSpace.getMaxWordId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 1, 12, 8));
        assertEquals(12, addressSpace.getMaxWordId());
        addressSpace.reset();
        assertEquals(-1, addressSpace.getMaxWordId());
    }
    
    @Test
    public void testWordAndBitIdMMD() {
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 1, 10, 5));
        assertEquals(-1, addressSpace.getMaxMMDWordId());
        assertEquals(-1, addressSpace.getMaxMMDBitId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 0, 2, 3));
        assertEquals(2, addressSpace.getMaxMMDWordId());
        assertEquals(3, addressSpace.getMaxMMDBitId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("asd", 1, 11, 4));
        assertEquals(2, addressSpace.getMaxMMDWordId());
        assertEquals(3, addressSpace.getMaxMMDBitId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 1, 12, 2));
        assertEquals(12, addressSpace.getMaxMMDWordId());
        assertEquals(2, addressSpace.getMaxMMDBitId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 1, 12, 5));
        assertEquals(12, addressSpace.getMaxMMDWordId());
        assertEquals(5, addressSpace.getMaxMMDBitId());
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 1, 12, 4));
        assertEquals(12, addressSpace.getMaxMMDWordId());
        assertEquals(5, addressSpace.getMaxMMDBitId());
        addressSpace.reset();
        assertEquals(-1, addressSpace.getMaxMMDWordId());
        assertEquals(-1, addressSpace.getMaxMMDBitId());
    }
    
    @Test
    public void testGetNumberMMDModules() {
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 1, 11, 14));
        assertEquals(48, addressSpace.getNumberOfMMDModules());
        addressSpace.reset();
        addressSpace.updateAddressSpace(new TestPLCHardwareAddress("PWA", 1, 5, 2));
        assertEquals(21, addressSpace.getNumberOfMMDModules());
    }
    
    
}
