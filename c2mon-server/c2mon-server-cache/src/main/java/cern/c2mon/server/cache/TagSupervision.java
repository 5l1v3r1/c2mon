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
package cern.c2mon.server.cache;

import java.sql.Timestamp;

import cern.c2mon.server.common.tag.Tag;

/**
 * Interface of bean used for modifying a Tag object to
 * take into account the Supervision status. Should be 
 * used for rule and datatags by cache listeners. The
 * Tag parameter in all the methods below is modified.
 * 
 * <p>The methods will modify the quality of the tag object
 * appropriately. Listeners are not notified.
 * 
 * <p>IMPORTANT: do not use for updating objects that are
 * living in the cache. The facade beans should be used
 * for that purpose.
 * 
 * @author Mark Brightwell
 *
 */
public interface TagSupervision {

  /**
   * Call when a DAQ is detected as DOWN.
   * 
   * @param tag the tag object to modify
   * @param message the quality message
   * @param timestamp the new timestamp (becomes cache timestamp)
   */
  void onProcessDown(Tag tag, String message, Timestamp timestamp);
  /**
   * Call when a DAQ is detected as UP.
   * 
   * @param tag the tag object to modify
   * @param message the quality message
   * @param timestamp the new timestamp (becomes cache timestamp)
   */
  void onProcessUp(Tag tag, String message, Timestamp timestamp);
  
  /**
   * Modifies Tag object appropriately when equipment is
   * detected as down.
   * 
   * @param tag the tag object to modify
   * @param message the quality message
   * @param timestamp the new timestamp (becomes cache timestamp)
   */
  void onEquipmentDown(Tag tag, String message, Timestamp timestamp);
  
  /**
   * Modifies Tag object appropriately when equipment is
   * detected as up.
   * 
   * @param tag the tag object to modify
   * @param message the quality message
   * @param timestamp the new timestamp (becomes cache timestamp)
   */
  void onEquipmentUp(Tag tag, String message, Timestamp timestamp);
  
}
