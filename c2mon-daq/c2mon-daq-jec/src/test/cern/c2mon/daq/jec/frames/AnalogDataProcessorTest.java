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
package cern.c2mon.daq.jec.frames;

import cern.c2mon.daq.common.IEquipmentMessageSender;
import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.jec.JECMessageHandler;
import cern.c2mon.daq.jec.PLCObjectFactory;
import cern.c2mon.daq.jec.address.AnalogJECAddressSpace;
import cern.c2mon.daq.jec.config.PLCConfiguration;
import cern.c2mon.daq.jec.plc.JECIndexOutOfRangeException;
import cern.c2mon.daq.jec.plc.JECPFrames;
import cern.c2mon.daq.jec.tools.JECConversionHelper;
import cern.c2mon.shared.common.ConfigurationException;
import cern.c2mon.shared.common.datatag.DataTagAddress;
import cern.c2mon.shared.common.datatag.DataTagDeadband;
import cern.c2mon.shared.common.datatag.SourceDataQuality;
import cern.c2mon.shared.common.datatag.SourceDataTag;
import cern.c2mon.shared.common.datatag.address.PLCHardwareAddress;
import cern.c2mon.shared.common.datatag.address.impl.PLCHardwareAddressImpl;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnalogDataProcessorTest {

    private AnalogDataProcessor<AnalogJECAddressSpace> analogDataProcessor;
    private PLCObjectFactory plcFactory;
    private IEquipmentMessageSender equipmentMessageSender;
    private SourceDataTag sourceDataTag;
    private SourceDataTag sourceDataTag2;
    private PLCHardwareAddressImpl hardwareAddress;

    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ConfigurationException {
        EquipmentLogger equipmentLogger = new EquipmentLogger("asd", "asd", "asd");
        PLCConfiguration plcConfiguration = new PLCConfiguration();
        plcConfiguration.setProtocol("TestPLCDriver");
        plcFactory = new PLCObjectFactory(plcConfiguration);
        equipmentMessageSender = createStrictMock(IEquipmentMessageSender.class);
        analogDataProcessor = new AnalogDataProcessor<AnalogJECAddressSpace>(1, new AnalogJECAddressSpace(), plcFactory, false, equipmentMessageSender, equipmentLogger);
        sourceDataTag = new SourceDataTag(1L, "asd", false);
        hardwareAddress = new PLCHardwareAddressImpl(PLCHardwareAddress.STRUCT_ANALOG, 0, 5, 0, 100, 1000, "TEST001");
        DataTagAddress dataTagAddress = new DataTagAddress(hardwareAddress);
        sourceDataTag.setAddress(dataTagAddress);
        sourceDataTag.update(10);

        //second tag with value deadband
        sourceDataTag2 = new SourceDataTag(2L, "valueDeadbandTag", false);
        sourceDataTag2.setDataType(Integer.class.getSimpleName());
        PLCHardwareAddress hardwareAddress2 = new PLCHardwareAddressImpl(PLCHardwareAddress.STRUCT_ANALOG, 3, 5, 0, 100, 1000, "TEST001");
        DataTagAddress dataTagAddress2 = new DataTagAddress(hardwareAddress2);
        dataTagAddress2.setValueDeadbandType(DataTagDeadband.DEADBAND_EQUIPMENT_ABSOLUTE);
        dataTagAddress2.setValueDeadband(10);
        sourceDataTag2.setAddress(dataTagAddress2);

        analogDataProcessor.addSourceDataTag(sourceDataTag);
        analogDataProcessor.addSourceDataTag(sourceDataTag2);
        analogDataProcessor.initArrays();
        analogDataProcessor.setInitialValuesSent(0, true);
    }

    @Test
    public void testDetectChanges() throws JECIndexOutOfRangeException {
        JECPFrames jecpFrames = plcFactory.getSendFrame((byte) 1);
        jecpFrames.AddJECData(JECConversionHelper.convertJavaToPLCValue(1.0f, hardwareAddress), 0); // change in bit 5
        jecpFrames.SetDataStartNumber((short) 0);
        jecpFrames.SetDataOffset((short) 1);

        equipmentMessageSender.sendTagFiltered(sourceDataTag, 1f, jecpFrames.GetJECCurrTimeMilliseconds());
        expectLastCall().andReturn(true);

        replay(equipmentMessageSender);
        analogDataProcessor.processJECPFrame(jecpFrames);
        verify(equipmentMessageSender);
    }

    /**
     * Only sent once as unit dead on second occasion.
     */
    @Test
    public void testSendTag() {
        equipmentMessageSender.sendTagFiltered(eq(sourceDataTag), eq(0.0f), geq(System.currentTimeMillis()));
        expectLastCall().andReturn(true);

        replay(equipmentMessageSender);
        analogDataProcessor.sendTag(0, 5);
        sourceDataTag.invalidate(new SourceDataQuality(SourceDataQuality.DATA_UNAVAILABLE, JECMessageHandler.HIERARCHICAL_INVALIDATION_MESSAGE));
        analogDataProcessor.sendTag(0, 5);
        verify(equipmentMessageSender);
    }

    @Test
    public void testRevalidateTag() {
        equipmentMessageSender.sendTagFiltered(eq(sourceDataTag), eq(0.0f), geq(System.currentTimeMillis()));
        expectLastCall().andReturn(true);
        equipmentMessageSender.sendTagFiltered(eq(sourceDataTag), eq(0.0f), geq(System.currentTimeMillis()));
        expectLastCall().andReturn(true);

        replay(equipmentMessageSender);
        analogDataProcessor.revalidateTag(0, 5, System.currentTimeMillis());
        sourceDataTag.getCurrentValue().setQuality(new SourceDataQuality(SourceDataQuality.DATA_UNAVAILABLE, JECMessageHandler.HIERARCHICAL_INVALIDATION_MESSAGE));
        analogDataProcessor.revalidateTag(0, 5, System.currentTimeMillis());
        verify(equipmentMessageSender);
    }

    @Test(expected=NullPointerException.class)
    public void testIsChangeOutOfDeadbandNull() {
      analogDataProcessor.isChangeOutOfDeadband(null, sourceDataTag2);
    }

    @Test
    public void testIsChangeOutOfDeadbandAbsoluteTrue() {
      sourceDataTag2.update(11);
      Integer newValue = 25;
      assertTrue(analogDataProcessor.isChangeOutOfDeadband(newValue, sourceDataTag2));
    }

    @Test
    public void testIsChangeOutOfDeadbandAbsoluteFalse() {
      sourceDataTag2.update(11);
      int newValue = 20;
      assertFalse(analogDataProcessor.isChangeOutOfDeadband(newValue, sourceDataTag2));
    }


}
