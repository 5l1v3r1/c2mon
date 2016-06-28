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
package cern.c2mon.daq.common.impl;

import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import cern.c2mon.daq.common.IDynamicTimeDeadbandFilterer;
import cern.c2mon.daq.common.IEquipmentMessageSender;
import cern.c2mon.daq.common.conf.equipment.ICoreDataTagChanger;
import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.daq.common.logger.EquipmentLoggerFactory;
import cern.c2mon.daq.common.messaging.IProcessMessageSender;
import cern.c2mon.daq.filter.IFilterMessageSender;
import cern.c2mon.daq.filter.dynamic.IDynamicTimeDeadbandFilterActivator;
import cern.c2mon.daq.tools.EquipmentSenderHelper;
import cern.c2mon.shared.common.datatag.DataTagAddress;
import cern.c2mon.shared.common.datatag.DataTagConstants;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.datatag.SourceDataQuality;
import cern.c2mon.shared.common.datatag.SourceDataTag;
import cern.c2mon.shared.common.process.EquipmentConfiguration;
import cern.c2mon.shared.common.process.SubEquipmentConfiguration;
import cern.c2mon.shared.daq.config.ChangeReport;
import org.springframework.stereotype.Component;

/**
 * EquipmentMessageSender to control all filtering and sending.
 *
 * @author vilches
 */
@Component
public class EquipmentMessageSender implements ICoreDataTagChanger, IEquipmentMessageSender, IDynamicTimeDeadbandFilterer {

  /**
   * The logger for this class.
   */
  private EquipmentLogger equipmentLogger;

  /**
   * The filter message sender. All tags a filter rule matched are added to
   * this.
   */
  private IFilterMessageSender filterMessageSender;

  /**
   * The process message sender takes the messages actually send to the server.
   */
  private IProcessMessageSender processMessageSender;

  /**
   * The dynamic time band filter activator activates time deadband filtering
   * based on tag occurrence. This one is for medium priorities.
   */
  private IDynamicTimeDeadbandFilterActivator medDynamicTimeDeadbandFilterActivator;

  /**
   * The dynamic time band filter activator activates time deadband filtering
   * based on tag occurrence. This one is for low priorities.
   */
  private IDynamicTimeDeadbandFilterActivator lowDynamicTimeDeadbandFilterActivator;

  /**
   * The equipment configuration of this sender.
   */
  private EquipmentConfiguration equipmentConfiguration;

  /**
   * Valid Sender helper class
   */
  private EquipmentSenderValid equipmentSenderValid;

  /**
   * Invalid Sender helper class
   */
  private EquipmentSenderInvalid equipmentSenderInvalid;

  /**
   * The equipment sender helper with many common and useful methods shared by
   * sending classes
   */
  private EquipmentSenderHelper equipmentSenderHelper = new EquipmentSenderHelper();

  /**
   * The Equipment Alive sender helper class
   */
  private EquipmentAliveSender equipmentAliveSender;

  /**
   * Time deadband helper class
   */
  private EquipmentTimeDeadband equipmentTimeDeadband;

  /**
   * The class with the message sender to send filtered tag values
   */
  private EquipmentSenderFilterModule equipmentSenderFilterModule;

  /**
   * Creates a new EquipmentMessageSender.
   *
   * @param filterMessageSender The filter message sender to send filtered tag
   *          values.
   * @param processMessageSender The process message sender to send tags to the
   *          server.
   * @param medDynamicTimeDeadbandFilterActivator The dynamic time deadband
   *          activator for medium priorities.
   * @param lowDynamicTimeDeadbandFilterActivator The dynamic time deadband
   *          activator for low priorities. checks around the data tag.
   */
  @Autowired
  public EquipmentMessageSender(final IFilterMessageSender filterMessageSender,
                                final IProcessMessageSender processMessageSender,
                                @Qualifier("medDynamicTimeDeadbandFilterActivator") final IDynamicTimeDeadbandFilterActivator medDynamicTimeDeadbandFilterActivator,
                                @Qualifier("lowDynamicTimeDeadbandFilterActivator") final IDynamicTimeDeadbandFilterActivator lowDynamicTimeDeadbandFilterActivator) {
    super();
    this.filterMessageSender = filterMessageSender;
    this.processMessageSender = processMessageSender;
    this.medDynamicTimeDeadbandFilterActivator = medDynamicTimeDeadbandFilterActivator;
    this.lowDynamicTimeDeadbandFilterActivator = lowDynamicTimeDeadbandFilterActivator;
  }

