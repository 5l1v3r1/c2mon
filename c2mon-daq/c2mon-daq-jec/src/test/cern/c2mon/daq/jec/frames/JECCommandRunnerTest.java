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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.jec.PLCObjectFactory;
import cern.c2mon.daq.tools.equipmentexceptions.EqCommandTagException;
import cern.c2mon.daq.jec.config.PLCConfiguration;
import cern.c2mon.daq.jec.plc.JECPFrames;
import cern.c2mon.daq.jec.plc.StdConstants;
import cern.c2mon.shared.common.ConfigurationException;
import cern.c2mon.shared.common.command.SourceCommandTag;
import cern.c2mon.shared.common.datatag.address.PLCHardwareAddress;
import cern.c2mon.shared.daq.command.SourceCommandTagValue;
import cern.c2mon.shared.common.datatag.address.impl.PLCHardwareAddressImpl;
import cern.c2mon.shared.common.process.EquipmentConfiguration;

public class JECCommandRunnerTest {

    private JECCommandRunner jecCommandRunner;
    private PLCObjectFactory plcFactory;
    
    @Before
    public void setUp() throws ConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        EquipmentLogger equipmentLogger = new EquipmentLogger("asd", "asd", "asd");
        EquipmentConfiguration equipmentConfiguration = new EquipmentConfiguration();
        PLCConfiguration plcConfiguration = new PLCConfiguration();
        plcConfiguration.setProtocol("TestPLCDriver");
        plcFactory = new PLCObjectFactory(plcConfiguration);
        SourceCommandTag sourceCommandTag1 = getCommandTag(PLCHardwareAddress.STRUCT_BOOLEAN_COMMAND, 1L, 0, 1, 0, 1, 2, "asd");
        SourceCommandTag sourceCommandTag2 = getCommandTag(PLCHardwareAddress.STRUCT_ANALOG_COMMAND, 2L, 1, -1, 0, 1, 2, "asd");
        equipmentConfiguration.getCommandTags().put(1L, sourceCommandTag1);
        equipmentConfiguration.getCommandTags().put(2L, sourceCommandTag2);
        jecCommandRunner = new JECCommandRunner(equipmentLogger, plcFactory, equipmentConfiguration);
        
    }
    
    @Test
    public void testSendBooleanCommandACK() throws EqCommandTagException {
        SourceCommandTagValue value = new SourceCommandTagValue(1L, "asd", 1L, (short) 0, true, "Boolean");
        prepareAndSendDelayedCommandAnswer(StdConstants.ACK_MSG);
        jecCommandRunner.runCommand(value);
    }
    
    @Test
    public void testSendBooleanCommandNACK() {
        SourceCommandTagValue value = new SourceCommandTagValue(1L, "asd", 1L, (short) 0, true, "Boolean");
        prepareAndSendDelayedCommandAnswer(StdConstants.NACK_MSG);
        try {
            jecCommandRunner.runCommand(value);
            fail("Expected EqCommandTagException not thrown.");
        }
        catch (EqCommandTagException e) {
            // expected exception thrown
        }
    }
    
    @Test
    public void testSendBooleanCommandUnknownReturnCode() {
        SourceCommandTagValue value = new SourceCommandTagValue(1L, "asd", 1L, (short) 0, true, "Boolean");
        prepareAndSendDelayedCommandAnswer((short) 1337);
        try {
            jecCommandRunner.runCommand(value);
            fail("Expected EqCommandTagException not thrown.");
        }
        catch (EqCommandTagException e) {
            // expected exception thrown
        }
    }
    
    @Test
    public void testSendAnalogCommandACK() throws EqCommandTagException {
        SourceCommandTagValue value = new SourceCommandTagValue(2L, "asd", 1L, (short) 0, 0.23, "Float");
        prepareAndSendDelayedCommandAnswer(StdConstants.ACK_MSG);
        jecCommandRunner.runCommand(value);
    }
    
    @Test
    public void testSendAnalogCommandNACK() {
        SourceCommandTagValue value = new SourceCommandTagValue(2L, "asd", 1L, (short) 0, 0.23, "Float");
        prepareAndSendDelayedCommandAnswer(StdConstants.NACK_MSG);
        try {
            jecCommandRunner.runCommand(value);
            fail("Expected EqCommandTagException not thrown.");
        }
        catch (EqCommandTagException e) {
            // expected exception thrown
        }
    }
    
    @Test
    public void testSendAnalogCommandUnknownReturnCode() {
        SourceCommandTagValue value = new SourceCommandTagValue(2L, "asd", 1L, (short) 0, 0.23, "Float");
        prepareAndSendDelayedCommandAnswer((short) 1337);
        try {
            jecCommandRunner.runCommand(value);
            fail("Expected EqCommandTagException not thrown.");
        }
        catch (EqCommandTagException e) {
            // expected exception thrown
        }
    }
    
    @Test
    public void testUnknownCommandTag() {
        SourceCommandTagValue value = new SourceCommandTagValue(31415926L, "asd", 1L, (short) 0, 0.23, "Float");
        prepareAndSendDelayedCommandAnswer((short) 1337);
        try {
            jecCommandRunner.runCommand(value);
            fail("Expected EqCommandTagException not thrown.");
        }
        catch (EqCommandTagException e) {
            // expected exception thrown
        }
    }
    
    @Test
    public void testIsCorrectMessageId() {
        JECPFrames jecpFrame = plcFactory.getRawRecvFrame();
        jecpFrame.SetMessageIdentifier(StdConstants.CONFIRM_ANA_CMD_MSG);
        assertTrue(jecCommandRunner.isCorrectMessageId(jecpFrame));
        jecpFrame.SetMessageIdentifier(StdConstants.CONFIRM_BOOL_CMD_CTRL_MSG);
        assertTrue(jecCommandRunner.isCorrectMessageId(jecpFrame));
        jecpFrame.SetMessageIdentifier(StdConstants.CONFIRM_BOOL_CMD_MSG);
        assertTrue(jecCommandRunner.isCorrectMessageId(jecpFrame));
    }

    /**
     * @param commandReturnCode
     */
    private void prepareAndSendDelayedCommandAnswer(final short commandReturnCode) {
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JECPFrames frame = plcFactory.getSendFrame(StdConstants.BOOL_CMD_MSG);
                frame.SetJECWord(1, commandReturnCode);
                jecCommandRunner.processJECPFrame(frame);
                
            }
        }).start();
    }

    private SourceCommandTag getCommandTag(int pBlockType, Long id, int pWordId, int pBitId, int pResolutionFactor, float pMinVal, float pMaxVal, String pNativeAddress) throws ConfigurationException {
        SourceCommandTag commandTag = new SourceCommandTag(id, "asd");
        commandTag.setSourceRetries(5);
        commandTag.setSourceTimeout(150);
        PLCHardwareAddressImpl plcHardwareAddress = new PLCHardwareAddressImpl(pBlockType, pWordId, pBitId, pResolutionFactor, pMinVal, pMaxVal, pNativeAddress, 1000);
        commandTag.setHardwareAddress(plcHardwareAddress);
        return commandTag;
    }
}
