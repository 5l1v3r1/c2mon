package cern.c2mon.server.cache.device;

import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.cache.api.factory.AbstractCacheFactory;
import cern.c2mon.server.cache.config.AbstractSimpleCacheConfig;
import cern.c2mon.server.cache.CacheName;
import cern.c2mon.server.cache.loader.CacheLoaderDAO;
import cern.c2mon.server.common.device.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Szymon Halastra
 * @author Alexandros Papageorgiou Koufidis
 */
@Configuration
public class DeviceCacheConfig extends AbstractSimpleCacheConfig<Device> {

  @Autowired
  protected DeviceCacheConfig(AbstractCacheFactory cachingFactory, CacheLoaderDAO<Device> cacheLoaderDAORef) {
    super(cachingFactory, CacheName.DEVICE, Device.class, cacheLoaderDAORef);
  }

  @Bean(name = CacheName.Names.DEVICE)
  @Override
  public C2monCache<Device> createCache() {
    return super.createCache();
  }
}