  /**
   * Init
   *
   * @param equipmentConfiguration
   * @param equipmentLoggerFactory
   */
  public void init(final EquipmentConfiguration equipmentConfiguration, final EquipmentLoggerFactory equipmentLoggerFactory) {
    // Configuration
    setEquipmentConfiguration(equipmentConfiguration);
    // Logger
    setEquipmentLoggerFactory(equipmentLoggerFactory);

    // Filter module
    this.equipmentSenderFilterModule = new EquipmentSenderFilterModule(this.filterMessageSender, equipmentLoggerFactory);

    // Time Deadband
    this.equipmentTimeDeadband = new EquipmentTimeDeadband(this, this.processMessageSender, this.equipmentSenderFilterModule, equipmentLoggerFactory);

    // Invalid Sender
    this.equipmentSenderInvalid = new EquipmentSenderInvalid(this.equipmentSenderFilterModule, this.processMessageSender, this.equipmentTimeDeadband, this,
        equipmentLoggerFactory);

    // Valid Sender
    this.equipmentSenderValid = new EquipmentSenderValid(this.equipmentSenderFilterModule, this.processMessageSender, this.equipmentSenderInvalid,
        this.equipmentTimeDeadband, this, equipmentLoggerFactory);

    // Alive Sender
    this.equipmentAliveSender = new EquipmentAliveSender(this.processMessageSender, this.equipmentConfiguration.getAliveTagId(), equipmentLoggerFactory);
    this.equipmentAliveSender.init(this.equipmentConfiguration.getAliveTagInterval(), this.equipmentConfiguration.getName());
  }

  /**
   * Check whether the given tag id corresponds to the alive tag of the
   * equipment, or any sub equipments.
   *
   * @param tagId the id of the tag to check
   * @return true if the tag id corresponds to an alive tag, false otherwise
   */
  boolean isAliveTag(Long tagId) {
    if (equipmentConfiguration.getAliveTagId() == tagId) {
      return true;
    }

    for (SubEquipmentConfiguration subEquipmentConfiguration : equipmentConfiguration.getSubEquipmentConfigurations().values()) {
      if (subEquipmentConfiguration.getAliveTagId() != null && subEquipmentConfiguration.getAliveTagId().equals(tagId)) {
        return true;
      }
    }

    return false;
  }

  /**
   * This method should be invoked each time you want to propagate the
   * supervision alive coming from the supervised equipment.
   */
  @Override
  public void sendSupervisionAlive() {
    Long supAliveTagId = Long.valueOf(this.equipmentConfiguration.getAliveTagId());

    if (supAliveTagId == null) {
      equipmentLogger.debug("sendSupervisionAlive() - No alive tag specified. Ignoring request.");
      return;
    }

    SourceDataTag supAliveTag = null;
    if (this.equipmentConfiguration.isSourceDataTagConfigured(supAliveTagId)) {
      supAliveTag = getTag(supAliveTagId);
    }

    this.equipmentAliveSender.sendEquipmentAlive(supAliveTag);
  }

  /**
   * Tries to send a new value to the server.
   *
   * @param currentTag The tag to which the value belongs.
   * @param milisecTimestamp The timestamp of the tag.
   * @param tagValue The tag value to send.
   * @return True if the tag has been send successfully to the server. False if
   *         the tag has been invalidated or filtered out.
   */
  @Override
  public boolean sendTagFiltered(final ISourceDataTag currentTag, final Object tagValue, final long milisecTimestamp) {
    return sendTagFiltered(currentTag, tagValue, milisecTimestamp, null);
  }

