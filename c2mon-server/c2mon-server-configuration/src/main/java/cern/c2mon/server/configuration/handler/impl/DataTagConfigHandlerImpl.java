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
package cern.c2mon.server.configuration.handler.impl;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

import cern.c2mon.server.cache.DataTagCache;
import cern.c2mon.server.cache.EquipmentFacade;
import cern.c2mon.server.cache.SubEquipmentFacade;
import cern.c2mon.server.cache.exception.CacheElementNotFoundException;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.configuration.handler.DataTagConfigHandler;
import cern.c2mon.server.configuration.handler.transacted.DataTagConfigTransacted;
import cern.c2mon.server.configuration.impl.ConfigurationUpdateImpl;
import cern.c2mon.server.configuration.impl.ProcessChange;
import cern.c2mon.shared.client.configuration.ConfigurationElement;
import cern.c2mon.shared.client.configuration.ConfigurationElementReport;

/**
 * See interface documentation also.
 * <p>
 * <p>Currently all alarms and rules must be manually removed from any tag before it can be removed.
 * This also applied when removing an Equipment or Process: this will only succeed if all alarms
 * and rules have first been removed.
 *
 * @author Mark Brightwell
 */
@Slf4j
@Service
public class DataTagConfigHandlerImpl implements DataTagConfigHandler {

  /**
   * Bean with DB transactions on methods.
   */
  @Autowired
  private DataTagConfigTransacted dataTagConfigTransacted;

  /**
   * Helper class for accessing the List of registered listeners
   * for configuration updates.
   */
  private ConfigurationUpdateImpl configurationUpdateImpl;

  /**
   * Cache for final removal.
   */
  private DataTagCache dataTagCache;

  private EquipmentFacade equipmentFacade;
  private SubEquipmentFacade subEquipmentFacade;

  /**
   * Constructor.
   *
   * @param dataTagCache cache
   * @param equipmentFacade
   * @param subEquipmentFacade
   * @param configurationUpdateImpl
   */
  @Autowired
  public DataTagConfigHandlerImpl(DataTagCache dataTagCache, EquipmentFacade equipmentFacade, SubEquipmentFacade subEquipmentFacade,
                                  ConfigurationUpdateImpl configurationUpdateImpl) {
    this.dataTagCache = dataTagCache;
    this.equipmentFacade = equipmentFacade;
    this.subEquipmentFacade = subEquipmentFacade;
    this.configurationUpdateImpl = configurationUpdateImpl;
  }

  @Override
  public ProcessChange createDataTag(ConfigurationElement element) throws IllegalAccessException {
    ProcessChange change = dataTagConfigTransacted.doCreateDataTag(element);
    dataTagCache.notifyListenersOfUpdate(element.getEntityId());
    log.trace("createDataTag - Notifying Configuration update listeners");
    this.configurationUpdateImpl.notifyListeners(element.getEntityId());
    return change;
  }

  @Override
  public ProcessChange removeDataTag(Long id, ConfigurationElementReport tagReport) {
    log.trace("Removing DataTag " + id);
    try {
      DataTag tagCopy = dataTagCache.getCopy(id);
      ProcessChange change = dataTagConfigTransacted.doRemoveDataTag(id, tagReport);
      dataTagCache.remove(id); //only removed from cache if no exception is thrown

      return change;
    } catch (CacheElementNotFoundException e) {
      tagReport.setWarning(e.getMessage());
      return new ProcessChange(); //no changes for DAQ layer
    }
  }

  @Override
  public ProcessChange updateDataTag(Long id, Properties elementProperties) {
    try {
      ProcessChange processChange = dataTagConfigTransacted.doUpdateDataTag(id, elementProperties);
      log.trace("createDataTag - Notifying Configuration update listeners");
      this.configurationUpdateImpl.notifyListeners(id);
      return processChange;
    } catch (UnexpectedRollbackException e) {
      log.error("Rolling back update in cache");
      dataTagCache.remove(id); //DB transaction is rolled back here: reload the tag
      dataTagCache.loadFromDb(id);
      throw e;
    }
  }

  @Override
  public void addAlarmToTag(Long tagId, Long alarmId) {
    dataTagConfigTransacted.addAlarmToTag(tagId, alarmId);
  }

  @Override
  public void addRuleToTag(Long tagId, Long ruleId) {
    dataTagConfigTransacted.addRuleToTag(tagId, ruleId);
  }

  @Override
  public void removeAlarmFromTag(Long tagId, Long alarmId) {
    dataTagConfigTransacted.removeAlarmFromTag(tagId, alarmId);
  }

  @Override
  public void removeRuleFromTag(Long tagId, Long ruleId) {
    dataTagConfigTransacted.removeRuleFromTag(tagId, ruleId);
  }
}