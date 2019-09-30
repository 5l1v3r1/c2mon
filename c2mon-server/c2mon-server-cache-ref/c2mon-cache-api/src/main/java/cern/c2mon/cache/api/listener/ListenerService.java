package cern.c2mon.cache.api.listener;

import cern.c2mon.cache.api.listener.impl.BufferedKeyCacheListener;
import cern.c2mon.cache.api.listener.impl.DefaultBufferedCacheListener;
import cern.c2mon.cache.api.listener.impl.MultiThreadedCacheListener;
import cern.c2mon.server.common.component.Lifecycle;
import cern.c2mon.shared.common.Cacheable;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Szymon Halastra
 */
@Slf4j
public class ListenerService<V extends Cacheable> implements Listener<V> {

  /**
   * Reference to the C2monCache event listeners
   */
  private LinkedBlockingDeque<CacheListener<? super V>> cacheListeners = new LinkedBlockingDeque<>();

  private LinkedBlockingDeque<CacheSupervisionListener<? super V>> cacheSupervisionListeners = new LinkedBlockingDeque<>();

  public ListenerService() {
  }


  /**
   * In init method there should be a line which more or less looks like this:
   * <p>
   * registeredEventListeners = cache.getCacheEventNotificationService();
   * <p>
   * Above line is strongly connected with Ehcache, so in our case it should be
   * abstracted and explicit call to cache should be in IMPL module
   */

  @Override
  public void notifyListenersOfUpdate(final V cacheable) {
//    registeredEventListeners.notifyElementUpdated(new Element(cacheable.getId(), null), false); //only for monitoring via Ehcache: not using Ehcache listeners o.w.
    try {
      @SuppressWarnings("unchecked")
      V cloned = (V) cacheable.clone();
      for (CacheListener<? super V> listener : cacheListeners) {
        listener.notifyElementUpdated(cloned);
      }
    }
    catch (CloneNotSupportedException e) {
      log.error("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
      throw new RuntimeException("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
    }
  }

  @Override
  public void notifyListenersOfSupervisionChange(V tag) {
    try {
      @SuppressWarnings("unchecked")
      V cloned = (V) tag.clone();
      for (CacheSupervisionListener<? super V> listener : cacheSupervisionListeners) {
        listener.onSupervisionChange(cloned);
      }
    }
    catch (CloneNotSupportedException e) {
      log.error("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
      throw new RuntimeException("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
    }
  }

  @Override
  public void notifyListenerStatusConfirmation(final V cacheable, final long timestamp) {
    try {
      @SuppressWarnings("unchecked")
      V cloned = (V) cacheable.clone();
      for (CacheListener<? super V> listener : cacheListeners) {
        listener.confirmStatus(cloned);
      }
    }
    catch (CloneNotSupportedException e) {
      log.error("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
      throw new RuntimeException("CloneNotSupportedException caught while cloning a cache element - this should never happen!", e);
    }
  }

  @Override
  public void registerSynchronousListener(CacheListener<? super V> cacheListener) {
    cacheListeners.add(cacheListener);
  }

  @Override
  public Lifecycle registerListener(CacheListener<? super V> cacheListener) {
    MultiThreadedCacheListener<? super V> wrappedCacheListener = new MultiThreadedCacheListener<>(cacheListener, 1000, 0);
    cacheListeners.add(wrappedCacheListener);
    return wrappedCacheListener;
  }

  @Override
  public void registerListenerWithSupervision(CacheSupervisionListener<? super V> cacheSupervisionListener) {
    cacheSupervisionListeners.add(cacheSupervisionListener);
  }

  @Override
  public Lifecycle registerThreadedListener(CacheListener<? super V> cacheListener, int queueCapacity, int threadPoolSize) {
    MultiThreadedCacheListener<? super V> threadedCacheListener = new MultiThreadedCacheListener<>(cacheListener, queueCapacity, threadPoolSize);
    cacheListeners.add(threadedCacheListener);
    return threadedCacheListener;
  }

  @Override
  public Lifecycle registerBufferedListener(final BufferedCacheListener<Cacheable> c2monBufferedCacheListener, int frequency) {
    DefaultBufferedCacheListener<Cacheable> bufferedCacheListener = new DefaultBufferedCacheListener<>(c2monBufferedCacheListener, frequency);
    cacheListeners.add(bufferedCacheListener);
    return bufferedCacheListener;
  }

  @Override
  public Lifecycle registerKeyBufferedListener(final BufferedCacheListener<Long> bufferedCacheListener, int frequency) {
    BufferedKeyCacheListener<V> bufferedKeyCacheListener = new BufferedKeyCacheListener<>(bufferedCacheListener, frequency);
    cacheListeners.add(bufferedKeyCacheListener);
    return bufferedKeyCacheListener;
  }
}
