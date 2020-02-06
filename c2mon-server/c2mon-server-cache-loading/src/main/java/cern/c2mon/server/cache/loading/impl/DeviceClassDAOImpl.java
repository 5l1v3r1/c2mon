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
package cern.c2mon.server.cache.loading.impl;

import cern.c2mon.server.cache.dbaccess.DeviceClassMapper;
import cern.c2mon.server.cache.loading.CacheLoaderName;
import cern.c2mon.server.cache.loading.DeviceClassDAO;
import cern.c2mon.server.cache.loading.common.AbstractDefaultLoaderDAO;
import cern.c2mon.server.common.device.Command;
import cern.c2mon.server.common.device.DeviceClass;
import cern.c2mon.server.common.device.Property;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * DeviceClass loader DAO implementation.
 *
 * @author Justin Lewis Salmon
 */
@Service(CacheLoaderName.Names.DEVICECLASS)
public class DeviceClassDAOImpl extends AbstractDefaultLoaderDAO<DeviceClass> implements DeviceClassDAO {

  /**
   * Reference to the DeviceClass MyBatis loader.
   */
  private DeviceClassMapper deviceClassMapper;

  /**
   * Base constructor, made for injection
   *
   * @param deviceClassMapper the batis mapper
   */
  @Inject
  public DeviceClassDAOImpl(final DeviceClassMapper deviceClassMapper) {
    super(2000, deviceClassMapper);
    this.deviceClassMapper = deviceClassMapper;
  }

  @Override
  public DeviceClass getItem(Object id) {
    return deviceClassMapper.getItem(id);
  }

  @Override
  protected DeviceClass doPostDbLoading(DeviceClass item) {
    return item;
  }

  @Override
  public void updateConfig(DeviceClass deviceClass) {
    deviceClassMapper.updateDeviceClassConfig(deviceClass);
  }

  @Override
  public void insert(DeviceClass deviceClass) {
    deviceClassMapper.insertDeviceClass(deviceClass);

    for (Property property : deviceClass.getProperties()) {
      deviceClassMapper.insertDeviceClassProperty(deviceClass.getId(), property);

      if (property.getFields() != null) {
        for (Property field : property.getFields()) {
          deviceClassMapper.insertDeviceClassField(property.getId(), field);
        }
      }
    }

    for (Command command : deviceClass.getCommands()) {
      deviceClassMapper.insertDeviceClassCommand(deviceClass.getId(), command);
    }
  }

  /**
   * Remove a device class, given an id
   *
   * This function used to be empty (2016-2020).
   * It appears the reason was to force consumers to use
   * another impl that took an entire object as argument,
   * in order to cascade the deletes properly.
   * If this was for some reason desired behavior, remove
   * the mapper call here (and please document why!)
   *
   * Will throw if there are any devices with this class
   * as their device class in the db. You should remove
   * them first.
   *
   * @param id the cache object unique id
   */
  @Override
  public void deleteItem(Long id) {
    DeviceClass deviceClass = deviceClassMapper.getItem(id);

    for (Long propertyId : deviceClass.getPropertyIds()) {
      deviceClassMapper.deleteFields(propertyId);
    }

    deviceClassMapper.deleteProperties(deviceClass.getId());
    deviceClassMapper.deleteCommands(deviceClass.getId());
    deviceClassMapper.deleteDeviceClass(deviceClass.getId());
  }
}
