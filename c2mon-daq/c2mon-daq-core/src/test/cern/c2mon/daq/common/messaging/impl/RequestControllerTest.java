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
package cern.c2mon.daq.common.messaging.impl;

import cern.c2mon.daq.common.ICommandRunner;
import cern.c2mon.daq.common.conf.core.ConfigurationController;
import cern.c2mon.daq.common.conf.core.RunOptions;
import cern.c2mon.daq.tools.equipmentexceptions.EqCommandTagException;
import cern.c2mon.shared.common.command.SourceCommandTag;
import cern.c2mon.shared.common.datatag.DataTagAddress;
import cern.c2mon.shared.common.datatag.SourceDataTag;
import cern.c2mon.shared.common.process.EquipmentConfiguration;
import cern.c2mon.shared.common.process.ProcessConfiguration;
import cern.c2mon.shared.daq.command.SourceCommandTagValue;
import cern.c2mon.shared.daq.config.*;
import cern.c2mon.shared.daq.datatag.SourceDataTagValueRequest;
import cern.c2mon.shared.daq.datatag.SourceDataTagValueResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequestControllerTest {
    private ConfigurationController configurationControllerMock;

    @Before
    public void setUp() {
        Class<ConfigurationController> clazz = ConfigurationController.class;
        configurationControllerMock = createMock(clazz, clazz.getMethods());
    }

    @Test
    public void testApplyChange() {
        RequestController requestController = new RequestController(configurationControllerMock);
        List<Change> changes = new ArrayList<Change>();
        /*
         *  Values in this case don't matter.
         *  Just add to the configuration controller and check
         *  if the MessageHandler does the right thing.
         */
        DataTagAdd dataTagAdd = new DataTagAdd();
        changes.add(dataTagAdd);
        configurationControllerMock.onDataTagAdd(dataTagAdd);
        expectLastCall().andReturn(new ChangeReport(1L));

        DataTagRemove dataTagRemove = new DataTagRemove();
        changes.add(dataTagRemove);
        configurationControllerMock.onDataTagRemove(dataTagRemove);
        expectLastCall().andReturn(new ChangeReport(1L));

        DataTagUpdate dataTagUpdate = new DataTagUpdate();
        changes.add(dataTagUpdate);
        configurationControllerMock.onDataTagUpdate(dataTagUpdate);
        expectLastCall().andReturn(new ChangeReport(1L));

        CommandTagAdd commandTagAdd = new CommandTagAdd();
        changes.add(commandTagAdd);
        configurationControllerMock.onCommandTagAdd(commandTagAdd);
        expectLastCall().andReturn(new ChangeReport(1L));

        CommandTagRemove commandTagremove = new CommandTagRemove();
        changes.add(commandTagremove);
        configurationControllerMock.onCommandTagRemove(commandTagremove);
        expectLastCall().andReturn(new ChangeReport(1L));

        CommandTagUpdate commandTagUpdate = new CommandTagUpdate();
        changes.add(commandTagUpdate);
        configurationControllerMock.onCommandTagUpdate(commandTagUpdate);
        expectLastCall().andReturn(new ChangeReport(1L));

        EquipmentConfigurationUpdate equipmentConfigurationUpdate = new EquipmentConfigurationUpdate();
        changes.add(equipmentConfigurationUpdate);
        configurationControllerMock.onEquipmentConfigurationUpdate(equipmentConfigurationUpdate);
        expectLastCall().andReturn(new ChangeReport(1L));

        ProcessConfigurationUpdate processConfigurationUpdate = new ProcessConfigurationUpdate();
        changes.add(processConfigurationUpdate);
        configurationControllerMock.onProcessConfigurationUpdate(processConfigurationUpdate);
        expectLastCall().andReturn(new ChangeReport(1L));

        replay(configurationControllerMock);
        for (Change change : changes) {
            requestController.applyChange(change);
        }
        verify(configurationControllerMock);
    }

    @Test
    public void testExecuteCommand() throws EqCommandTagException {
        ConfigurationController configurationController = getBasicConfigurationController();

        ICommandRunner commandRunner = createMock(ICommandRunner.class);
        RequestController requestController = new RequestController(configurationController);
        requestController.putCommandRunner(1L, commandRunner);

        SourceCommandTagValue sourceCommandTagValue = new SourceCommandTagValue(1L, null, 1L, (short) 0, null, null);
        // MessageHandler should try to call this.
        commandRunner.runCommand(sourceCommandTagValue);
        expectLastCall().andReturn("");

        replay(commandRunner);
        requestController.executeCommand(sourceCommandTagValue);
        verify(commandRunner);
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestProcess() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_PROCESS, 1L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 3);
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestWrongProcessId() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_PROCESS, 2L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 0);
        assertFalse(response.isStatusOK());
        assertTrue(response.getErrorMessage().contains("does not have id: " + 2));
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestEquipment() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_EQUIPMENT, 2L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 2);
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestEquipmentWrongId() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_EQUIPMENT, 20L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 0);
        assertFalse(response.isStatusOK());
        assertTrue(response.getErrorMessage().contains("does not have equipment with id: " + 20L));
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestDataTag() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_DATATAG, 1L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 1);
    }

    @Test
    public void testOnSourceDataTagValueUpdateRequestDataTagWrongId() {
        ConfigurationController configurationController = getBasicConfigurationController();

        RequestController handler = new RequestController(configurationController);
        SourceDataTagValueRequest valueRequest =
            new SourceDataTagValueRequest(SourceDataTagValueRequest.TYPE_DATATAG, 20L);

        SourceDataTagValueResponse response =
            handler.onSourceDataTagValueUpdateRequest(valueRequest);

        assertTrue(response.getAllDataTagValueObjects().size() == 0);
        assertFalse(response.isStatusOK());
        assertTrue(response.getErrorMessage().contains(" does not have a data tag with id: " + 20L));
    }

    /**
     * @return configurationController
     */
    private ConfigurationController getBasicConfigurationController() {
      // We need run options for being use with fir the new PIK (default is we send the PIK)
      RunOptions runOptions = new RunOptions();

      ConfigurationController configurationController = new ConfigurationController();
      ProcessConfiguration processConfiguration = new ProcessConfiguration();
      EquipmentConfiguration equipmentConfiguration = new EquipmentConfiguration();
      EquipmentConfiguration equipmentConfiguration2 = new EquipmentConfiguration();
      SourceCommandTag commandTag = new SourceCommandTag(1L, "hello");
      DataTagAddress address = new DataTagAddress();
      SourceDataTag sourceDataTag = new SourceDataTag(1L, "asd", false, (short)0, "Integer", address);
      SourceDataTag sourceDataTag2 = new SourceDataTag(2L, "asd", false, (short)0, "Integer", address);
      SourceDataTag sourceDataTag3 = new SourceDataTag(3L, "asd", false, (short)0, "Integer", address);
      processConfiguration.setProcessID(1L);
      equipmentConfiguration.setId(1L);
      sourceDataTag.update(25);
      sourceDataTag2.update(25);
      sourceDataTag3.update(25);
      configurationController.setProcessConfiguration(processConfiguration);
      processConfiguration.getEquipmentConfigurations().put(1L, equipmentConfiguration);
      processConfiguration.getEquipmentConfigurations().put(2L, equipmentConfiguration2);
      equipmentConfiguration.getCommandTags().put(1L, commandTag);
      equipmentConfiguration.getDataTags().put(1L, sourceDataTag);
      equipmentConfiguration2.getDataTags().put(2L, sourceDataTag2);
      equipmentConfiguration2.getDataTags().put(3L, sourceDataTag3);
      return configurationController;
    }
}
