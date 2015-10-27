/******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 *
 * Copyright (C) 2005-2011 CERN.
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
 * Author: TIM team, tim.support@cern.ch
 *****************************************************************************/
package cern.c2mon.server.configuration.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.transform.RegistryMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import cern.c2mon.server.cache.ClusterCache;
import cern.c2mon.server.cache.ProcessCache;
import cern.c2mon.server.cache.ProcessFacade;
import cern.c2mon.server.configuration.ConfigProgressMonitor;
import cern.c2mon.server.configuration.ConfigurationLoader;
import cern.c2mon.server.configuration.dao.ConfigurationDAO;
import cern.c2mon.server.configuration.handler.AlarmConfigHandler;
import cern.c2mon.server.configuration.handler.ControlTagConfigHandler;
import cern.c2mon.server.configuration.handler.DataTagConfigHandler;
import cern.c2mon.server.configuration.handler.DeviceClassConfigHandler;
import cern.c2mon.server.configuration.handler.DeviceConfigHandler;
import cern.c2mon.server.configuration.handler.EquipmentConfigHandler;
import cern.c2mon.server.configuration.handler.ProcessConfigHandler;
import cern.c2mon.server.configuration.handler.RuleTagConfigHandler;
import cern.c2mon.server.configuration.handler.SubEquipmentConfigHandler;
import cern.c2mon.server.configuration.handler.impl.CommandTagConfigHandler;
import cern.c2mon.server.daqcommunication.in.JmsContainerManager;
import cern.c2mon.server.daqcommunication.out.ProcessCommunicationManager;
import cern.c2mon.shared.client.configuration.ConfigConstants.Action;
import cern.c2mon.shared.client.configuration.ConfigConstants.Entity;
import cern.c2mon.shared.client.configuration.ConfigConstants.Status;
import cern.c2mon.shared.client.configuration.ConfigurationElement;
import cern.c2mon.shared.client.configuration.ConfigurationElementReport;
import cern.c2mon.shared.client.configuration.ConfigurationException;
import cern.c2mon.shared.client.configuration.ConfigurationReport;
import cern.c2mon.shared.client.configuration.ConfigurationReportFileFilter;
import cern.c2mon.shared.client.configuration.ConfigurationReportHeader;
import cern.c2mon.shared.client.configuration.configuration.Configuration;
import cern.c2mon.shared.client.configuration.configuration.process.Process;
import cern.c2mon.shared.client.configuration.configuration.tag.DataTag;
import cern.c2mon.shared.client.configuration.converter.DateFormatConverter;
import cern.c2mon.shared.daq.config.Change;
import cern.c2mon.shared.daq.config.ChangeReport;
import cern.c2mon.shared.daq.config.ConfigurationChangeEventReport;
/**
 * Implementation of the server ConfigurationLoader bean.
 *
 * <p>This implementation uses the injected DAO for all database access,
 * so alternative DAO implementation can be wired in if required. The
 * default provided DAO uses iBatis in the background.
 *
 * <p>Notice that creating a cache object will also notify any update
 * listeners. In particular, new datatags, rules and control tags will
 * be passed on to the client, STL module etc.
 *
 * <p>Creations of processes and equipments require a DAQ restart.
 *
 * @author Mark Brightwell
 *
 */
public class ConfigurationLoaderImpl implements ConfigurationLoader {

  //TODO element & element report status always both need updating - redesign this part

  /**
   * Class logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoaderImpl.class);

  /**
   * Avoids interfering with running cache persistence jobs.
   * To avoid a direct dependency to the c2mon-server-cachepersistence module
   * we decided to create a local constant, but the same String is used by the
   * <code>cern.c2mon.server.cachepersistence.common.BatchPersistenceManager</code>
   * to lock on the ClusterCache.
   */
  private final String cachePersistenceLock = "c2mon.cachepersistence.cachePersistenceLock";

  int changeId = 0; //unique id for all generated changes (including those recursive ones during removal)

  private ProcessCommunicationManager processCommunicationManager;

  private ConfigurationDAO configurationDAO;

  private DataTagConfigHandler dataTagConfigHandler;

  private ControlTagConfigHandler controlTagConfigHandler;

  private CommandTagConfigHandler commandTagConfigHandler;

  private AlarmConfigHandler alarmConfigHandler;

