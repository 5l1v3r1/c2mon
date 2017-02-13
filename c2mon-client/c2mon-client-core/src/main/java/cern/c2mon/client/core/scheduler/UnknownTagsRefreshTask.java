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
package cern.c2mon.client.core.scheduler;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cern.c2mon.client.common.tag.Tag;
import cern.c2mon.client.core.cache.TagCache;

/**
 * Spring SchedulerTask which is executed every 5 minutes to ask
 * the server whether the unknown tags have been configured in meantime.
 *
 * @author Matthias Braeger
 */
@Component
@EnableScheduling
class UnknownTagsRefreshTask {
  
  /**
   * The cache instance which is managing all <code>Tag</code> objects
   */
  private final TagCache cache;
  
  /** Logger instance */
  private static final Logger LOG = LoggerFactory.getLogger(UnknownTagsRefreshTask.class);
  
  
  /**
   * Default Constructor
   * @param cache The cache instance which is managing all <code>Tag</code> objects
   */
  @Autowired
  public UnknownTagsRefreshTask(final TagCache cache) {
    this.cache = cache;
  }
   
  /**
   * This method is called every 5 minutes in order to refresh all Tags which are
   * currently marked as unknown.
   */
  @Scheduled(fixedRate=300000, initialDelay=300000)
  public void refreshAllUnknownTags() {
    if (LOG.isTraceEnabled()) {
      LOG.trace("refreshAllUnknownTags() - Start refreshing all unknown tags ... ");
    }
    Set<Long> unknownTags = new HashSet<>();
    
    try {
      if (!cache.isHistoryModeEnabled()) {
        for (Tag cdt : cache.getAllSubscribedDataTags()) {
          if (!cdt.getDataTagQuality().isExistingTag()) {
            unknownTags.add(cdt.getId());
          }
        }
        
        if (!unknownTags.isEmpty()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("refreshAllUnknownTags() - trying to refresh " + unknownTags.size() + " tags marked as UNKNOWN ...");
          }
          cache.refresh(unknownTags);
        }
      
      } // end if block
    }
    catch (Exception e) {
      LOG.error("refreshAllUnknownTags() - An error occured while trying to refresh all unknown tags from the server.", e);
    }
    
    if (LOG.isTraceEnabled()) {
      LOG.trace("refreshAllUnknownTags() - Scheduled task finished. Next call in 5 minutes.");
    }
  }
}
