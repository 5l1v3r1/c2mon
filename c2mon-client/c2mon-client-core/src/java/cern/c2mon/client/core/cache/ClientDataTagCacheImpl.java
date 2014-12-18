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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cern.c2mon.client.common.listener.DataTagListener;
import cern.c2mon.client.common.listener.DataTagUpdateListener;
import cern.c2mon.client.common.tag.ClientDataTag;
import cern.c2mon.client.common.tag.ClientDataTagValue;
import cern.c2mon.client.core.listener.TagSubscriptionListener;
import cern.c2mon.client.core.manager.CoreSupervisionManager;
import cern.c2mon.client.core.tag.ClientDataTagImpl;

/**
 * This class implements the cache of the C2MON client API. The public method
 * provided by this class are never accessed directly by the application layer.
 * Only the <code>TagManager</code> has a reference to the cache service and is
 * controlling the access to it.
 * <p>
 * The cache provides a <code>create()</code> method for creating a new
 * <code>ClientDataTag</code> cache entry. In the background it handles also the
 * subscription to the incoming live events. Only the initialization of the tag
 * is performed by the <code>TagManager</code>.
 * <p>
 * It is possible to switch the <code>ClientDataTagCache</code> from live mode
 * into history mode and back. Therefore this class manages internally two
 * <code>ClientDataTag</code> map instances, one for live tag updates and the
 * other for historical events. Depending on the cache mode the getter methods
 * return either references to the live tags or to the history tags.
 *
 * @author Matthias Braeger
 */
@Service
public class ClientDataTagCacheImpl implements ClientDataTagCache {

  /** Log4j Logger for this class */
  private static final Logger LOG = Logger.getLogger(ClientDataTagCacheImpl.class);

  /** The cache controller manages the cache references */
  private final CacheController controller;

  /** The cache Synchronizer */
  private final CacheSynchronizer cacheSynchronizer;

  /** Reference to the supervision manager singleton */
  private final CoreSupervisionManager supervisionManager;
  
  /** Lock for accessing the <code>listeners</code> variable */
  private ReentrantReadWriteLock listenersLock = new ReentrantReadWriteLock();
  
  /** List of subscribed listeners */
  private final Set<TagSubscriptionListener> tagSubscriptionListeners = new HashSet<TagSubscriptionListener>();

  /**
   * <code>Map</code> reference containing all subscribed data tags which are
   * updated via the <code>JmsProxy</code>
   */
  private Map<Long, ClientDataTagImpl> liveCache = null;

  /** Reference to the cache read lock */
  private ReadLock cacheReadLock = null;

  /** Reference to the cache write lock */
  private WriteLock cacheWriteLock = null;

  /**
   * Default Constructor used by Spring to wire in the references to other
   * Services.
   *
   * @param pCacheController Provides access to the different cache instances
   *          and to the thread locks.
   * @param pCacheSynchronizer Handles the cache synchronization with the C2MON
   *          server
   * @param pSupervisionManager Handles registration of supervision listeners on
   *          data tags
   */
  @Autowired
  protected ClientDataTagCacheImpl(final CacheController pCacheController,
                                   final CacheSynchronizer pCacheSynchronizer,
                                   final CoreSupervisionManager pSupervisionManager) {
    this.controller = pCacheController;
    this.cacheSynchronizer = pCacheSynchronizer;
    this.supervisionManager = pSupervisionManager;
  }

  /**
   * This method is called by Spring after having created this service.
   */
  @PostConstruct
  protected void init() {
    cacheReadLock = controller.getReadLock();
    cacheWriteLock = controller.getWriteLock();
    liveCache = controller.getLiveCache();
  }

  @Override
  public ClientDataTag get(final Long tagId) {
    ClientDataTag cdt = null;

    cacheReadLock.lock();
    try {
      cdt = controller.getActiveCache().get(tagId);
    } finally {
      cacheReadLock.unlock();
    }

    return cdt;
  }

  @Override
  public Collection<ClientDataTag> getAllSubscribedDataTags() {
    Collection<ClientDataTag> list = new ArrayList<ClientDataTag>(controller.getActiveCache().size());

    cacheReadLock.lock();
    try {
      for (ClientDataTagImpl cdt : controller.getActiveCache().values()) {
        if (cdt.hasUpdateListeners()) {
          list.add(cdt);
        }
      }
    } finally {
      cacheReadLock.unlock();
    }

    return list;
  }