  private RuleTagConfigHandler ruleTagConfigHandler;

  private EquipmentConfigHandler equipmentConfigHandler;

  private SubEquipmentConfigHandler subEquipmentConfigHandler;

  private ProcessConfigHandler processConfigHandler;

  private ProcessFacade processFacade;

  private ProcessCache processCache;

  private DeviceClassConfigHandler deviceClassConfigHandler;

  private DeviceConfigHandler deviceConfigHandler;

  /**
   * Flag recording if configuration events should be sent to the DAQ layer (set in XML).
   */
  private boolean daqConfigEnabled;

  /**
   * The directory name in C2MON home where the configreports will be saved.
   */
  private String reportDirectory;

  /**
   * Flag indicating if a cancel request has been made.
   */
  private volatile boolean cancelRequested = false;

  private ClusterCache clusterCache;

  @Autowired
  public ConfigurationLoaderImpl(ProcessCommunicationManager processCommunicationManager,
      ConfigurationDAO configurationDAO, DataTagConfigHandler dataTagConfigHandler,
      ControlTagConfigHandler controlTagConfigHandler, CommandTagConfigHandler commandTagConfigHandler,
      final AlarmConfigHandler alarmConfigHandler, RuleTagConfigHandler ruleTagConfigHandler,
      EquipmentConfigHandler equipmentConfigHandler, SubEquipmentConfigHandler subEquipmentConfigHandler,
      ProcessConfigHandler processConfigHandler, ProcessFacade processFacade, ClusterCache clusterCache,
      ProcessCache processCache, DeviceClassConfigHandler deviceClassConfigHandler,
      DeviceConfigHandler deviceConfigHandler) {
    super();
    this.processCommunicationManager = processCommunicationManager;
    this.configurationDAO = configurationDAO;
    this.dataTagConfigHandler = dataTagConfigHandler;
    this.controlTagConfigHandler = controlTagConfigHandler;
    this.commandTagConfigHandler = commandTagConfigHandler;
    this.alarmConfigHandler = alarmConfigHandler;
    this.ruleTagConfigHandler = ruleTagConfigHandler;
    this.equipmentConfigHandler = equipmentConfigHandler;
    this.subEquipmentConfigHandler = subEquipmentConfigHandler;
    this.processConfigHandler = processConfigHandler;
    this.processFacade = processFacade;
    this.processCache = processCache;
    this.clusterCache = clusterCache;
    this.deviceClassConfigHandler = deviceClassConfigHandler;
    this.deviceConfigHandler = deviceConfigHandler;
  }

  @Override
  public ConfigurationReport applyConfiguration(Configuration configuration) {
    LOGGER.info(String.format("Applying configuration for %d process(es)", configuration.getProcesses().size()));
    ConfigurationReport report = null;

    // Try to acquire the configuration lock.
    if (clusterCache.tryWriteLockOnKey(JmsContainerManager.CONFIG_LOCK_KEY, 1L)) {
      try {
        List<ConfigurationElement> configurationElements = parseConfigurationElements(configuration);

        report = applyConfiguration(-1, configuration.getName(), configurationElements, null);

      } catch (Exception ex) {
        LOGGER.error("Exception caught while applying configuration " + configuration.getName(), ex);
          report = new ConfigurationReport(-1, configuration.getName(), "", Status.FAILURE,
              "Exception caught when applying configuration with name <" + configuration.getName() + ">.");
          report.setExceptionTrace(ex);
        throw new ConfigurationException(report, ex);
      } finally {
        clusterCache.releaseWriteLockOnKey(JmsContainerManager.CONFIG_LOCK_KEY);
        if (report != null) {
          archiveReport(configuration.getName(), report.toXML());
        }
      }
    }

    else {
      // If we couldn't acquire the configuration lock, reject the request.
      LOGGER.warn("Unable to apply configuration - another configuration is already running.");
      return new ConfigurationReport(-1, configuration.getName(), configuration.getUser(), Status.FAILURE,
          "Your configuration request has been rejected since another configuration is still running. Please try again later.");
    }

    return report;
  }

