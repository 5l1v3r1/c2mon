package cern.c2mon.server.cache.common;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PreDestroy;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.jmx.export.annotation.ManagedOperation;

import cern.c2mon.server.cache.exception.CacheElementNotFoundException;

/**
 * Provides all core functionalities that are required to manage a cache. This
 * class uses internally Ehcache.
 *
 * @author Mark Brightwell, Justin Lewis Salmon
 *
 * @param <K> The key class type
 * @param <T> The value class type
 */
public abstract class BasicCache<K, T extends Serializable> extends ApplicationObjectSupport {

  /**
   * Private class logger.
   */
  private static final Logger LOGGER = Logger.getLogger(BasicCache.class);

  /**
   * Reference to the wrapped Ehcache.
   */
  protected Ehcache cache;

  /**
   * if c2mon.cache.skippreloading is set to true, the cache will not be
   * preloaded from the database, instead ehcache will initialize it from a
   * local cache storage (if available)
   */
  @Value("${c2mon.server.cache.skippreloading}")
  protected boolean skipCachePreloading = false;

  /**
   * The cache mode is either set to "multi", "single" or "single-nonpersistent", depending on whether
   * the server is running with a distributed or single cache.
   */
  @Value("${c2mon.cache.mode}")
  protected String cacheMode;

  /**
   * Length of time (in milliseconds) to wait for a lock to be acquired.
   */
  private int lockTimeout = 1000;

  /**
   * Number of times to attempt to lock a key before reporting that a deadlock
   * has possibly occurred.
   */
  private int lockAttemptThreshold = 60;

  /**
   * An inexpensive check to see if the key exists in the cache.
   *
   * <p>
   * Note: this method will block if the key to be checked is locked by another
   * thread.
   * </p>
   *
   * @param id The key to check for
   * @return <code>true</code> if an Element matching the key is found in the
   *         cache. No assertions are made about the state of the Element.
   * @see Ehcache#isKeyInCache(Object)
   * @throws NullPointerException In case a null pointer is passed as key
   */
  public final boolean hasKey(final K id) {
    if (id == null) {
      throw new NullPointerException("Querying cache with a null key.");
    }

    return cache.isKeyInCache(id);

//    boolean locked = false;
//    int tries = 0;
//
//    while (true) {
//      try {
//        // Check if the key is locked before calling isKeyInCache()
//        locked = cache.tryReadLockOnKey(id, lockTimeout);
//
//        if (locked) {
//          return cache.isKeyInCache(id);
//        }
//
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace("Key " + id + " was locked. Couldn't call isKeyInCache(). Trying again...");
//        }
//
//        // If we tried to acquire the lock more than lockAttemptThreshold times,
//        // we may have a deadlock situation.
//        tries++;
//
//        if (tries > lockAttemptThreshold) {
//          LOGGER.warn("hasKey(): Thread " + Thread.currentThread().getName() + " was waiting for " + (lockTimeout * lockAttemptThreshold) + "ms on key " + id
//              + ". Possible deadlock detected.");
//          tries = 0;
//        }
//
//      } catch (InterruptedException e) {
//        LOGGER.debug("Thread interrupted waiting for read lock on key=" + id);
//
//      } finally {
//        cache.releaseReadLockOnKey(id);
//      }
//    }
  }

  /**
   * Get a Reference to the object of type T in the cache.
   *
   * <p>
   * Throws the following unchecked exceptions:
   * <li> {@link IllegalArgumentException} if called with a null key
   * <li> {@link CacheElementNotFoundException} if the object was not found in
   * the cache
   * <li> {@link RuntimeException} if an error occurs when accessing the cache
   * object in Ehcache.
   * <p>
   * If not sure whether an element is in the cache, first use the hasKey(Long)
   * method.
   *
   * <p>
   * Notice that since this method returns a reference, the object may need
   * locking to stay consistent (if several field are read for instance). For
   * this, the provided read (or write) lock should be used. In the distributed
   * cache mode, the class in which the locking is performed must be
   * instrumented in the Terracotta configuration file for the locking to be
   * effective across server nodes. For this reason, it is generally preferable
   * to use the provided getCopy method, which returns a clone of the cache
   * object.
   *
   * <p>
   * Notice this method does not go to the DB to find a cache element. To
   * explicitly load an element from the DB use the loadFromDb(Long id) method
   * below.
   *
   * @param id the id (key) of the cache element
   * @return a reference to the object stored in the cache
   */
  @SuppressWarnings("unchecked")
  public final T get(final K id) {
    T result = null;
    if (id != null) {

      acquireReadLockOnKey(id);
      try {
        Element element = cache.get(id);
        if (element != null) {
          result = (T) element.getObjectValue();
        } else {
          throw new CacheElementNotFoundException("Failed to locate cache element with id " + id + " (Cache is " + this.getClass() + ")");
        }
      } catch (CacheException cacheException) {
        LOGGER.error("getReference() - Caught cache exception thrown by Ehcache while accessing object with id " + id, cacheException);
        throw new RuntimeException("An error occured when accessing the cache object with id " + id, cacheException);
      } finally {
        releaseReadLockOnKey(id);
      }
    } else {
      LOGGER.error("getReference() - Trying to access cache with a NULL key - throwing an exception!");
      // TODO throw runtime exception here or not?
      throw new IllegalArgumentException("Accessing cache with null key!");
    }

    return result;
  }

  /**
   * Returns the list of all keys in the cache. Only Longs can be inserted as
   * keys in C2monCache.
   *
   * @return list of keys
   */
  @SuppressWarnings("unchecked")
  public List<K> getKeys() {
    return cache.getKeys();
  }