  @Override
  public Collection<ClientDataTag> getAllTagsForEquipment(final Long equipmentId) {
    Collection<ClientDataTag> list = new ArrayList<ClientDataTag>();

    cacheReadLock.lock();
    try {
      for (ClientDataTag cdt : controller.getActiveCache().values()) {
        if (cdt.getEquipmentIds().contains(equipmentId)) {
          list.add(cdt);
        }
      }
    } finally {
      cacheReadLock.unlock();
    }

    return list;
  }

  @Override
  public Collection<ClientDataTag> getAllTagsForListener(final DataTagUpdateListener listener) {
    Collection<ClientDataTag> list = new ArrayList<ClientDataTag>();

    cacheReadLock.lock();
    try {
      for (ClientDataTagImpl cdt : controller.getActiveCache().values()) {
        if (cdt.isUpdateListenerRegistered(listener)) {
          list.add(cdt);
        }
      }
    } finally {
      cacheReadLock.unlock();
    }

    return list;
  }

  @Override
  public Set<Long> getAllTagIdsForListener(final DataTagUpdateListener listener) {
    Set<Long> list = new HashSet<Long>();

    cacheReadLock.lock();
    try {
      for (ClientDataTagImpl cdt : controller.getActiveCache().values()) {
        if (cdt.isUpdateListenerRegistered(listener)) {
          list.add(cdt.getId());
        }
      }
    } finally {
      cacheReadLock.unlock();
    }

    return list;
  }

  @Override
  public Collection<ClientDataTag> getAllTagsForProcess(final Long processId) {
    Collection<ClientDataTag> list = new ArrayList<ClientDataTag>();

    cacheReadLock.lock();
    try {
      for (ClientDataTag cdt : controller.getActiveCache().values()) {
        if (cdt.getProcessIds().contains(processId)) {
          list.add(cdt);
        }
      }
    } finally {
      cacheReadLock.unlock();
    }

    return list;
  }

  @Override
  public void refresh() throws CacheSynchronizationException {
    cacheSynchronizer.refresh(null);
  }

  @Override
  public void refresh(final Set<Long> tagIds) throws CacheSynchronizationException {
    cacheSynchronizer.refresh(tagIds);
  }

  @Override
  public void unsubscribeAllDataTags(final DataTagUpdateListener listener) {
    Set<Long> tagsToRemove = new HashSet<Long>();
    cacheWriteLock.lock();
    try {
      for (ClientDataTagImpl cdt : controller.getActiveCache().values()) {
        if (cdt.isUpdateListenerRegistered(listener)) {
          cdt.removeUpdateListener(listener);
          if (!cdt.hasUpdateListeners()) {
            tagsToRemove.add(cdt.getId());
          }
        }
      }

      // Remove from cache
      cacheSynchronizer.removeTags(tagsToRemove);
    } finally {
      cacheWriteLock.unlock();
    }
    
    fireOnUnsubscribeEvent(tagsToRemove);
  }

  @Override
  public void unsubscribeDataTags(final Set<Long> dataTagIds, final DataTagUpdateListener listener) {
    Set<Long> tagsToRemove = new HashSet<Long>();
    cacheWriteLock.lock();
    try {
      ClientDataTagImpl cdt = null;
      for (Long tagId : dataTagIds) {
        cdt = controller.getActiveCache().get(tagId);
        if (cdt != null) {
          cdt.removeUpdateListener(listener);
          if (!cdt.hasUpdateListeners()) {
            tagsToRemove.add(tagId);
          }
        }
      }

      // Remove from cache
      cacheSynchronizer.removeTags(tagsToRemove);
    } finally {
      cacheWriteLock.unlock();
    }
    
    fireOnUnsubscribeEvent(tagsToRemove);
  }

  @Override
  public Map<Long, ClientDataTag> get(final Set<Long> tagIds) {
    Map<Long, ClientDataTag> resultMap = new HashMap<Long, ClientDataTag>(tagIds.size());
    cacheReadLock.lock();
    try {
      for (Long tagId : tagIds) {
        resultMap.put(tagId, controller.getActiveCache().get(tagId));
      }
    } finally {
      cacheReadLock.unlock();
    }

    return resultMap;
  }

  @Override
  public boolean isHistoryModeEnabled() {
    return controller.isHistoryModeEnabled();
  }

  @Override
  public void setHistoryMode(final boolean enable) {
    controller.setHistoryMode(enable);
  }

  @Override
  public Object getHistoryModeSyncLock() {
    return controller.getHistoryModeSyncLock();
  }

  @Override
  public boolean containsTag(final Long tagId) {
    return controller.getActiveCache().containsKey(tagId);
  }