  /**
   * Tries to send a new value to the server.
   *
   * @param currentTag The tag to which the value belongs.
   * @param tagValue The tag value to send.
   * @param milisecTimestamp The timestamp of the tag.
   * @param pValueDescr A description belonging to the value.
   * @return True if the tag has been send successfully to the server. False if
   *         the tag has been invalidated or filtered out.
   */
  @Override
  public boolean sendTagFiltered(final ISourceDataTag currentTag, final Object tagValue, final long milisecTimestamp, String pValueDescr) {
    return sendTagFiltered(currentTag, tagValue, milisecTimestamp, pValueDescr, false);
  }

  /**
   * Tries to send a new value to the server.
   *
   * @param currentTag The tag to which the value belongs.
   * @param sourceTimestamp The source timestamp of the tag in milliseconds.
   * @param tagValue The tag value to send.
   * @param pValueDescr A description belonging to the value.
   * @return True if the tag has been send successfully to the server. False if
   *         the tag has been invalidated or filtered out.
   */
  @Override
  public boolean sendTagFiltered(final ISourceDataTag currentTag, final Object tagValue, final long sourceTimestamp, String pValueDescr,
      boolean sentByValueCheckMonitor) {

    this.equipmentLogger.trace("sendTagFiltered - entering sendTagFiltered()");

    boolean successfulSent = true;
    long tagID = currentTag.getId();
    SourceDataTag tag = getTag(tagID);

    // If we received an update of equipment alive tag, we send immediately a
    // message to the server
    if (isAliveTag(tagID)) {
      successfulSent = this.equipmentAliveSender.sendEquipmentAlive(tag, tagValue, sourceTimestamp, pValueDescr);
    } else {
      successfulSent = this.equipmentSenderValid.sendTagFiltered(tag, tagValue, sourceTimestamp, pValueDescr);
    }

    this.equipmentLogger.trace("sendTagFiltered - leaving sendTagFiltered()");

    return successfulSent;
  }

  /**
   * This method sends an invalid SourceDataTagValue to the server. Source and
   * DAQ timestamps are set to the current DAQ system time.
   *
   * @param sourceDataTag SourceDataTag object
   * @param pQualityCode the SourceDataTag's quality see
   *          {@link SourceDataQuality} class for details
   * @param pDescription the quality description (optional)
   */
  @Override
  public void sendInvalidTag(final ISourceDataTag sourceDataTag, final short pQualityCode, final String pDescription) {
    sendInvalidTag(sourceDataTag, pQualityCode, pDescription, null);
  }

  /**
   * This method sends an invalid SourceDataTagValue to the server, without
   * changing its origin value.
   *
   * @param sourceDataTag SourceDataTag object
   * @param pQualityCode the SourceDataTag's quality see
   *          {@link SourceDataQuality} class for details
   * @param qualityDescription the quality description (optional)
   * @param pTimestamp time when the SourceDataTag's value has become invalid;
   *          if null the source timestamp and DAQ timestamp will be set to the
   *          current DAQ system time
   */
  @Override
  public void sendInvalidTag(final ISourceDataTag sourceDataTag, final short qualityCode, final String qualityDescription, final Timestamp pTimestamp) {

    // Get the source data quality from the quality code
    SourceDataQuality newSDQuality = this.equipmentSenderHelper.createTagQualityObject(qualityCode, qualityDescription);

    // The sendInvalidTag function with the value argument will take are of it
    if (sourceDataTag.getCurrentValue() != null) {
      sendInvalidTag(sourceDataTag, sourceDataTag.getCurrentValue().getValue(), sourceDataTag.getCurrentValue().getValueDescription(), newSDQuality, pTimestamp);
    } else {
      sendInvalidTag(sourceDataTag, null, "", newSDQuality, pTimestamp);
    }
  }

