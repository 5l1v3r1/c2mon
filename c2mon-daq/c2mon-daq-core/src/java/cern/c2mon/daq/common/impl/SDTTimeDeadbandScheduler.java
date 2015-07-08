/******************************************************************************
 * This file is part of the CERN Control and Monitoring (C2MON) platform.
 * 
 * See http://cern.ch/c2mon
 * 
 * Copyright (C) 2005-2013 CERN.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * Author: C2MON team, c2mon-support@cern.ch
 *****************************************************************************/
package cern.c2mon.daq.common.impl;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import cern.c2mon.daq.common.IDynamicTimeDeadbandFilterer;
import cern.c2mon.daq.common.messaging.IProcessMessageSender;
import cern.c2mon.daq.tools.DataTagValueFilter;
import cern.c2mon.shared.common.datatag.SourceDataQuality;
import cern.c2mon.shared.common.datatag.SourceDataTag;
import cern.c2mon.shared.common.datatag.SourceDataTagValue;
import cern.c2mon.shared.common.filter.FilteredDataTagValue;
import cern.c2mon.shared.common.filter.FilteredDataTagValue.FilterType;
import cern.c2mon.shared.common.type.TypeConverter;

/**
 * The SourceDataTagTimeDeadbandScheduler class models threads responsible for
 * handling source time deadband filtering. It will work with the Static TimeDeadband
 */
public class SDTTimeDeadbandScheduler extends TimerTask {
  /**
   * Log4j Logger for this class
   */
  private static final Logger LOGGER = Logger.getLogger(SDTTimeDeadbandScheduler.class);

  /**
   * The process message sender takes the messages actually send to the server.
   */
  private IProcessMessageSender processMessageSender;

  /**
   * The source data tag controlled by this object.
   */
  private final SourceDataTag sourceDataTag;

  /**
   * True if the current value should be send in the next run cycle.
   */
  private volatile boolean sendValue = false;
  
  /**
   * Last source Data Tag Value sent to the server
   */
  private SourceDataTag lastSourceDataTag;

  /**
   * The class with the message sender to send filtered tag values
   */
  private EquipmentSenderFilterModule equipmentSenderFilterModule;

  /**
   * Filters for Data Tag outgoing Values
   */
  private DataTagValueFilter dataTagValueFilter;

  /**
   * The timer to schedule this task on
   */
  private Timer timeDeadbandTimer;
  
  /**
   * The dynamic time dead band filterer for recording the current source data tag
   */
  private IDynamicTimeDeadbandFilterer dynamicTimeDeadbandFilterer;

  /**
   * Indicates if there is a new value to send to the server.
   * 
   * @return True if in the next cycle a value should be send to the server else
   *         false.
   */
  public boolean isScheduledForSending() {
    return sendValue;
  }

  /**
   * Creates a new SDTTimeDeadbandscheduler
   * 
   * @param sourceDataTag
   *          The source data tag controlled by this object.
   * @param processMessageSender
   *          Takes the messages sent to the server
   * @param equipmentSenderValid
   *          Used to send messages to the server.
   * @param timeDeadbandTimer
   *          The timer to schedule this task on.
   * @param valueChecker
   *          Value checker object to avoid repeated values.
   * @param dynamicTimeDeadbandFilterer 
   *          The dynamic time dead band filterer for recording the current source data tag
   */
  public SDTTimeDeadbandScheduler(final SourceDataTag sourceDataTag, 
                                  final IProcessMessageSender processMessageSender,
                                  final EquipmentSenderFilterModule equipmentSenderFilterModule, 
                                  final Timer timeDeadbandTimer, 
                                  final DataTagValueFilter dataTagValueFilter, 
                                  final IDynamicTimeDeadbandFilterer dynamicTimeDeadbandFilterer) {
    this.dataTagValueFilter = dataTagValueFilter;
    this.processMessageSender = processMessageSender;
    this.equipmentSenderFilterModule = equipmentSenderFilterModule;
    this.dynamicTimeDeadbandFilterer = dynamicTimeDeadbandFilterer;

    this.timeDeadbandTimer = timeDeadbandTimer;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("creating time-deadband scheduler for tag : " + sourceDataTag.getId());
    }