  /**
   * Takes a {@link Configuration} object and converts it to a list of {@link ConfigurationElement} objects.
   *
   * @param configuration
   * @return
   */
  private List<ConfigurationElement> parseConfigurationElements(Configuration configuration) {
    List<ConfigurationElement> elements = new ArrayList<>();

    for (Process process : configuration.getProcesses()) {
      for (cern.c2mon.shared.client.configuration.configuration.equipment.Equipment equipment : process.getEquipments()) {
        for (DataTag tag : equipment.getDataTags()) {
          ConfigurationElement element = new ConfigurationElement();
          if (tag.getDelete()) {
            element.setAction(Action.REMOVE);
          } else {
            element.setAction(Action.CREATE);
          }
          element.setEntity(Entity.DATATAG);
          element.setEntityId(tag.getId());
          element.setSequenceId(-1L);

          Properties properties = new Properties();
          properties.put("name", tag.getName());
          properties.put("description", tag.getDescription());
          properties.put("dataType", tag.getType());
          properties.put("equipmentId", String.valueOf(tag.getEquipmentId()));
          properties.put("address", tag.getAddress().toConfigXML());

          element.setElementProperties(properties);
          elements.add(element);
        }
      }
    }

    return elements;
  }

  @Override
  public ConfigurationReport applyConfiguration(final int configId, final ConfigProgressMonitor configProgressMonitor) {
    LOGGER.info(configId + " Applying configuration ");
    ConfigurationReport report = null;

    // Try to acquire the configuration lock.
    if (clusterCache.tryWriteLockOnKey(JmsContainerManager.CONFIG_LOCK_KEY, 1L)) {
      try {

        String configName = configurationDAO.getConfigName(configId);
        if (configName == null) {
          LOGGER.warn(configId + " Unable to locate configuration - cannot be applied.");
          return new ConfigurationReport(
              configId,
              "UNKNOWN",
              "", //TODO set user name through RBAC once available
              Status.FAILURE,
              "Configuration with id <" + configId + "> not found. Please try again with a valid configuration id"
            );
        }

        List<ConfigurationElement> configElements;
        try {
          LOGGER.debug(configId + " Fetching configuration items from DB...");
          configElements = configurationDAO.getConfigElements(configId);
          LOGGER.debug(configId + " Got " + configElements.size() + " elements from DB");
        } catch (Exception e) {
          String message = "Exception caught while loading the configuration for " + configId + " from the DB: " + e.getMessage();
          LOGGER.error(message, e);
          throw new RuntimeException(message, e);
        }

        report = applyConfiguration(configId, configName, configElements, configProgressMonitor);

      } catch (Exception ex) {
        LOGGER.error("Exception caught while applying configuration " + configId, ex);
          report = new ConfigurationReport(configId, "UNKNOWN", "", Status.FAILURE,
              "Exception caught when applying configuration with id <" + configId + ">.");
          report.setExceptionTrace(ex);
        throw new ConfigurationException(report, ex);
      } finally {
        clusterCache.releaseWriteLockOnKey(JmsContainerManager.CONFIG_LOCK_KEY);
        if (report != null) {
          archiveReport(String.valueOf(configId), report.toXML());
        }
      }
    }

    // If we couldn't acquire the configuration lock, reject the request.
    else {
      LOGGER.warn(configId + " Unable to apply configuration - another configuration is already running.");
      return new ConfigurationReport(configId, null, null, Status.FAILURE,
          "Your configuration request has been rejected since another configuration is still running. Please try again later.");
    }

    return report;
  }