  /**
   * This method sends both an invalid and updated SourceDataTagValue to the
   * server.
   *
   * @param sourceDataTag SourceDataTag object
   * @param newValue The new update value that we want set to the tag
   * @param newTagValueDesc The new value description
   * @param newSDQuality the new SourceDataTag see {@link SourceDataQuality}
   * @param pTimestamp time when the SourceDataTag's value has become invalid;
   *          if null the source timestamp and DAQ timestamp will be set to the
   *          current DAQ system time
   */
  protected void sendInvalidTag(final ISourceDataTag sourceDataTag, final Object newValue, final String newTagValueDesc, final SourceDataQuality newSDQuality,
      final Timestamp pTimestamp) {
    this.equipmentLogger.debug("sendInvalidTag - entering sendInvalidTag() for tag #" + sourceDataTag.getId());

    long tagID = sourceDataTag.getId();
    SourceDataTag tag = getTag(tagID);

    if (newSDQuality == null || newSDQuality.isValid()) {
      // this means we have a valid quality code 0 (OK)
      this.equipmentLogger.warn("sendInvalidTag - method called with 0(OK) quality code for tag " + sourceDataTag.getId()
          + ". This should normally not happen! Redirecting call to sendTagFiltered() method.");
      this.equipmentSenderValid.sendTagFiltered(tag, newValue, pTimestamp.getTime(), newTagValueDesc);
    } else {
      if (this.equipmentLogger.isDebugEnabled()) {
        this.equipmentLogger.debug("sendInvalidTag - Bad Quality confirmed. Invalidating ...");
      }
      this.equipmentSenderInvalid.sendInvalidTag(tag, newValue, newTagValueDesc, newSDQuality, pTimestamp);
    }

    this.equipmentLogger.debug("sendInvalidTag - leaving sendInvalidTag()");
  }

  /**
   * TimeDeadband policy:
   *
   * Static TimeDeadband Dynamic TimeDeadband Filter applied -------------------
   * -------------------- -------------- Yes Yes Static yes No Static No Yes
   * Dynamic No No None
   *
   *
   * Static TimeDeadband has more priority than the Dynamic one. So if the
   * Static TimeDeadband for the current Tag is disable and the DAQ has the
   * Dynamic TimeDeadband enabled then the Tag will be recorded for dynamic time
   * deadband filtering depending on the tag priority (only LOW and MEDIUM are
   * used).
   *
   * @param tag The tag to be recorded.
   */
  @Override
  public void recordTag(final SourceDataTag tag) {
    DataTagAddress address = tag.getAddress();
    if (isDynamicTimeDeadband(tag)) {
      switch (address.getPriority()) {
      case DataTagConstants.PRIORITY_LOW:
        this.lowDynamicTimeDeadbandFilterActivator.newTagValueSent(tag.getId());
        break;
      case DataTagConstants.PRIORITY_MEDIUM:
        this.medDynamicTimeDeadbandFilterActivator.newTagValueSent(tag.getId());
        break;
      default:
        // other priorities are ignored
        break;
      }
    }
  }

  /**
   * Checks if Dynamic Timedeadband can be appliyed or not
   *
   * @param tag The tag to be recorded.
   * @return True if the Dynamic Timedeadband can be apply or false if not
   */
  @Override
  public boolean isDynamicTimeDeadband(final SourceDataTag tag) {
    DataTagAddress address = tag.getAddress();
    return (!address.isStaticTimedeadband() && this.equipmentConfiguration.isDynamicTimeDeadbandEnabled());
  }

  /**
   * Sends a note to the business layer, to confirm that the equipment is not
   * properly configured, or connected to its data source
   */
  @Override
  public final void confirmEquipmentStateIncorrect() {
    confirmEquipmentStateIncorrect(null);
  }

  /**
   * Sends a note to the business layer, to confirm that the equipment is not
   * properly configured, or connected to its data source
   *
   * @param pDescription additional description
   */
  @Override
  public final void confirmEquipmentStateIncorrect(final String pDescription) {
    sendCommfaultTag(this.equipmentConfiguration.getCommFaultTagId(), this.equipmentConfiguration.getCommFaultTagValue(), pDescription);

    // Send the commFaultTag for the equipment's subequipments too
    Map<Long, SubEquipmentConfiguration> subEquipmentConfigurations = equipmentConfiguration.getSubEquipmentConfigurations();

    for (SubEquipmentConfiguration subEquipmentConfiguration : subEquipmentConfigurations.values()) {
      sendCommfaultTag(subEquipmentConfiguration.getCommFaultTagId(), subEquipmentConfiguration.getCommFaultTagValue(), pDescription);
    }
  }

  @Override
  public void confirmSubEquipmentStateIncorrect(Long commFaultTagId) {
    confirmSubEquipmentStateIncorrect(commFaultTagId, null);
  }