    this.sourceDataTag = sourceDataTag;
  }
  
  /**
   * Starts the timer task with the scheduled fixed rate defined for the given {@link SourceDataTag}
   */
  public void start() {
    // create timers
    if (sourceDataTag.getAddress().isTimeDeadbandEnabled()) {
      if (sourceDataTag.getAddress().getTimeDeadband() > 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : setting scheduling interval to : "
              + this.sourceDataTag.getAddress().getTimeDeadband() + " miliseconds");
        }
      }
      
      this.timeDeadbandTimer.scheduleAtFixedRate(this, 0, this.sourceDataTag.getAddress().getTimeDeadband());
      
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : setting scheduled");
      }
    }
  }
  
  /**
   * flushes and resets the scheduler
   */
  public void flushAndCancel() {
    synchronized (this.sourceDataTag) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : flush and reset");
      }
      
      // Execute the run method to empty the scheduler
      this.cancel();
      this.run();
      this.lastSourceDataTag = null;
    }
  }

  /**
   * Informs the scheduler, that next time it wakes up, it should send the value
   * of the source data tag
   */
  public void scheduleValueForSending() {
    this.sendValue = true;
  }

  /**
   * The run method called from the timer.
   */
  @Override
  public void run() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("scheduler[" + this.sourceDataTag.getId() + "] : entering run()..");
    }
    
    try {
      synchronized (this.sourceDataTag) {
        if (isScheduledForSending()) {
          
          SourceDataTagValue currentSDValue = this.sourceDataTag.getCurrentValue();
          
          FilterType filterType;
          // The first time the lastSentSDTagValue is empty
          if (this.lastSourceDataTag == null) {
            filterType = FilterType.NO_FILTERING;
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : first time running scheduler");
            }
          }
          else {
            // Check the current Source Data tag against the last one sent since
            // they have never been compared 

            // Cast the value to the proper type before sending it 
            if(currentSDValue.getValue() != null) {
              Object newValueCasted = TypeConverter.cast(currentSDValue.getValue(), this.lastSourceDataTag.getDataType());

              filterType = this.dataTagValueFilter.isCandidateForFiltering(this.lastSourceDataTag, newValueCasted,
                  currentSDValue.getValueDescription(), currentSDValue.getQuality(), 
                  currentSDValue.getTimestamp().getTime());
            } else {
              filterType = this.dataTagValueFilter.isCandidateForFiltering(this.lastSourceDataTag, currentSDValue.getValue(),
                  currentSDValue.getValueDescription(), currentSDValue.getQuality(), 
                  currentSDValue.getTimestamp().getTime());
            }
            
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : Filter type: " + filterType);
            }
          }

          // The new value is not filtered out
          if (filterType == FilterType.NO_FILTERING) {
            // Clone the last value sent to the server
            this.lastSourceDataTag = this.sourceDataTag.clone();
            
            currentSDValue.setValueDescription("Time-deadband filtering enabled. " + currentSDValue.getValueDescription());
            // Add the value sent
            this.processMessageSender.addValue(currentSDValue);
            
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : sending value: " + currentSDValue.getValue());
            }
          }
          // The new value is filtered out
          else {
            // Send to filter module (Dynamic or Static information added)
            if(this.dynamicTimeDeadbandFilterer.isDynamicTimeDeadband(this.sourceDataTag)) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : value filtered with Dynamic TimeDeadband : " 
                    + currentSDValue.getValue());
              }
              
              this.equipmentSenderFilterModule.sendToFilterModuleByDynamicTimedeadbandFilterer(this.sourceDataTag, currentSDValue.getValue(), 
                  currentSDValue.getTimestamp().getTime(), currentSDValue.getValueDescription(), filterType.getNumber());
            } else {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : value filtered with Static TimeDeadband: " 
                    + currentSDValue.getValue());
              }
              
              this.equipmentSenderFilterModule.sendToFilterModule(this.sourceDataTag, currentSDValue.getValue(), 
                  currentSDValue.getTimestamp().getTime(), currentSDValue.getValueDescription(), filterType.getNumber());
            }
          }

          // Reset the sendValue variable
          this.sendValue = false;
        }
        else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : no new value to be sent");
          }
        }
      } // synchronized
    }
    catch (Exception exception) {
      LOGGER.error("Critical error in scheduler for tag " + this.sourceDataTag.getId(), exception);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("scheduler[" + this.sourceDataTag.getId() + "] : leaving run()");
    }
  }

  /**
   * If the quality has changed it means the data tag is swapping from valid to
   * invalid or the other way round
   * 
   * @param newSDQuality
   *          The new tag quality
   * @return <code>true</code> if the quality of the last sent Data Tag Value is
   *         not the same as the new one and <code>false</code> in any other
   *         case
   */
  public boolean isNewQualityStatus(final SourceDataQuality newSDQuality) {    
    // if the scheduler has sent a value before we compare against it
    if (this.lastSourceDataTag != null) {
      SourceDataTagValue lastSentSDTagValue = this.lastSourceDataTag.getCurrentValue();
      if ((lastSentSDTagValue.getValue() != null)
          && (lastSentSDTagValue.getQuality().getQualityCode() != newSDQuality.getQualityCode())) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : New Quality status. Last Sent Quality [ " 
              + lastSentSDTagValue.getQuality() + "] vs New Quality [" + newSDQuality + "]");
        }
        
        return true;
      }
    }
    // If there is something scheduled for sending but was not sent yet we compare against the value scheduled
    else if (isScheduledForSending()) {
      SourceDataTagValue scheduledValue = sourceDataTag.getCurrentValue();
 
      if ((scheduledValue != null) 
          && (scheduledValue.getQuality().getQualityCode() != newSDQuality.getQualityCode())) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : New Quality status. Scheduled Quality [" 
              + scheduledValue.getQuality() + "] vs New Quality [" + newSDQuality + "]");
        }
        
        return true;
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\tscheduler[" + this.sourceDataTag.getId() + "] : No new Quality status " );
    }
    
    return false;
  }

}
