/**
 *
 */
package cern.c2mon.client.ext.device;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.client.common.listener.DataTagUpdateListener;
import cern.c2mon.client.common.tag.ClientDataTag;
import cern.c2mon.client.common.tag.ClientDataTagValue;
import cern.c2mon.client.core.C2monCommandManager;
import cern.c2mon.client.core.C2monTagManager;
import cern.c2mon.client.core.cache.BasicCacheHandler;
import cern.c2mon.client.core.tag.ClientCommandTagImpl;
import cern.c2mon.client.core.tag.ClientDataTagImpl;
import cern.c2mon.client.ext.device.cache.DeviceCache;
import cern.c2mon.client.ext.device.property.ClientDeviceProperty;
import cern.c2mon.client.ext.device.property.ClientDevicePropertyImpl;
import cern.c2mon.client.ext.device.property.PropertyInfo;
import cern.c2mon.client.ext.device.request.DeviceRequestHandler;
import cern.c2mon.client.ext.device.util.DeviceTestUtils;
import cern.c2mon.shared.client.device.DeviceClassNameResponse;
import cern.c2mon.shared.client.device.DeviceClassNameResponseImpl;
import cern.c2mon.shared.client.device.DeviceCommand;
import cern.c2mon.shared.client.device.DeviceProperty;
import cern.c2mon.shared.client.device.TransferDevice;
import cern.c2mon.shared.client.device.TransferDeviceImpl;
import cern.c2mon.shared.client.tag.TagUpdate;
import cern.c2mon.shared.rule.RuleFormatException;

