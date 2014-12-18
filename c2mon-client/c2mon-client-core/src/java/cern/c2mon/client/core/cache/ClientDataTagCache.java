/*******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 * 
 * Copyright (C) 2004 - 2011 CERN. This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with this program; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 * 
 * Author: TIM team, tim.support@cern.ch
 ******************************************************************************/
package cern.c2mon.client.core.cache;

import java.util.Set;

import cern.c2mon.client.common.listener.DataTagUpdateListener;
import cern.c2mon.client.core.listener.TagSubscriptionListener;
import cern.c2mon.client.core.manager.TagManager;

/**
 * This interface describes the methods which are provided manipulate
 * the references in the client data tag cache.
 * <p>
 * <b>Please note</b>, that only the {@link TagManager} should use this
 * interface face! If your class needs absolutely to access the cache
 * it should use the {@link BasicCacheHandler} instead. 
 *
 * @author Matthias Braeger
 * @see BasicCacheHandler
 */
public interface ClientDataTagCache extends BasicCacheHandler {
  
  /**
   * Adds the given listener to the tags in the cache. If the tag is not yet known to the
   * client API it will fetch it from the server.
   * <p>
   * <b>Please note, that this method is synchronizing on the history lock.</b>
   * @param tagIds List of tag ids
   * @param listener The listener to be added to the <code>ClientDataTag</code> references
   * @throws NullPointerException If one of the parameter is <code>null</code> or if one of 
   *                              the tags is not present in the cache
   * @throws CacheSynchronizationException In case of communication problems with the C2MON
   *         server during the refresh process.
   * @see #getHistoryModeSyncLock();
   */
  <T extends DataTagUpdateListener> void addDataTagUpdateListener(Set<Long> tagIds, T listener) throws CacheSynchronizationException;
  
  /**
   * This method synchronizes subscribed data tags with the server.
   * It will ask the server to send the actual tag information for all subscribed data tags.
   * Once the cache is synchronized, all subscribed <code>DataTagUpdateListener</code> will
   * be notified.
   * @throws CacheSynchronizationException In case of communication problems with the C2MON
   *         server during the refresh process.
   */
  void refresh() throws CacheSynchronizationException;
  
  /**
   * This method synchronizes subscribed data tags with the server.
   * It will ask the server to send the actual tag information for all subscribed data tags.
   * Once the cache is synchronized, all subscribed <code>DataTagUpdateListener</code> will
   * be notified.
   * 
   * @param tagIds A set of data tag id's
   * @throws CacheSynchronizationException In case of communication problems with the C2MON
   *         server during the refresh process.
   */
  void refresh(Set<Long> tagIds) throws CacheSynchronizationException;
  
  /**
   * Unsubscribes the given listener from all cache objects. 
   * @param listener The listener which shall be unsubscribed.
   * @throws NullPointerException When the parameter is <code>null</code>
   */
  void unsubscribeAllDataTags(DataTagUpdateListener listener);
  
  /**
   * Unsubscribes the given listener from all tags specified by the
   * list of tag ids.
   * @param dataTagIds list of tag ids 
   * @param listener The listener which shall be unsubscribed.
   * @throws NullPointerException When the parameter is <code>null</code>
   */
  void unsubscribeDataTags(Set<Long> dataTagIds, DataTagUpdateListener listener);
  
  /**
   * Returns the cache size.
   * @return the cache size (this is the number of subscribed data 
   * tags contained in the cache).
   */
  int getCacheSize();
  
  /**
   * Registers a <code>TagSubscriptionListener</code>. 
   * @param listener The listener to be registered
   * @throws NullPointerException In case that the parameter is <code>null</code>.
   */
  void addTagSubscriptionListener(TagSubscriptionListener listener);
  
  /**
   * Unregisters a <code>TagSubscriptionListener</code>. 
   * @param listener The listener to be unregistered
   * @throws NullPointerException In case that the parameter is <code>null</code>.
   */
  void removeTagSubscriptionListener(TagSubscriptionListener listener);
}