  @Override
  public void confirmSubEquipmentStateIncorrect(Long commFaultTagId, String description) {
    for (SubEquipmentConfiguration subEquipmentConfiguration : equipmentConfiguration.getSubEquipmentConfigurations().values()) {
      if (subEquipmentConfiguration.getCommFaultTagId().equals(commFaultTagId)) {
        sendCommfaultTag(commFaultTagId, subEquipmentConfiguration.getCommFaultTagValue(), description);
      }
    }
  }

  /**
   * Sends the CommfaultTag message.
   *
   * @param tagID The CommfaultTag id.
   * @param value The CommFaultTag value to send.
   * @param description The description of the CommfaultTag
   */
  private void sendCommfaultTag(final long tagID, final Boolean value, final String description) {
    if (this.equipmentLogger.isDebugEnabled()) {
      this.equipmentLogger.debug("sendCommfaultTag - entering sendCommfaultTag()..");
      this.equipmentLogger.debug("\tCommFaultTag: #" + tagID);
    }
    if (description == null) {
      this.processMessageSender.sendCommfaultTag(tagID, value);
    } else {
      this.processMessageSender.sendCommfaultTag(tagID, value, description);
    }
    this.equipmentLogger.debug("sendCommfaultTag - leaving sendCommfaultTag()");
  }

  /**
   * Sends a note to the business layer, to confirm that the equipment is
   * properly configured, connected to its source and running
   */
  @Override
  public final void confirmEquipmentStateOK() {
    confirmEquipmentStateOK(null);
  }

  /**
   * Sends a note to the business layer, to confirm that the equipment is
   * properly configured, connected to its source and running
   *
   * @param pDescription additional description
   */
  @Override
  public final void confirmEquipmentStateOK(final String pDescription) {
    sendCommfaultTag(this.equipmentConfiguration.getCommFaultTagId(), !this.equipmentConfiguration.getCommFaultTagValue(), pDescription);

    // Send the commFaultTag for the equipment's subequipments too
    Map<Long, SubEquipmentConfiguration> subEquipmentConfigurations = equipmentConfiguration.getSubEquipmentConfigurations();

    for (SubEquipmentConfiguration subEquipmentConfiguration : subEquipmentConfigurations.values()) {
      sendCommfaultTag(subEquipmentConfiguration.getCommFaultTagId(), !subEquipmentConfiguration.getCommFaultTagValue(), pDescription);
    }
  }

  @Override
  public void confirmSubEquipmentStateOK(Long commFaultTagId) {
    confirmSubEquipmentStateOK(commFaultTagId, null);
  }

  @Override
  public void confirmSubEquipmentStateOK(Long commFaultTagId, String description) {
    for (SubEquipmentConfiguration subEquipmentConfiguration : equipmentConfiguration.getSubEquipmentConfigurations().values()) {
      if (subEquipmentConfiguration.getCommFaultTagId().equals(commFaultTagId)) {
        sendCommfaultTag(commFaultTagId, !subEquipmentConfiguration.getCommFaultTagValue(), description);
      }
    }
  }

  /**
   * Sets the equipment configuration
   *
   * @param equipmentConfiguration The equipment configuration.
   */
  private void setEquipmentConfiguration(final EquipmentConfiguration equipmentConfiguration) {
    this.equipmentConfiguration = equipmentConfiguration;
    Map<Long, SourceDataTag> sourceDataTags = equipmentConfiguration.getDataTags();
    this.medDynamicTimeDeadbandFilterActivator.clearDataTags();
    this.lowDynamicTimeDeadbandFilterActivator.clearDataTags();
    for (Entry<Long, SourceDataTag> entry : sourceDataTags.entrySet()) {
      DataTagAddress address = entry.getValue().getAddress();
      if (!address.isStaticTimedeadband() && equipmentConfiguration.isDynamicTimeDeadbandEnabled()) {
        switch (address.getPriority()) {
        case DataTagConstants.PRIORITY_LOW:
          this.lowDynamicTimeDeadbandFilterActivator.addDataTag(entry.getValue());
          break;
        case DataTagConstants.PRIORITY_MEDIUM:
          this.medDynamicTimeDeadbandFilterActivator.addDataTag(entry.getValue());
          break;
        default:
          // other priorities are ignored
        }
      }
    }
  }

