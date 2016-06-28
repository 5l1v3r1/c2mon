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
package cern.c2mon.server.cache.subequipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.server.cache.dbaccess.SubEquipmentMapper;
import cern.c2mon.server.common.subequipment.SubEquipment;
import cern.c2mon.server.common.subequipment.SubEquipmentCacheObject;

/**
 * Integration test of the SubEquipmentCache
 * with the cache loading and DB access
 * modules.
 * 
 * @author Mark Brightwell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ContextConfiguration({"classpath:cern/c2mon/server/cache/config/server-cache-subequipment-test.xml"})
public class SubEquipmentCacheTest {

  @Autowired
  private SubEquipmentMapper subEquipmentMapper;
  
  @Autowired
  private SubEquipmentCacheImpl subEquipmentCache;
  
  @Test
  public void testCacheLoading() {
    assertNotNull(subEquipmentCache);
    
    List<SubEquipment> subEquipmentList = subEquipmentMapper.getAll(); //IN FACT: GIVES TIME FOR CACHE TO FINISH LOADING ASYNCH BEFORE COMPARISON BELOW...
    
    //test the cache is the same size as in DB
    assertEquals(subEquipmentList.size(), subEquipmentCache.getCache().getKeys().size());
    //compare all the objects from the cache and buffer
    Iterator<SubEquipment> it = subEquipmentList.iterator();
    while (it.hasNext()) {
      SubEquipmentCacheObject currentSubEquipment = (SubEquipmentCacheObject) it.next();
      //only compares one field so far
      assertEquals(currentSubEquipment.getName(), (((SubEquipment) subEquipmentCache.getCopy(currentSubEquipment.getId())).getName()));
    }
  }
  
}