  public void put(K key, T value) {
    cache.put(new Element(key, value));
  }

  /**
   * Remove an object from the cache.
   *
   * @param id the key of the cache element
   * @return true if successful
   */
  @ManagedOperation(description = "Manually remove a given object from the cache (will need re-loading manually from DB)")
  public boolean remove(K id) {
    return cache.remove(id);
  }

  /**
   * @return the cache
   */
  public Ehcache getCache() {
    return cache;
  }

  @PreDestroy
  public void shutdown() {
    LOGGER.debug("Closing cache (" + this.getClass() + ")");
  }

  public void acquireReadLockOnKey(K id) {
    if (id == null) {
      LOGGER.error("Trying to acquire read lock with a NULL key - throwing an exception!");
      throw new IllegalArgumentException("Acquiring read lock with null key!");
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(cache.getName() + " Acquiring READ lock for id=" + String.valueOf(id));
    }

    cache.acquireReadLockOnKey(id);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(cache.getName() + " Got READ lock for id=" + String.valueOf(id));
    }

//    boolean locked = false;
//    int tries = 0;
//
//    while (true) {
//      try {
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace(cache.getName() + " Acquiring READ lock for id=" + String.valueOf(id));
//        }
//
//        locked = cache.tryReadLockOnKey(id, 1000);
//
//        if (locked) {
//          if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace(cache.getName() + " Got READ lock for id=" + String.valueOf(id));
//          }
//
//          return;
//        }
//
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace("Key " + id + " was read locked. Trying again...");
//        }
//
//        // If we tried to acquire the lock more than lockAttemptThreshold times,
//        // we may have a deadlock situation.
//        tries++;
//
//        if (tries > lockAttemptThreshold) {
//          LOGGER.warn("acquireReadLockOnKey: Thread " + Thread.currentThread().getName() + " was waiting for " + (lockTimeout * lockAttemptThreshold)
//              + "ms on key " + id + ". Possible deadlock detected.");
//          tries = 0;
//        }
//
//      } catch (InterruptedException e) {
//        LOGGER.debug("Thread interrupted for id=" + String.valueOf(id) + " (" + this.getClass() + ")");
//      }
//    }
  }

  public void releaseReadLockOnKey(K id) {
    if (id != null) {
      cache.releaseReadLockOnKey(id);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(cache.getName() + " Released READ lock for id=" + String.valueOf(id));
      }

    } else {
      LOGGER.error("Trying to release read lock with a NULL key - throwing an exception!");
      throw new IllegalArgumentException("Trying to release read lock with null key!");
    }
  }

  public void acquireWriteLockOnKey(K id) {
    if (id == null) {
      LOGGER.error("Trying to acquire write lock with a NULL key - throwing an exception!");
      throw new IllegalArgumentException("Acquiring write lock with null key!");
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(cache.getName() + " Acquiring WRITE lock for id=" + String.valueOf(id));
    }

    cache.acquireWriteLockOnKey(id);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(cache.getName() + " Got WRITE lock for id=" + String.valueOf(id));
    }

//    boolean locked = false;
//    int tries = 0;
//
//    while (true) {
//      try {
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace(cache.getName() + " Acquiring WRITE lock for id=" + String.valueOf(id));
//        }
//
//        locked = cache.tryWriteLockOnKey(id, 1000);
//
//        if (locked) {
//          if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace(cache.getName() + " Got WRITE lock for id=" + String.valueOf(id));
//          }
//
//          return;
//        }
//
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace("Key " + id + " was write locked. Trying again...");
//        }
//
//        // If we tried to acquire the lock more than lockAttemptThreshold times,
//        // we may have a deadlock situation.
//        tries++;
//
//        if (tries > lockAttemptThreshold) {
//          LOGGER.warn("acquireWriteLockOnKey(): Thread " + Thread.currentThread().getName() + " was waiting for " + (lockTimeout * lockAttemptThreshold)
//              + "ms on key " + id + ". Possible deadlock detected.");
//          tries = 0;
//        }
//
//      } catch (InterruptedException e) {
//        LOGGER.debug("Thread interrupted for id=" + String.valueOf(id) + " (" + this.getClass() + ")");
//      }
//    }
  }

  public void releaseWriteLockOnKey(K id) {
    if (id != null) {
      cache.releaseWriteLockOnKey(id);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(cache.getName() + " Released WRITE lock for id=" + String.valueOf(id));
      }

    } else {
      LOGGER.error("Trying to release write lock with a NULL key - throwing an exception!");
      throw new IllegalArgumentException("Trying to release write lock with null key!");
    }
  }

  public boolean isWriteLockedByCurrentThread(K id) {
    return cache.isWriteLockedByCurrentThread(id);
  }

  public boolean isReadLockedByCurrentThread(K id) {
    return cache.isReadLockedByCurrentThread(id);
  }

  public boolean tryReadLockOnKey(K id, Long timeout) {
    try {
      return cache.tryReadLockOnKey(id, timeout);
    } catch (InterruptedException e) {
      LOGGER.debug("Thread interrupted for id=" + String.valueOf(id) + " (" + this.getClass() + ")");
      return false;
    }
  }

  public boolean tryWriteLockOnKey(K id, Long timeout) {
    try {
      return cache.tryWriteLockOnKey(id, timeout);
    } catch (InterruptedException e) {
      LOGGER.debug("Thread interrupted for id=" + String.valueOf(id) + " (" + this.getClass() + ")");
      return false;
    }
  }
}
