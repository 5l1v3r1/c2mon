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
package cern.c2mon.server.cache.command;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cern.c2mon.server.cache.ClusterCache;
import cern.c2mon.server.cache.CommandTagCache;
import cern.c2mon.server.cache.common.AbstractCache;
import cern.c2mon.server.cache.common.C2monCacheLoader;
import cern.c2mon.server.cache.loading.SimpleCacheLoaderDAO;
import cern.c2mon.server.common.config.C2monCacheName;
import cern.c2mon.shared.common.command.CommandTag;

/**
 * Implementation of the CommandTag cache.
 * 
 * @author Mark Brightwell
 *
 */
@Service
public class CommandTagCacheImpl extends AbstractCache<Long, CommandTag> implements CommandTagCache {

  /**
   * Private class logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CommandTagCacheImpl.class); 
 
  @Autowired
  public CommandTagCacheImpl(final ClusterCache clusterCache, 
                          @Qualifier("commandTagEhcache") final Ehcache ehcache,
                          @Qualifier("commandTagEhcacheLoader") final CacheLoader cacheLoader, 
                          @Qualifier("commandTagCacheLoader") final C2monCacheLoader c2monCacheLoader,
                          @Qualifier("commandTagDAO") final SimpleCacheLoaderDAO<CommandTag> cacheLoaderDAO) {
    super(clusterCache, ehcache, cacheLoader, c2monCacheLoader, cacheLoaderDAO);    
  } 
  
  @PostConstruct
  public void init() {        
    LOGGER.info("Initializing the CommandTag cache...");
    commonInit();        
    LOGGER.info("... CommandTag cache initialization complete.");
  }

  @Override
  protected void doPostDbLoading(CommandTag cacheObject) {
    //do nothing
  }

  @Override
  protected C2monCacheName getCacheName() {
    return C2monCacheName.COMMAND;
  }
  
  @Override
  protected String getCacheInitializedKey() {
    return cacheInitializedKey;
  }


}