  /**
   * @param equipmentLoggerFactory the equipmentLoggerFactory to set
   */
  private void setEquipmentLoggerFactory(final EquipmentLoggerFactory equipmentLoggerFactory) {
    this.equipmentLogger = equipmentLoggerFactory.getEquipmentLogger(getClass());
  }

  /**
   * Sends all through timedeadband delayed values immediately
   */
  @Override
  public void sendDelayedTimeDeadbandValues() {
    this.equipmentLogger.debug("sendDelayedTimeDeadbandValues - Sending all time deadband delayed values to the server");

    this.equipmentSenderValid.sendDelayedTimeDeadbandValues();
  }

  /**
   * Gets a source data tag with the provided id.
   *
   * @param tagID The id of the tag to get.
   * @return The SourceDataTag with this id.
   */
  private SourceDataTag getTag(final Long tagID) {
    if (tagID == null) {
      throw new InvalidParameterException("Passed null parameter as tag ID");
    }

    SourceDataTag sdt = (SourceDataTag) this.equipmentConfiguration.getSourceDataTag(tagID);

    if (sdt == null) {
      throw new RuntimeException("Could not get the SourceDataTag for tag " + tagID + ". The tag is not registered in the equipment configurations cache. No update is sent!");
    }

    return sdt;
  }

  /**
   *
   * @return equipmentSenderValid
   */
  protected EquipmentSenderValid getEquipmentSenderValid() {
    return this.equipmentSenderValid;
  }

  /**
   * Reconfiguration functions Add/Remove/Update
   */

  /**
   * Adds a data tag to this sender.
   *
   * @param sourceDataTag The data tag to add.
   * @param changeReport The change report to fill with the results of the
   *          change.
   */
  @Override
  public void onAddDataTag(final SourceDataTag sourceDataTag, final ChangeReport changeReport) {
    DataTagAddress address = sourceDataTag.getAddress();
    if (!address.isStaticTimedeadband() && this.equipmentConfiguration.isDynamicTimeDeadbandEnabled()) {
      switch (address.getPriority()) {
      case DataTagConstants.PRIORITY_LOW:
        this.lowDynamicTimeDeadbandFilterActivator.addDataTag(sourceDataTag);
        changeReport.appendInfo("Data tag " + sourceDataTag.getId() + " added to low priority filter.");
        break;
      case DataTagConstants.PRIORITY_MEDIUM:
        this.medDynamicTimeDeadbandFilterActivator.addDataTag(sourceDataTag);
        changeReport.appendInfo("Data tag " + sourceDataTag.getId() + " added to medium priority filter.");
        break;
      default:
        changeReport.appendInfo("Data tag " + sourceDataTag.getId() + " not added to any filter.");
      }
    }
  }

  /**
   * Removes a data tag from this sender.
   *
   * @param sourceDataTag The data tag to remove.
   * @param changeReport The change report to fill with the results of the
   *          change.
   */
  @Override
  public void onRemoveDataTag(final SourceDataTag sourceDataTag, final ChangeReport changeReport) {
    this.medDynamicTimeDeadbandFilterActivator.removeDataTag(sourceDataTag);
    this.lowDynamicTimeDeadbandFilterActivator.removeDataTag(sourceDataTag);
    changeReport.appendInfo("Data tag " + sourceDataTag.getId() + " removed from any filters.");
  }

  /**
   * Updates a data tag of this sender.
   *
   * @param sourceDataTag The data tag to update.
   * @param oldSourceDataTag The old source data tag to identify if necessary
   *          for changes.
   * @param changeReport The change report to fill with the results.
   */
  @Override
  public void onUpdateDataTag(final SourceDataTag sourceDataTag, final SourceDataTag oldSourceDataTag, final ChangeReport changeReport) {
    if (!sourceDataTag.getAddress().isStaticTimedeadband() && sourceDataTag.getAddress().getPriority() != oldSourceDataTag.getAddress().getPriority()) {
      onRemoveDataTag(sourceDataTag, changeReport);
      onAddDataTag(sourceDataTag, changeReport);
    }
  }

}
