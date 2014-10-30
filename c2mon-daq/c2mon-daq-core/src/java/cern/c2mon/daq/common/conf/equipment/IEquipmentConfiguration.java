/******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 *
 * Copyright (C) 2005 - 2011 CERN This program is free software; you can
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
 *****************************************************************************/
package cern.c2mon.daq.common.conf.equipment;

import java.util.Map;

import cern.c2mon.daq.common.conf.core.SubEquipmentConfiguration;
import cern.c2mon.shared.daq.command.ISourceCommandTag;
import cern.c2mon.shared.daq.datatag.ISourceDataTag;

/**
 * The equipment configuration interface covers all for the implementation
 * part of the DAQ unnecessary informations.
 *
 * @author Andreas Lang
 *
 */
public interface IEquipmentConfiguration {

    /**
     * This method gets the Equipment identifier
     *
     * @return The id of this equipment.
     */
    long getId();

    /**
     * This method gets the Equipment Unit's name.
     *
     * @return The name of this equipment.
     */
    String getName();

    /**
     * Gets the alive tag interval.
     * @return The alive tag interval.
     */
    long getAliveTagInterval();

    /**
     * Returns the id of the Equipment alive tag
     * (required for instance if the MessageHandler
     * needs to publish the alive and read it back
     * through a subscription mechanism).
     * @return the alive id
     */
    long getAliveTagId();

    /**
     * This method sets the Equipment unit's address
     *
     * @return The address of this equipment. (key value pairs separated with ';')
     */
    String getAddress();

    /**
     * Checks if this equipment configuration contains the source data tag
     * with the provided id.
     * @param tagID The tag id to check.
     * @return True if the source data tag is part of this equipment unit else false.
     */
    boolean hasSourceDataTag(final Long tagID);

    /**
     * Checks if this equipment configuration contains the command tag
     * with the provided id.
     * @param tagID The tag id to check.
     * @return True if the command tag is part of this equipment unit else false.
     */
    boolean hasSourceCommandTag(final Long tagID);

    /**
     * Retrieve all SubEquipment configurations.
     * 
     * @return the map of SubEquipment configuration
     */
    Map<Long, SubEquipmentConfiguration> getSubEquipmentConfigurations();

    /**
     * Retrieve a single SubEquipment configuration.
     * 
     * @param subEquipmentId the id of the SubEquipment configuration
     * @return the SubEquipment configuration
     */
    SubEquipmentConfiguration getSubEquipmentConfiguration(Long subEquipmentId);

    /**
     * Returns a copy of the map of data tags. Adding tags to this
     * map will not affect the rest of the application.
     * @return SourceCommandTag map (dataTagId -> SourceCommandTag)
     */
    Map<Long, ISourceDataTag> getSourceDataTags();

    /**
     * Returns a copy of the map of command tags. Adding tags to this
     * map will not affect the rest of the application.
     * @return SourceCommandTag map (commandTagId -> SourceCommandTag)
     */
    Map<Long, ISourceCommandTag> getSourceCommandTags();

    /**
     * Gets the SourceDataTag with the specified id.
     * @param dataTagId The id of the data tag to look for.
     * @return The searched data tag or null if there is no data tag for the id.
     */
    ISourceDataTag getSourceDataTag(final Long dataTagId);

    /**
     * Gets the SourceCommandTag with the specified id.
     * @param commandTagId The id of the command tag to look for.
     * @return The searched data tag or null if there is no command tag for the id.
     */
    ISourceCommandTag getSourceCommandTag(final Long commandTagId);

}
