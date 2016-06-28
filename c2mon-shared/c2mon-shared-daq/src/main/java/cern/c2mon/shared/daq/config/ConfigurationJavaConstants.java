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
package cern.c2mon.shared.daq.config;

/**
 * Constants for the field names of configuration change objects.
 *
 * @author Andreas Lang
 *
 * @deprecated
 */
public interface ConfigurationJavaConstants {
    /**
     * The id field.
     */
    String ID_FIELD = "id";
    /**
     * The name field.
     */
    String NAME_FIELD = "name";
    /**
     * The control ID field.
     */
    String CONTROL_FIELD = "control";
    /**
     * The mode field.
     */
    String MODE_FIELD = "mode";
    /**
     * The change id field of all changes.
     */
    String CHANGE_ID_FIELD = "changeId";
    /**
     * The data tag ID field.
     */
    String DATA_TAG_ID_FIELD = "dataTagId";
    /**
     * The command tag ID field.
     */
    String COMMAND_TAG_ID_FIELD = "commandTagId";
    /**
     * The equipment unit ID field.
     */
    String EQUIPMENT_UNIT_ID_FIELD = "equipmentUnitId";
    /**
     * The equipment ID field.
     */
    String EQUIPMENT_ID_FIELD = "equipmentId";
    /**
     * The sub equipment ID field.
     */
    String SUB_EQUIPMENT_ID_FIELD = "subEquipmentId";
    /**
     * The process id field
     */
    String PROCESS_ID_FIELD = "processId";
    /**
     * The min value field
     */
    String MIN_VALUE_FIELD = "minValue";
    /**
     * The max value field
     */
    String MAX_VALUE_FIELD = "minValue";
    /**
     * The state field of change report.
     */
    String STATE_FIELD = "state";
}
