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
package cern.c2mon.daq.common.logger;

import java.util.Hashtable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import cern.c2mon.shared.common.process.EquipmentConfiguration;
import cern.c2mon.shared.common.process.ProcessConfiguration;

/**
 * This class is used to create EquipmentLoggers.
 *
 * @deprecated this functionality is currently not used and/or needed, to be removed
 */
public class EquipmentLoggerFactory {

  /**
   * The handler class name.
   */
  private String handlerClassName;
  /**
   * File appender used for this handler
   */
  private RollingFileAppender rollingFileAppender;
  /**
   * True if equipment appenders should be used.
   */
  private boolean eqAppendersOnly;
  /**
   * The equipment name and id.
   */
  private String eqNameAndId;
  /**
   * The registered loggers shared static over all EquipmentLoggers
   */
  private static final Hashtable<String, EquipmentLogger> REGISTERED_EQUIPMENT_LOGGERS = new Hashtable<String, EquipmentLogger>();

  /**
   * Creates a new EquipmentLogger.
   *
   * @param econf      The configuration of the equipment.
   * @param pconf      The configuration of the process.
   *
   * @return The new EquipmentLogger.
   */
  public static EquipmentLoggerFactory createFactory(final EquipmentConfiguration econf, final ProcessConfiguration pconf, boolean useEquipmentLoggers,
                                                     boolean useEquipmentAppendersOnly) {
    return new EquipmentLoggerFactory(econf.getHandlerClassName(), Long.valueOf(econf.getId()), econf.getName(), pconf.getProcessName(), useEquipmentLoggers,
        useEquipmentAppendersOnly);
  }

  /**
   * Returns a new equipment logger.
   *
   * @return The new equipment logger.
   */
  public EquipmentLogger getEquipmentLogger() {
    return this.getEquipmentLogger("");
  }

  /**
   * Returns a new equipment logger.
   *
   * @param clazz Class to add more information to the log.
   *
   * @return The new equipment logger.
   */
  public EquipmentLogger getEquipmentLogger(final Class<?> clazz) {
    String className = clazz.getName();
    return this.getEquipmentLogger(className);
  }

  /**
   * Returns a new equipment logger.
   *
   * @param loggerName Logger name to add more information to the log.
   *
   * @return The new equipment logger.
   */
  public synchronized EquipmentLogger getEquipmentLogger(final String loggerName) {
    String key = eqNameAndId + "#" + loggerName + "#" + handlerClassName;
    EquipmentLogger logger;
    if (!REGISTERED_EQUIPMENT_LOGGERS.containsKey(key)) {
      logger = new EquipmentLogger(eqNameAndId, loggerName, handlerClassName.replaceAll(".*\\.", ""));
      REGISTERED_EQUIPMENT_LOGGERS.put(key, logger);
      logger.addAppender(rollingFileAppender);
      if (eqAppendersOnly) {
        logger.setAdditivity(false);
      }
    } else {
      logger = REGISTERED_EQUIPMENT_LOGGERS.get(key);
    }
    return logger;
  }

  /**
   * Creates a new EquipmentLoggerFactory.
   *
   * @param handlerClassName The name of the handler class.
   * @param equipmentId      The id of the equipment.
   * @param equipmentName    The name of the equipment.
   * @param processName      The name of the process.
   * @param eqLoggers        If true creates a logging file.
   * @param eqAppendersOnly  If true the log messages will not only appended to the
   *                         appender of this class. They will also be forwarded to
   *                         possible other log4j appenders.
   */
  public EquipmentLoggerFactory(final String handlerClassName, final Long equipmentId, final String equipmentName, final String processName, final boolean
      eqLoggers, final boolean eqAppendersOnly) {
    this.handlerClassName = handlerClassName;
    this.eqAppendersOnly = eqAppendersOnly;
    this.eqNameAndId = equipmentName + "_" + equipmentId;

    if (eqLoggers) {
      String processLoggerFileLocation = System.getProperty("tim.log.path");
      // the file name should have format :
      // PRCOCESS-NAME_EQUIPMENT-NAME_EQUIPMENT_ID.log
      String fileName = processLoggerFileLocation + "/" + processName + "_" + eqNameAndId + ".log";
      try {
        Appender processLoggerAppender = (Appender) Logger.getRootLogger().getAllAppenders().nextElement();

        Layout processLoggerLayout = processLoggerAppender.getLayout();
        PatternLayout p2 = new PatternLayout(((PatternLayout) processLoggerLayout).getConversionPattern());
        rollingFileAppender = new RollingFileAppender(p2, fileName);

        if (processLoggerAppender instanceof RollingFileAppender) {
          rollingFileAppender.setMaximumFileSize(((RollingFileAppender) processLoggerAppender).getMaximumFileSize());
        } else {
          rollingFileAppender.setMaxFileSize("10MB");
        }
        rollingFileAppender.setMaxBackupIndex(1);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Sets the logging level for an equipment.
   *
   * @param eqName The equipment name.
   * @param level  The log level.
   */
  public static synchronized void setLevel(final String eqName, final Level level) {
    for (EquipmentLogger equipmentLogger : REGISTERED_EQUIPMENT_LOGGERS.values()) {
      if (equipmentLogger.getEquipmentNameAndId().contains(eqName)) equipmentLogger.setLevel(level);
    }
  }

}
