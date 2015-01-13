/*******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 *
 * Copyright (C) 2004 - 2014 CERN. This program is free software; you can
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

package cern.c2mon.client.ext.device;

import java.util.List;
import java.util.Set;

import cern.c2mon.client.ext.device.exception.DeviceNotFoundException;

/**
 * This interface describes the methods which are provided by the C2MON device
 * manager singleton. The device manager handles communication with the C2MON
 * server to retrieve information following the class/device/property pattern.
 *
 * @author Justin Lewis Salmon
 */
public interface C2monDeviceManager {

  /**
   * This method allows you to retrieve a list of the names of all device
   * classes. You can then call {@link #getAllDevices(String)} to retrieve a
   * particular device.
   *
   * @return the list of all device class names
   */
  List<String> getAllDeviceClassNames();

  /**
   * Retrieve all devices of a particular class.
   *
   * <p>
   * Note: retrieving devices using this method does not automatically subscribe
   * to those devices. Accessing a property of a particular device will fetch
   * the value from the server only once. To receive updates about property
   * changes, use {@link #subscribeDevices(Set, DeviceUpdateListener)}.
   * </p>
   *
   * @param deviceClassName the name of the class of devices to retrieve
   * @return the list of devices of the specified class
   */
  List<Device> getAllDevices(final String deviceClassName);

  /**
   * Subscribe to retrieve updates of property changes of a device.
   *
   * <p>
   * Subscribing to a device means subscribing to all the properties of that
   * device. When a particular property changes, the given
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method will be
   * called with the device itself and the name of the property that has
   * changed.
   * </p>
   *
   * <p>
   * When a device is first subscribed to, the
   * {@link DeviceUpdateListener#onInitialUpdate(Device)} method is passed a
   * reference to the fully initialised device. Note that the
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method is
   * guaranteed not to be called until this method returns.
   * </p>
   *
   * <p>
   * Note: only a single property will change for any given listener invocation.
   * The listener will be invoked multiple times to reflect multiple property
   * changes.
   * </p>
   *
   * @param devices the device you want to subscribe to
   * @param listener the callback listener that will be notified when the device
   *          is done initialising, and when a device property changes
   *
   * @see DeviceUpdateListener
   */
  void subscribeDevice(final Device device, final DeviceUpdateListener listener);

  /**
   * Subscribe to retrieve updates of property changes of a device.
   *
   * <p>
   * Subscribing to a device means subscribing to all the properties of that
   * device. When a particular property changes, the given
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method will be
   * called with the device itself and the name of the property that has
   * changed.
   * </p>
   *
   * <p>
   * When a device is first subscribed to, the
   * {@link DeviceUpdateListener#onInitialUpdate(Device)} method is passed a
   * reference to the fully initialised device. Note that the
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method is
   * guaranteed not to be called until this method returns.
   * </p>
   *
   * <p>
   * Note: only a single property will change for any given listener invocation.
   * The listener will be invoked multiple times to reflect multiple property
   * changes.
   * </p>
   *
   * @param className the class name of the device you want to subscribe to
   * @param deviceName the name of the device you want to subscribe to
   * @param listener the callback listener that will be notified when a device
   *          property changes
   *
   * @throws DeviceNotFoundException if no device of the given class was found
   */
  void subscribeDevice(final String className, final String deviceName, final DeviceUpdateListener listener) throws DeviceNotFoundException;

  /**
   * Subscribe to retrieve updates of property changes of a set of devices.
   *
   * <p>
   * Subscribing to a device means subscribing to all the properties of that
   * device. When a particular property changes, the given
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method will be
   * called with the device itself and the name of the property that has
   * changed.
   * </p>
   *
   * <p>
   * When a device is first subscribed to, the
   * {@link DeviceUpdateListener#onInitialUpdate(Device)} method is passed a
   * reference to the fully initialised device. Note that the
   * {@link DeviceUpdateListener#onUpdate(Device, PropertyInfo)} method is
   * guaranteed not to be called until this method returns.
   * </p>
   *
   * <p>
   * Note: only a single property will change for any given listener invocation.
   * The listener will be invoked multiple times to reflect multiple property
   * changes.
   * </p>
   *
   * @param devices the set of devices you want to subscribe to
   * @param listener the callback listener that will be notified when a device
   *          property changes
   */
  void subscribeDevices(final Set<Device> devices, final DeviceUpdateListener listener);

  /**
   * Unsubscribe from a previously subscribed-to device.
   *
   * @param devices the device you want to unsubscribe from
   * @param listener the listener that was previously registered
   */
  void unsubscribeDevice(final Device device, final DeviceUpdateListener listener);

  /**
   * Unsubscribe from a previously subscribed-to set of devices.
   *
   * @param devices the set of devices you want to unsubscribe from
   * @param listener the listener that was previously registered
   */
  void unsubscribeDevices(final Set<Device> devices, final DeviceUpdateListener listener);

  /**
   * Unsubscribe from all previously subscribed-to devices.
   *
   * @param listener the listener that was previously registered
   */
  void unsubscribeAllDevices(final DeviceUpdateListener listener);

}