  @Override
  public <T extends DataTagUpdateListener> void addDataTagUpdateListener(final Set<Long> tagIds, final T listener) throws CacheSynchronizationException {
    doAddDataTagUpdateListener(tagIds, listener, !(listener instanceof DataTagListener));
  }
  
  private void doAddDataTagUpdateListener(final Set<Long> tagIds, final DataTagUpdateListener listener, final boolean sendInitialValuesToListener) throws CacheSynchronizationException {
    final Set<Long> newTagIds = new HashSet<Long>();
    final Collection<ClientDataTagValue> initialValues = new ArrayList<>(tagIds.size());
    synchronized (getHistoryModeSyncLock()) {
      cacheWriteLock.lock();
      try {
        ClientDataTagImpl cdt = null;
        for (Long tagId : tagIds) {
          if (liveCache.containsKey(tagId)) {
            cdt = controller.getActiveCache().get(tagId);
            try {
              initialValues.add(cdt.clone());
            }
            catch (CloneNotSupportedException e) {
              throw new RuntimeException("ClientDataTagImpl object could not be cloned for tag " + tagId + ": " + cdt.toString());
            }
            cdt.addUpdateListener(listener, sendInitialValuesToListener);
          } else {
            newTagIds.add(tagId);
          }
        }

        if (newTagIds.size() > 0) {
          // Create the uninitialised tags
          cacheSynchronizer.createTags(newTagIds);

          // Add the update listeners and supervision listeners
          for (Long tagId : newTagIds) {
            cdt = controller.getActiveCache().get(tagId);
            if (cdt != null) {
              supervisionManager.addSupervisionListener(cdt, cdt.getProcessIds(), cdt.getEquipmentIds(), cdt.getSubEquipmentIds());
              try {
                initialValues.add(cdt.clone());
              }
              catch (CloneNotSupportedException e) {
                throw new RuntimeException("ClientDataTagImpl object could not be cloned for tag " + tagId + ": " + cdt.toString());
              }
              cdt.addUpdateListener(listener, sendInitialValuesToListener);
            }
          }
          
          // Before subscribing to the update topics we send the initial values,
          // if the listener is of type DataTagListener
          if (!sendInitialValuesToListener && listener instanceof DataTagListener) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("doAddDataTagUpdateListener() - Sending initial values to DataTagListener");
            }
            ((DataTagListener) listener).onInitialValues(initialValues);
          }

          // Asynchronously subscribe to the topics and get the latest values
          // again
          cacheSynchronizer.subscribeTags(newTagIds);
        }

      } 
      finally {
        cacheWriteLock.unlock();
      }
    }

    // Inform listeners (e.g. HistoryManager) about new subscriptions
    fireOnNewTagSubscriptionsEvent(newTagIds);
  }

  @Override
  public int getCacheSize() {
    return liveCache.size();
  }
  
  @Override
  public void addTagSubscriptionListener(final TagSubscriptionListener listener) {
    listenersLock.writeLock().lock();
    try {
      tagSubscriptionListeners.add(listener);
    } finally {
      listenersLock.writeLock().unlock();
    }
  }

  @Override
  public void removeTagSubscriptionListener(final TagSubscriptionListener listener) {
    listenersLock.writeLock().lock();
    try {
      tagSubscriptionListeners.remove(listener);
    } finally {
      listenersLock.writeLock().unlock();
    }
  }
  
  /**
   * Fires an <code>onNewTagSubscriptions()</code> event to all registered <code>TagSubscriptionListener</code>
   * listeners.
   *
   * @param tagIds list of new subscribed tags
   */
  private void fireOnNewTagSubscriptionsEvent(final Set<Long> tagIds) {
    if (!tagIds.isEmpty()) {
      listenersLock.readLock().lock();
      try {
        Set<Long> copyList = new HashSet<Long>(tagIds);
        for (TagSubscriptionListener listener : tagSubscriptionListeners) {
          listener.onNewTagSubscriptions(copyList);
        }
      } finally {
        listenersLock.readLock().unlock();
      }
    }
  }

  /**
   * Fires an <code>onUnsubscribe()</code> event to all registered <code>TagSubscriptionListener</code> listeners.
   *
   * @param tagIds list of tags that have been removed from the cache
   */
  private void fireOnUnsubscribeEvent(final Set<Long> tagIds) {
    listenersLock.readLock().lock();
    try {
      Set<Long> copyList = new HashSet<Long>(tagIds);
      for (TagSubscriptionListener listener : tagSubscriptionListeners) {
        listener.onUnsubscribe(copyList);
      }
    } finally {
      listenersLock.readLock().unlock();
    }
  }
}
