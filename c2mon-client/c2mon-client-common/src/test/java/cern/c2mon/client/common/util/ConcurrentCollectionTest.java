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
package cern.c2mon.client.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for the class {@link ConcurrentIdentitySet}
 * 
 * @author vdeila
 */
public class ConcurrentCollectionTest extends TestCase {

  /** The class to test */
  private ConcurrentIdentitySet<Long> list;
  
  /** The expected list of values */
  private List<Long> expectedList = new ArrayList<Long>();
  
  protected void setUp() throws Exception {
    list = new ConcurrentIdentitySet<Long>();
    expectedList = new ArrayList<Long>();
    for (long i = 0; i < 10; i++) {
      expectedList.add(i);
    }
    list.addAll(expectedList);
  }

  public void testAddRemove() {
    final int startSize = list.size();
    final Long element = 50L;
    assertTrue(list.add(element));
    assertEquals(startSize + 1, list.size());
    assertTrue(list.contains(element));
    assertTrue(list.remove(element));
    assertFalse(list.contains(element));
    assertEquals(startSize, list.size());
  }

  public void testAddAll() {
    final List<Long> addAll = new ArrayList<Long>(Arrays.asList(50L, 51L, 52L, 53L));
    assertTrue(list.addAll(addAll));
    assertTrue(list.containsAll(addAll));
  }

  public void testClear() {
    assertFalse(list.isEmpty());
    list.clear();
    assertTrue(list.isEmpty());
  }
  
  public void testIterator() {
    final Iterator<Long> iterator = list.iterator();
    assertNotNull(iterator);
    while (iterator.hasNext()) {
      assertTrue(expectedList.contains(iterator.next()));
    }
  }

  public void testRemoveAll() {
    final List<Long> removeList = expectedList.subList(0, 4);
    list.removeAll(removeList);
    for (Long element : expectedList) {
      assertEquals(removeList.contains(element), !list.contains(element));
    }
  }

  public void testRetainAll() {
    final List<Long> retainList = Arrays.asList(8L, 9L, 10L, 15L);
    list.retainAll(retainList);
    for (Long element : list) {
      assertTrue(retainList.contains(element));
    }
  }

  public void testToArray() {
    for (Object obj : list.toArray()) {
      assertTrue(expectedList.contains(obj));
    }
  }

  public void testToArrayTArray() {
    for (Object obj : list.toArray(new Long[0])) {
      assertTrue(expectedList.contains(obj));
    }
  }

}