/**
 * @author Justin Lewis Salmon
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:cern/c2mon/client/ext/device/config/c2mon-devicemanager-test.xml" })
public class DeviceManagerTest {

  /** Component to test */
  @Autowired
  private DeviceManager deviceManager;

  /** Mocked components */
  @Autowired
  private C2monTagManager tagManagerMock;

  @Autowired
  private C2monCommandManager commandManagerMock;

  @Autowired
  private DeviceCache deviceCacheMock;

  @Autowired
  private BasicCacheHandler dataTagCacheMock;

  @Autowired
  private DeviceRequestHandler requestHandlerMock;

  @Test
  public void testGetAllDeviceClassNames() throws JMSException {
    // Reset the mock
    reset(requestHandlerMock);

    List<DeviceClassNameResponse> deviceClassNamesReturnMap = new ArrayList<DeviceClassNameResponse>();
    deviceClassNamesReturnMap.add(new DeviceClassNameResponseImpl("test_device_class_1"));
    deviceClassNamesReturnMap.add(new DeviceClassNameResponseImpl("test_device_class_2"));

    // Expect the device manager to query the server
    expect(requestHandlerMock.getAllDeviceClassNames()).andReturn(deviceClassNamesReturnMap).once();

    // Setup is finished, need to activate the mock
    replay(requestHandlerMock);

    List<String> deviceClassNames = deviceManager.getAllDeviceClassNames();
    Assert.assertNotNull(deviceClassNames);
    Assert.assertTrue(deviceClassNames.get(0) == deviceClassNamesReturnMap.get(0).getDeviceClassName());
    Assert.assertTrue(deviceClassNames.get(1) == deviceClassNamesReturnMap.get(1).getDeviceClassName());

    // Verify that everything happened as expected
    verify(requestHandlerMock);
  }

  @Test
  public void testGetAllDevices() throws JMSException {
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock, commandManagerMock);
    reset(requestHandlerMock);

    List<TransferDevice> devicesReturnList = new ArrayList<TransferDevice>();
    final TransferDeviceImpl device1 = new TransferDeviceImpl(1000L, "test_device_1", 1L);
    final TransferDeviceImpl device2 = new TransferDeviceImpl(1000L, "test_device_2", 1L);
    device1.addDeviceProperty(new DeviceProperty(1L, "TEST_PROPERTY_1", "100430", "tagId", null));
    device2.addDeviceProperty(new DeviceProperty(2L, "TEST_PROPERTY_2", "100431", "tagId", null));
    device1.addDeviceCommand(new DeviceCommand(1L, "TEST_COMMAND_1", "4287", "commandTagId", null));
    device2.addDeviceCommand(new DeviceCommand(2L, "TEST_COMMAND_2", "4288", "commandTagId", null));
    devicesReturnList.add(device1);
    devicesReturnList.add(device2);

    ClientCommandTagImpl cct1 = new ClientCommandTagImpl<>(-1L);
    ClientCommandTagImpl cct2 = new ClientCommandTagImpl<>(-2L);

    // Expect the device manager to check the cache
    EasyMock.expect(deviceCacheMock.getAllDevices("test_device_class")).andReturn(new ArrayList<Device>());
    // Expect the device manager to retrieve the devices
    expect(requestHandlerMock.getAllDevices(EasyMock.<String> anyObject())).andReturn(devicesReturnList);
    // Expect the device manager to get the command tags
    EasyMock.expect(commandManagerMock.getCommandTag(EasyMock.<Long> anyObject())).andReturn(cct1);
    EasyMock.expect(commandManagerMock.getCommandTag(EasyMock.<Long> anyObject())).andReturn(cct2);
    // Expect the device manager to add the devices to the cache
    deviceCacheMock.add(EasyMock.<Device> anyObject());
    EasyMock.expectLastCall().times(2);

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock, commandManagerMock);
    replay(requestHandlerMock);

    List<Device> devices = deviceManager.getAllDevices("test_device_class");
    Assert.assertNotNull(devices);
    Assert.assertTrue(devices.size() == 2);

    for (Device device : devices) {
      Assert.assertTrue(device.getDeviceClassName().equals("test_device_class"));
    }

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock, commandManagerMock);
    verify(requestHandlerMock);
  }

  @Test
  public void testSubscribeDevice() {

    ClientDataTagImpl cdt1 = new ClientDataTagImpl(100000L);
    ClientDataTagImpl cdt2 = new ClientDataTagImpl(200000L);

    Map<String, ClientDataTagValue> deviceProperties = new HashMap<String, ClientDataTagValue>();
    deviceProperties.put("test_property_name_1", cdt1);
    deviceProperties.put("test_property_name_2", cdt2);

    final DeviceImpl device1 = new DeviceImpl(1L, "test_device", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    device1.setDeviceProperties(deviceProperties);

    final Map<Long, ClientDataTag> cacheReturnMap = getCacheReturnMap();

    // Expect the tag manager to subscribe to the tags
    tagManagerMock.subscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall()
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            // Simulate the tag update calls
            new Thread(new Runnable() {
              @Override
              public void run() {
                for (ClientDataTagValue tag : cacheReturnMap.values()) {
                  device1.onUpdate(tag);
                }
              }
            }).start();
            return true;
          }
        }).once();
    // Expect the device to get the tags from the cache
    // EasyMock.expect(dataTagCacheMock.get(EasyMock.<Set<Long>>
    // anyObject())).andReturn(cacheReturnMap).once();

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    TestDeviceUpdateListener listener = new TestDeviceUpdateListener();
    deviceManager.subscribeDevice(device1, listener);

    // Simulate the tag update calls
    new Thread(new Runnable() {
      @Override
      public void run() {
        for (ClientDataTagValue tag : cacheReturnMap.values()) {
          device1.onUpdate(tag);
        }
      }
    }).start();

    listener.await(5000L);
    Device device = listener.getDevice();
    Assert.assertNotNull(device);
    Assert.assertTrue(device.getId() == device1.getId());

    Map<String, ClientDataTagValue> values = device.getProperties();
    Assert.assertTrue(values.get("test_property_name_1").getId().equals(100000L));
    Assert.assertTrue(values.get("test_property_name_2").getId().equals(200000L));

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock);
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock);
  }

  @Test
  public void testSubscribeLazyDevice() throws RuleFormatException, ClassNotFoundException {

    List<DeviceProperty> sparsePropertyMap = new ArrayList<>();
    sparsePropertyMap.add(new DeviceProperty(1L, "test_property_name_1", "100000", "tagId", null));
    sparsePropertyMap.add(new DeviceProperty(2L, "test_property_name_2", "200000", "tagId", null));

    final DeviceImpl device1 = new DeviceImpl(1L, "test_device", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    device1.setDeviceProperties(sparsePropertyMap);

    final Map<Long, ClientDataTag> cacheReturnMap = getCacheReturnMap();

    // Expect the device to not call getDataTags() but instead to
    // get the tags from the cache
    // EasyMock.expect(dataTagCacheMock.get(EasyMock.<Set<Long>>
    // anyObject())).andReturn(cacheReturnMap).once();
    // Expect the tag manager to subscribe to the tags
    tagManagerMock.subscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
        EasyMock.expectLastCall()
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            // Simulate the tag update calls
            new Thread(new Runnable() {
              @Override
              public void run() {
                for (ClientDataTagValue tag : cacheReturnMap.values()) {
                  device1.onUpdate(tag);
                }
              }
            }).start();
            return true;
          }
        }).once();

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    TestDeviceUpdateListener listener = new TestDeviceUpdateListener();
    deviceManager.subscribeDevice(device1, listener);

    // Update a property
    device1.onUpdate(cacheReturnMap.get(100000L));

    listener.await(5000L);
    Device device = listener.getDevice();
    Assert.assertNotNull(device);
    Assert.assertTrue(device.getId() == device1.getId());

    // Check all properties are subscribed to at this point
    Assert.assertTrue(areAllPropertiesAndFieldsSubscribed(device));

    Map<String, ClientDataTagValue> values = device.getProperties();
    Assert.assertTrue(values.get("test_property_name_1").getId().equals(100000L));
    Assert.assertTrue(values.get("test_property_name_2").getId().equals(200000L));

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock);
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock);
  }

  @Test
  public void testSubscribeDeviceWithFields() throws RuleFormatException {

    Map<String, ClientDeviceProperty> deviceFields = new HashMap<>();

    for (int i = 0; i < 1000; i++) {
      deviceFields.put("test_field_name_" + i, new ClientDevicePropertyImpl(new Long(i)));
    }

    HashMap<String, ClientDeviceProperty> deviceProperties = new HashMap<>();
    deviceProperties.put("test_property_name", new ClientDevicePropertyImpl(deviceFields));

    final DeviceImpl device1 = new DeviceImpl(1L, "test_device", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    device1.setDeviceProperties(deviceProperties);

    final Map<Long, ClientDataTag> cacheReturnMap = new HashMap<Long, ClientDataTag>();

    for (int i = 0; i < 1000; i++) {
      ClientDataTagImpl cdt = new ClientDataTagImpl(new Long(i));
      cdt.update(DeviceTestUtils.createValidTransferTag(new Long(i), "test_tag_name_" + i, "test_value_" + i));
      cacheReturnMap.put(new Long(i), cdt);
    }

    // Expect the tag manager to subscribe to the tags
    tagManagerMock.subscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall()
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            // Simulate the tag update calls
            new Thread(new Runnable() {
              @Override
              public void run() {
                for (ClientDataTagValue tag : cacheReturnMap.values()) {
                  device1.onUpdate(tag);
                }
              }
            }).start();
            return true;
          }
        }).once();

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    TestDeviceUpdateListener listener = new TestDeviceUpdateListener();
    deviceManager.subscribeDevices(new HashSet<Device>(Arrays.asList(device1)), listener);

    // Update a property
    device1.onUpdate(cacheReturnMap.get(0L));

    listener.await(5000L);
    Device device = listener.getDevice();
    Assert.assertNotNull(device);
    Assert.assertTrue(device.getId() == device1.getId());

    Assert.assertTrue(areAllPropertiesAndFieldsSubscribed(device));

    Map<String, ClientDataTagValue> fields = device.getMappedProperty("test_property_name");
    for (int i = 0; i < 1000; i++) {
      ClientDataTagValue tag = fields.get("test_field_name_" + i);
      Assert.assertTrue(tag.getId().equals(new Long(i)));
      Assert.assertTrue(tag.getValue() != null);
      Assert.assertTrue(tag.getValue().equals("test_value_" + i));
    }

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock);
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock);
  }

  @Test
  public void testUnsubscribeDevices() {
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    ClientDataTagImpl cdt1 = new ClientDataTagImpl(100000L);
    ClientDataTagImpl cdt2 = new ClientDataTagImpl(200000L);

    final Map<String, ClientDataTagValue> deviceProperties = new HashMap<String, ClientDataTagValue>();
    deviceProperties.put("test_property_name_1", cdt1);
    deviceProperties.put("test_property_name_2", cdt2);

    final DeviceImpl device1 = new DeviceImpl(1L, "test_device", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    device1.setDeviceProperties(deviceProperties);

    Map<Long, ClientDataTag> cacheReturnMap = getCacheReturnMap();

    // Expect the tag manager to subscribe to the tags
    tagManagerMock.subscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall()
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            // Simulate the tag update calls
            new Thread(new Runnable() {
              @Override
              public void run() {
                for (ClientDataTagValue tag : deviceProperties.values()) {
                  device1.onUpdate(tag);
                }
              }
            }).start();
            return true;
          }
        }).times(2);

    // Expect the device to get the tags from the cache
    // EasyMock.expect(dataTagCacheMock.get(EasyMock.<Set<Long>>
    // anyObject())).andReturn(cacheReturnMap).times(2);
    // Expect the device manager to unsubscribe the tags
    tagManagerMock.unsubscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall().times(2);
    // Expect the device to be removed from the cache
    deviceCacheMock.remove(EasyMock.<Device> anyObject());
    EasyMock.expectLastCall().once();

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    TestDeviceUpdateListener listener1 = new TestDeviceUpdateListener();
    TestDeviceUpdateListener listener2 = new TestDeviceUpdateListener();

    Set<Device> devices = new HashSet<Device>();
    devices.add(device1);

    // Subscribe multiple listeners
    deviceManager.subscribeDevices(devices, listener1);
    deviceManager.subscribeDevices(devices, listener2);

    // Remove a listener
    deviceManager.unsubscribeDevices(devices, listener1);
    Assert.assertFalse(device1.getDeviceUpdateListeners().contains(listener1));
    Assert.assertTrue(device1.getDeviceUpdateListeners().contains(listener2));

    // Remove another listener
    deviceManager.unsubscribeDevices(devices, listener2);
    Assert.assertFalse(device1.getDeviceUpdateListeners().contains(listener2));

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock);
  }

  @Test
  public void testUnsubscribeAllDevices() {
    // Reset the mock
    EasyMock.reset(tagManagerMock, deviceCacheMock, dataTagCacheMock);
    System.out.println("testUnsubscribeAllDevices");

    final ClientDataTagImpl cdt1 = new ClientDataTagImpl(100000L);
    final ClientDataTagImpl cdt2 = new ClientDataTagImpl(200000L);
    Map<String, ClientDataTagValue> deviceProperties1 = new HashMap<String, ClientDataTagValue>();
    Map<String, ClientDataTagValue> deviceProperties2 = new HashMap<String, ClientDataTagValue>();
    deviceProperties1.put("test_property_name_1", cdt1);
    deviceProperties2.put("test_property_name_2", cdt2);

    final DeviceImpl device1 = new DeviceImpl(1L, "test_device_1", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    final DeviceImpl device2 = new DeviceImpl(2L, "test_device_2", 1L, "test_device_class", tagManagerMock, commandManagerMock);
    device1.setDeviceProperties(deviceProperties1);
    device2.setDeviceProperties(deviceProperties2);

    Set<Device> devices = new HashSet<Device>();
    devices.add(device1);
    devices.add(device2);

    Map<Long, ClientDataTag> cacheReturnMap = getCacheReturnMap();

    // Expect the tag manager to subscribe to the tags
    tagManagerMock.subscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall()
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            final Set<Long> tagIds = (Set<Long>) EasyMock.getCurrentArguments()[0];
            // Simulate the tag update calls
            new Thread(new Runnable() {
              @Override
              public void run() {
                if (tagIds.contains(cdt1.getId()))
                  device1.onUpdate(cdt1);
                else if (tagIds.contains(cdt2.getId()))
                  device2.onUpdate(cdt2);
              }
            }).start();
            return true;
          }
        }).times(2);

    // Expect the device manager to get the tags from the cache
    // EasyMock.expect(dataTagCacheMock.get(EasyMock.<Set<Long>>
    // anyObject())).andReturn(cacheReturnMap).times(2);
    // Expect the device manager to get all cached devices
    EasyMock.expect(deviceCacheMock.getAllDevices()).andReturn(new ArrayList<Device>(devices)).once();
    // Expect the device manager to unsubscribe the tags
    tagManagerMock.unsubscribeDataTags(EasyMock.<Set<Long>> anyObject(), EasyMock.<DataTagUpdateListener> anyObject());
    EasyMock.expectLastCall().times(2);
    // Expect the devices to be removed from the cache
    deviceCacheMock.remove(EasyMock.<Device> anyObject());
    EasyMock.expectLastCall().times(2);

    // Setup is finished, need to activate the mock
    EasyMock.replay(tagManagerMock, deviceCacheMock, dataTagCacheMock);

    TestDeviceUpdateListener listener = new TestDeviceUpdateListener();
    deviceManager.subscribeDevices(devices, listener);

    // Update a property
    device1.onUpdate(cacheReturnMap.get(100000L));
    device2.onUpdate(cacheReturnMap.get(200000L));

    deviceManager.unsubscribeAllDevices(listener);

    Assert.assertFalse(device1.getDeviceUpdateListeners().contains(listener));
    Assert.assertFalse(device2.getDeviceUpdateListeners().contains(listener));

    // Verify that everything happened as expected
    EasyMock.verify(tagManagerMock, deviceCacheMock, dataTagCacheMock);
  }

  class TestDeviceUpdateListener implements DeviceUpdateListener {

    final CountDownLatch latch = new CountDownLatch(1);
    Device device;
    PropertyInfo propertyInfo;

    @Override
    public void onUpdate(Device device, PropertyInfo propertyInfo) {
      this.device = device;
      this.propertyInfo = propertyInfo;

      Assert.assertTrue(areAllPropertiesAndFieldsSubscribed(device));

      latch.countDown();
    }

    public void await(Long timeout) {
      try {
        latch.await(timeout, TimeUnit.MILLISECONDS);
        System.out.println("latch released");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public Device getDevice() {
      return device;
    }
  }

  private Map<Long, ClientDataTag> getCacheReturnMap() {
    Map<Long, ClientDataTag> cacheReturnMap = new HashMap<Long, ClientDataTag>();
    ClientDataTagImpl cdt1 = new ClientDataTagImpl(100000L);
    ClientDataTagImpl cdt2 = new ClientDataTagImpl(200000L);

    TagUpdate tu1 = DeviceTestUtils.createValidTransferTag(cdt1.getId(), "test_tag_name_1");
    TagUpdate tu2 = DeviceTestUtils.createValidTransferTag(cdt2.getId(), "test_tag_name_2");

    try {
      cdt1.update(tu1);
      cdt2.update(tu2);
    } catch (RuleFormatException e1) {
      e1.printStackTrace();
    }

    cacheReturnMap.put(cdt1.getId(), cdt1);
    cacheReturnMap.put(cdt2.getId(), cdt2);
    return cacheReturnMap;
  }

  private boolean areAllPropertiesAndFieldsSubscribed(Device device) {
    Map<String, ClientDeviceProperty> properties = ((DeviceImpl) device).getDeviceProperties();
    for (ClientDeviceProperty property : properties.values()) {
      if (property.isDataTag()) {
        if (!property.isSubscribed()) {
          return false;
        }
      }

      if (property.isMappedProperty()) {
        for (ClientDeviceProperty field : property.getFields().values()) {
          if (field.isDataTag()) {
            if (!field.isSubscribed()) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }
}
