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
package cern.c2mon.server.cache.rule;

import java.util.HashSet;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import cern.c2mon.server.cache.ClusterCache;
import cern.c2mon.server.cache.DataTagCache;
import cern.c2mon.server.cache.RuleTagCache;
import cern.c2mon.server.cache.common.C2monCacheLoader;
import cern.c2mon.server.cache.exception.CacheElementNotFoundException;
import cern.c2mon.server.cache.loading.SimpleCacheLoaderDAO;
import cern.c2mon.server.cache.tag.AbstractTagCache;
import cern.c2mon.server.common.config.C2monCacheName;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.common.rule.RuleTag;

/**
 * Implementation of the Rule cache.
 *
 * @author Mark Brightwell
 *
 */
@Service("ruleTagCache")
@ManagedResource(objectName="cern.c2mon:type=cache,name=ruleTagCache")
public class RuleTagCacheImpl extends AbstractTagCache<RuleTag> implements RuleTagCache {

  /**
   * Class logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RuleTagCacheImpl.class);

  /**
   * DataTagCache for rule parent id loading.
   */
  private final DataTagCache dataTagCache;

  @Autowired
  public RuleTagCacheImpl(@Qualifier("clusterCache") final ClusterCache clusterCache,
                          @Qualifier("ruleTagEhcache") final Ehcache ehcache,
                          @Qualifier("ruleTagEhcacheLoader") final CacheLoader cacheLoader,
                          @Qualifier("ruleTagCacheLoader") final C2monCacheLoader c2monCacheLoader,
                          @Qualifier("ruleTagLoaderDAO") final SimpleCacheLoaderDAO<RuleTag> cacheLoaderDAO,
                          @Qualifier("dataTagCache") final DataTagCache dataTagCache) {
    super(clusterCache, ehcache, cacheLoader, c2monCacheLoader, cacheLoaderDAO);
    this.dataTagCache = dataTagCache;
  }

  @PostConstruct
  public void init() {
    LOGGER.info("Initializing RuleTag cache...");
    commonInit();
    LOGGER.info("... RuleTag cache initialization complete.");
  }

  @Override
  protected void doPostDbLoading(RuleTag ruleTag) {
    LOGGER.trace("doPostDbLoading() - Post processing RuleTag " + ruleTag.getId() + " ...");
    setParentSupervisionIds(ruleTag);
    LOGGER.trace("doPostDbLoading() - ... RuleTag " + ruleTag.getId() + " done!");
  }

  @Override
  protected C2monCacheName getCacheName() {
    return C2monCacheName.RULETAG;
  }

  @Override
  protected String getCacheInitializedKey() {
    return cacheInitializedKey;
  }

  /**
   * Sets the parent process and equipment fields for RuleTags.
   * Please notice that the caller method should first make a write lock
   * on the RuleTag reference.
   *
   * @param ruleTag the RuleTag for which the fields should be set
   */
  @Override
  public void setParentSupervisionIds(final RuleTag ruleTag) {
    LOGGER.trace("setParentSupervisionIds() - Setting supervision ids for rule " + ruleTag.getId() + " ...");
    //sets for this ruleTag
    HashSet<Long> processIds = new HashSet<Long>();
    HashSet<Long> equipmentIds = new HashSet<Long>();
    HashSet<Long> subEquipmentIds = new HashSet<Long>();
    int cnt = 0;

    LOGGER.trace(ruleTag.getId() + " Has "+ ruleTag.getRuleInputTagIds().size() + " input rule tags");
    for (Long tagKey : ruleTag.getRuleInputTagIds()) {

      cnt++;
      LOGGER.trace(ruleTag.getId() + " Trying to find rule input tag No#" + cnt + " with id=" + tagKey + " in caches.. ");
      if (dataTagCache.hasKey(tagKey)) {
        DataTag dataTag = dataTagCache.getCopy(tagKey);
        processIds.add(dataTag.getProcessId());
        equipmentIds.add(dataTag.getEquipmentId());
        if (dataTag.getSubEquipmentId() != null) {
          subEquipmentIds.add(dataTag.getSubEquipmentId());
        }
      } else {
          this.acquireWriteLockOnKey(tagKey);
          try {
              RuleTag childRuleTag = (RuleTag) this.get(tagKey);
              //if not empty, already processed; if empty, needs processing
              if (childRuleTag.getProcessIds().isEmpty()) {
                setParentSupervisionIds(childRuleTag);
                this.putQuiet(childRuleTag);
              }
              processIds.addAll(childRuleTag.getProcessIds());
              equipmentIds.addAll(childRuleTag.getEquipmentIds());
              subEquipmentIds.addAll(childRuleTag.getSubEquipmentIds());
          } catch(CacheElementNotFoundException cenfe) {
              throw new RuntimeException("Unable to set rule parent process & equipment ids for rule " + ruleTag.getId()
                      + ": unable to locate tag " + tagKey + " in either RuleTag or DataTag cache (Control tags not supported in rules)", cenfe);
          } finally {
            this.releaseWriteLockOnKey(tagKey);
          }
      }

    }
    LOGGER.debug("setParentSupervisionIds() - Setting parent ids for rule " + ruleTag.getId() + "; process ids: " + processIds + "; equipment ids: " + equipmentIds
        + "; subequipmnet ids: " + subEquipmentIds);
    ruleTag.setProcessIds(processIds);
    ruleTag.setEquipmentIds(equipmentIds);
    ruleTag.setSubEquipmentIds(subEquipmentIds);
  }


}