  /**
   * Private method to apply a list of ConfigurationElements.
   *
   * Note: configuration lock must be acquired before entering.
   *
   * @param id
   * @param elements
   * @param monitor
   * @return
   */
  private ConfigurationReport applyConfiguration(final int configId, final String configName, final List<ConfigurationElement> configElements, final ConfigProgressMonitor configProgressMonitor) {
    ConfigurationReport report = new ConfigurationReport(configId, configName, "");

    //map of element reports that need a DAQ child report adding
    Map<Long, ConfigurationElementReport> daqReportPlaceholder = new HashMap<Long, ConfigurationElementReport>();
    //map of elements themselves elt_seq_id -> element
    Map<Long, ConfigurationElement> elementPlaceholder = new HashMap<Long, ConfigurationElement>();
    //map of lists, where each list needs sending to a particular DAQ (processId -> List of events)
    Map<Long, List<Change>> processLists = new HashMap<Long, List<Change>>();

    AtomicInteger progressCounter = new AtomicInteger(1);
    if (configProgressMonitor != null){
      configProgressMonitor.serverTotalParts(configElements.size());
    }
    for (ConfigurationElement element : configElements) {
      if (!cancelRequested) {
        //initialize success report
        ConfigurationElementReport elementReport = new ConfigurationElementReport(element.getAction(),
            element.getEntity(),
            element.getEntityId());
        report.addElementReport(elementReport);
        List<ProcessChange> processChanges = null;
        try {
          processChanges = applyConfigElement(element, elementReport);  //never returns null

          if (processChanges == null) {
            continue;
          }
          
          for (ProcessChange processChange : processChanges) {

            Long processId = processChange.getProcessId();
            if (processChange.processActionRequired()) {

              if (!processLists.containsKey(processId)) {
                processLists.put(processId, new ArrayList<Change>());
              }

              processLists.get(processId).add((Change) processChange.getChangeEvent());   //cast to implementation needed as DomFactory uses this - TODO change to interface

              if (processChange.hasNestedSubReport()) {
                elementReport.addSubReport(processChange.getNestedSubReport());
                daqReportPlaceholder.put(processChange.getChangeEvent().getChangeId(), processChange.getNestedSubReport());
              }
              else {
                daqReportPlaceholder.put(processChange.getChangeEvent().getChangeId(), elementReport);
              }

              elementPlaceholder.put(processChange.getChangeEvent().getChangeId(), element);
              element.setDaqStatus(Status.RESTART); //default to restart; if successful on DAQ layer switch to OK
            } else if (processChange.requiresReboot()) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(configId + " RESTART for " + processChange.getProcessId() + " required");
              }
              element.setDaqStatus(Status.RESTART);
              report.addStatus(Status.RESTART);
              report.addProcessToReboot(processCache.get(processId).getName());
              element.setStatus(Status.RESTART);
              processFacade.requiresReboot(processId, Boolean.TRUE);
            }
          }
        } catch (Exception ex) {
          String errMessage = configId + " Exception caught while applying the configuration change (Action, Entity, Entity id) = ("
            + element.getAction() + "; " + element.getEntity() + "; " + element.getEntityId() + ")";
          LOGGER.error(errMessage, ex);
          elementReport.setFailure("Exception caught while applying the configuration change.", ex);
          element.setStatus(Status.FAILURE);
          report.addStatus(Status.FAILURE);
          report.setStatusDescription("Failure: see details below.");
        }
        if (configProgressMonitor != null){
          configProgressMonitor.onServerProgress(progressCounter.getAndIncrement());
        }
      } else {
        LOGGER.info(configId + " Interrupting configuration due to cancel request.");
      }
    }

    //send events to Process if enabled, convert the responses and introduce them into the existing report; else set all DAQs to restart
    if (daqConfigEnabled) {
      if (configProgressMonitor != null){
        configProgressMonitor.daqTotalParts(processLists.size());
      }

      LOGGER.info(configId + " Reconfiguring " + processLists.keySet().size()+ " processes ...");

      AtomicInteger daqProgressCounter = new AtomicInteger(1);
      for (Long processId : processLists.keySet()) {
        if (!cancelRequested){
          List<Change> processChangeEvents = processLists.get(processId);
          if (processFacade.isRunning(processId) && !processFacade.isRebootRequired(processId)) {
            try {
              LOGGER.trace(configId + " Sending " + processChangeEvents.size() + " change events to process " + processId + "...");
              ConfigurationChangeEventReport processReport = processCommunicationManager.sendConfiguration(processId, processChangeEvents);
              if (!processReport.getChangeReports().isEmpty()) {

                LOGGER.trace(configId + " Received " + processReport.getChangeReports().size() + " back from process.");
              } else {
                LOGGER.trace(configId + " Received 0 reports back from process");
              }
              for (ChangeReport changeReport : processReport.getChangeReports()) {
                ConfigurationElementReport convertedReport =
                  ConfigurationReportConverter.fromProcessReport(changeReport, daqReportPlaceholder.get(changeReport.getChangeId()));
                daqReportPlaceholder.get(changeReport.getChangeId()).addSubReport(convertedReport);
                //if change report has REBOOT status, mark this DAQ for a reboot in the configuration
                if (changeReport.isReboot()) {
                  report.addProcessToReboot(processCache.get(processId).getName());
                  elementPlaceholder.get(changeReport.getChangeId()).setDaqStatus(Status.RESTART);
                  //TODO set flag & tag to indicate that process restart is needed
                } else if (changeReport.isFail()) {
                  LOGGER.debug(configId + " changeRequest failed at process " + processCache.get(processId).getName());
                  report.addStatus(Status.FAILURE);
                  report.setStatusDescription("Failed to apply the configuration successfully. See details in the report below.");
                  elementPlaceholder.get(changeReport.getChangeId()).setDaqStatus(Status.FAILURE);
                } else { //success, override default failure
                  if (elementPlaceholder.get(changeReport.getChangeId()).getDaqStatus().equals(Status.RESTART)) {
                    elementPlaceholder.get(changeReport.getChangeId()).setDaqStatus(Status.OK);
                  }
                }
              }
            } catch (Exception e) {
              String errorMessage = "Error during DAQ reconfiguration: unsuccessful application of configuration (possible timeout) to Process " + processCache.get(processId).getName();
              LOGGER.error(errorMessage, e);
              processFacade.requiresReboot(processId, true);
              report.addProcessToReboot(processCache.get(processId).getName());
              report.addStatus(Status.FAILURE);
              report.setStatusDescription(report.getStatusDescription() + errorMessage + "\n");
            }
          } else {
            processFacade.requiresReboot(processId, true);
            report.addProcessToReboot(processCache.get(processId).getName());
            report.addStatus(Status.RESTART);
          }
          if (configProgressMonitor != null) {
            configProgressMonitor.onDaqProgress(daqProgressCounter.getAndIncrement());
          }
        } else {
          LOGGER.info("Interrupting configuration " + configId + " due to cancel request.");
        }
      }
    } else {
      LOGGER.debug("DAQ runtime reconfiguration not enabled - setting required restart flags");
      if (!processLists.isEmpty()){
        report.addStatus(Status.RESTART);
        for (Long processId : processLists.keySet()) {
          processFacade.requiresReboot(processId, true);
          report.addProcessToReboot(processCache.get(processId).getName());
        }
      }
    }

    //LOGGER.info(configId + " Saving configuration ")
    //save Configuration element status information in the DB tables
    for (ConfigurationElement element : configElements) {
      configurationDAO.saveStatusInfo(element);
    }
    //mark the Configuration as applied in the DB table, with timestamp set
    configurationDAO.markAsApplied(configId);
    LOGGER.info("Finished applying configuraton " + configId);

    report.normalize();

    return report;
  }


  /**
   * Applies a single configuration element. On the DB level, this action should
   * either be entirely applied or the transaction rolled back. In the case of
   * a rollback, the cache should also reflect this rollback (emptied and reloaded
   * for instance).
   *
   * @param element the details of the configuration action
   * @param elementReport report that should be set to failed if there is a problem
   * @param changeId first free id to use in the sequence of changeIds, used for sending to DAQs *is increased by method*
   * @return list of DAQ configuration events; is never null but may be empty
   * @throws IllegalAccessException
   **/
  private List<ProcessChange> applyConfigElement(final ConfigurationElement element,
                                                 final ConfigurationElementReport elementReport) throws IllegalAccessException {
    // Write lock needed to avoid parallel Batch persistence transactions
    clusterCache.acquireWriteLockOnKey(this.cachePersistenceLock);
    //initialize the DAQ config event
    List<ProcessChange> daqConfigEvents = new ArrayList<ProcessChange>();
    try {
      if (LOGGER.isTraceEnabled()){
        LOGGER.trace(element.getConfigId() + " Applying configuration element with sequence id " + element.getSequenceId());
      }

      if (element.getAction() == null || element.getEntity() == null || element.getEntityId() == null) {
        elementReport.setFailure("Parameter missing in configuration line with sequence id " + element.getSequenceId());
        return null;
      }

//    String fieldName = element.getEntity().toString().toLowerCase() + "ConfigHandler";
//    Object configHandler = this.getClass().getField(fieldName).get(this);
//    Method createMethod = configHandler.getClass().getMethod("create" + element.getEntity().toString().toLowerCase(), parameterTypes)

      switch (element.getAction()) {
      case CREATE :
        switch (element.getEntity()) {
        case DATATAG : daqConfigEvents.add(dataTagConfigHandler.createDataTag(element)); break;
        case RULETAG : ruleTagConfigHandler.createRuleTag(element); break;
        case CONTROLTAG: daqConfigEvents.add(controlTagConfigHandler.createControlTag(element)); break;
        case COMMANDTAG : daqConfigEvents = commandTagConfigHandler.createCommandTag(element); break;
        case ALARM : alarmConfigHandler.createAlarm(element); break;
        case PROCESS : daqConfigEvents.add(processConfigHandler.createProcess(element));
                       element.setDaqStatus(Status.RESTART); break;
        case EQUIPMENT : daqConfigEvents.addAll(equipmentConfigHandler.createEquipment(element)); break;
        case SUBEQUIPMENT : daqConfigEvents.addAll(subEquipmentConfigHandler.createSubEquipment(element)); break;
        case DEVICECLASS : daqConfigEvents.add(deviceClassConfigHandler.createDeviceClass(element)); break;
        case DEVICE : daqConfigEvents.add(deviceConfigHandler.createDevice(element)); break;
        default : elementReport.setFailure("Unrecognized reconfiguration entity: " + element.getEntity());
          LOGGER.warn("Unrecognized reconfiguration entity: " + element.getEntity()
              + " - see reconfiguration report for details.");
        }
        break;
      case UPDATE :
        switch (element.getEntity()) {
        case DATATAG :
          daqConfigEvents.add(dataTagConfigHandler.updateDataTag(element.getEntityId(), element.getElementProperties())); break;
        case CONTROLTAG :
          daqConfigEvents.add(controlTagConfigHandler.updateControlTag(element.getEntityId(), element.getElementProperties())); break;
        case RULETAG :
          ruleTagConfigHandler.updateRuleTag(element.getEntityId(), element.getElementProperties()); break;
        case COMMANDTAG :
          daqConfigEvents.addAll(commandTagConfigHandler.updateCommandTag(element.getEntityId(), element.getElementProperties())); break;
        case ALARM :
          alarmConfigHandler.updateAlarm(element.getEntityId(), element.getElementProperties()); break;
        case PROCESS :
          daqConfigEvents.add(processConfigHandler.updateProcess(element.getEntityId(), element.getElementProperties())); break;
        case EQUIPMENT :
          daqConfigEvents.addAll(equipmentConfigHandler.updateEquipment(element.getEntityId(), element.getElementProperties())); break;
        case SUBEQUIPMENT :
          daqConfigEvents.addAll(subEquipmentConfigHandler.updateSubEquipment(element.getEntityId(), element.getElementProperties())); break;
        case DEVICECLASS :
          daqConfigEvents.add(deviceClassConfigHandler.updateDeviceClass(element.getEntityId(), element.getElementProperties())); break;
        case DEVICE :
          daqConfigEvents.add(deviceConfigHandler.updateDevice(element.getEntityId(), element.getElementProperties())); break;
        default : elementReport.setFailure("Unrecognized reconfiguration entity: " + element.getEntity());
          LOGGER.warn("Unrecognized reconfiguration entity: " + element.getEntity()
              + " - see reconfiguration report for details.");
        }
        break;
      case REMOVE :
        switch (element.getEntity()) {
        case DATATAG : daqConfigEvents.add(dataTagConfigHandler.removeDataTag(element.getEntityId(), elementReport)); break;
        case CONTROLTAG : daqConfigEvents.add(controlTagConfigHandler.removeControlTag(element.getEntityId(), elementReport)); break;
        case RULETAG : ruleTagConfigHandler.removeRuleTag(element.getEntityId(), elementReport); break;
        case COMMANDTAG : daqConfigEvents.addAll(commandTagConfigHandler.removeCommandTag(element.getEntityId(), elementReport)); break;
        case ALARM : alarmConfigHandler.removeAlarm(element.getEntityId(), elementReport); break;
        case PROCESS : daqConfigEvents.add(processConfigHandler.removeProcess(element.getEntityId(), elementReport)); break;
        case EQUIPMENT : daqConfigEvents.add(equipmentConfigHandler.removeEquipment(element.getEntityId(), elementReport)); break;
        case SUBEQUIPMENT : daqConfigEvents.addAll(subEquipmentConfigHandler.removeSubEquipment(element.getEntityId(), elementReport)); break;
        case DEVICECLASS : deviceClassConfigHandler.removeDeviceClass(element.getEntityId(), elementReport); break;
        case DEVICE : deviceConfigHandler.removeDevice(element.getEntityId(), elementReport); break;
        default : elementReport.setFailure("Unrecognized reconfiguration entity: " + element.getEntity());
        LOGGER.warn("Unrecognized reconfiguration entity: " + element.getEntity()
            + " - see reconfiguration report for details.");
        }
        break;
      default : elementReport.setFailure("Unrecognized reconfiguration action: " + element.getAction());
      LOGGER.warn("Unrecognized reconfiguration action: " + element.getAction()
          + " - see reconfiguration report for details.");
      }

      //set *unique* change id (single element may trigger many changes e.g. rule removal)
      if (!daqConfigEvents.isEmpty()) {
        for (ProcessChange processChange : daqConfigEvents) {
          if (processChange.processActionRequired()) {
            processChange.getChangeEvent().setChangeId(changeId);
            changeId++;
          }
        }
      }
    } finally {
      clusterCache.releaseWriteLockOnKey(this.cachePersistenceLock);
    }

    return daqConfigEvents;
  }


  /**
   * Save the report to disk.
   * @param configId id of the config
   * @param xmlReport the XML report in String format
   */
  private void archiveReport(String configId, String xmlReport) {
    try {
      File outFile = new File(reportDirectory, "report_" + configId + "_" + System.currentTimeMillis() + ".xml");
      FileWriter fileWriter;
      fileWriter = new FileWriter(outFile);
      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(xmlReport);
      bufferedWriter.close();
    } catch (Exception e) {
      LOGGER.error("Exception caught while writing configuration report to directory: "
          + reportDirectory, e);
    }
  }

  /**
   * @param daqConfigEnabled the daqConfigEnabled to set
   */
  public void setDaqConfigEnabled(final boolean daqConfigEnabled) {
    this.daqConfigEnabled = daqConfigEnabled;
  }

  /**
   * Set the (absolute) directory where the config reports should be saved.
   * @param reportDirectory report directory
   */
  public void setReportDirectory(final String reportDirectory) {
    this.reportDirectory = reportDirectory;
  }

  @Override
  public void cancelCurrentConfiguration() {
    cancelRequested = true;
  }

  @Override
  public ConfigurationReport applyConfiguration(int configId) {
    return applyConfiguration(configId, null);
  }

  @Override
  public List<ConfigurationReportHeader> getConfigurationReports() {
    List<ConfigurationReportHeader> reports = new ArrayList<>();

    // Read all report files and deserialise them
    try {
      ArrayList<File> files = new ArrayList<File>(Arrays.asList(new File(reportDirectory).listFiles(new ConfigurationReportFileFilter())));
      Serializer serializer = getSerializer();

      for (File file : files) {
        ConfigurationReportHeader report = serializer.read(ConfigurationReportHeader.class, file);
        LOGGER.debug("Deserialised configuration report " + report.getId());
        reports.add(report);
      }

    } catch (Exception e) {
      LOGGER.error("Error deserialising configuration report", e);
    }

    return reports;
  }

  @Override
  public List<ConfigurationReport> getConfigurationReports(String id) {
    List<ConfigurationReport> reports = new ArrayList<>();

    try {
      ArrayList<File> files = new ArrayList<File>(Arrays.asList(new File(reportDirectory).listFiles(new ConfigurationReportFileFilter(id))));
      Serializer serializer = getSerializer();

      for (File file : files) {
        ConfigurationReport report = serializer.read(ConfigurationReport.class, file);
        LOGGER.debug("Deserialised configuration report " + report.getId());
        reports.add(report);
      }

    } catch (Exception e) {
      LOGGER.error("Error deserialising configuration report", e);
    }

    return reports;
  }

  /**
   * Retrieve a {@link Serializer} instance suitable for deserialising a
   * {@link ConfigurationReport}.
   *
   * @return a new {@link Serializer} instance
   */
  private Serializer getSerializer() {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    RegistryMatcher matcher = new RegistryMatcher();
    matcher.bind(Timestamp.class, new DateFormatConverter(format));
    Strategy strategy = new AnnotationStrategy();
    Serializer serializer = new Persister(strategy, matcher);
    return serializer;
  }
}
