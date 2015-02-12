/******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
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
 * Author: TIM team, tim.support@cern.ch
 *****************************************************************************/
package cern.c2mon.daq.dip;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cern.c2mon.daq.common.logger.EquipmentLogger;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.datatag.SourceDataQuality;
import cern.c2mon.shared.common.datatag.address.DIPHardwareAddress;
import cern.dip.BadParameter;
import cern.dip.DipData;
import cern.dip.DipQuality;
import cern.dip.DipSubscription;
import cern.dip.DipSubscriptionListener;
import cern.dip.TypeMismatch;

/**
 * This class is a handler for connect/disconnect/data reception events
 *
 * @author vilches
 *
 */
public class DipMessageHandlerDataListener implements DipSubscriptionListener {

  /**
   * ThreadPoolExecutor parameter CORE_POOL_SIZE
   */
  private static final int CORE_POOL_SIZE = 2;

  /**
   * ThreadPoolExecutor parameter MAX_POOL_SIZE
   */
  private static final int MAX_POOL_SIZE = Integer.MAX_VALUE;

  /**
   * ThreadPoolExecutor parameter KEEP_ALIVE_TIME
   */
  private static final long KEEP_ALIVE_TIME = 10L * 1000L; // 60s

  /**
   * ThreadPoolExecutor parameter MAX_QUEUE_SIZE
   */
  private static final int MAX_QUEUE_SIZE = 1000;

  /**
   * DIP controller
   */
  private DIPController dipController;

  /**
   *  Executes each submitted task using one of possibly several pooled threads
   */
  private ThreadPoolExecutor executor;

  /**
   * The equipment logger of this class.
   */
  private EquipmentLogger equipmentLogger;

  /**
   * The default constructor.
   */
  public DipMessageHandlerDataListener(final DIPController dipController, final EquipmentLogger equipmentLogger) {
    this.dipController = dipController;
    this.equipmentLogger = equipmentLogger;

    this.executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(MAX_QUEUE_SIZE));
    this.executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
        getEquipmentLogger().warn("rejectedExecution - Error in DIP thread pool. Is it running full?");
      }
    });
  }

  /**
   * The DIPMessageHandler uses DipMessageHandlerDataListener to handle
   * changes to subscribed data
   *
   * @param subscription
   *            the subscription object
   * @param message
   *            the DIP incomming message
   * @roseuid 409BE0D20337
   */
  @Override
  public void handleMessage(final DipSubscription subscription, final DipData message) {
    this.executor.execute(new Runnable() {

      @Override
      public void run() {
        // TODO Auto-generated method stub

        Vector<ISourceDataTag> t4topic = null;
        try {
          getEquipmentLogger().debug("handleMessage - entering handleMessage..");

          // get DipData's timestamp
          final long timestamp = message.extractDipTime().getAsMillis();
          getEquipmentLogger().debug("\ttimestamp retrieved from the DipData : " + new Timestamp(timestamp));

          getEquipmentLogger().debug("\tchecking data quality");
          boolean propagateData = true;
          String errorQualityDescription = "";

          switch (message.extractDataQuality()) {
            case DipQuality.DIP_QUALITY_UNINITIALIZED:
              getEquipmentLogger().debug("\treceived DipData with quality : DIP_QUALITY_UNINITIALIZED, quality descr.: "
                  + message.extractQualityString());
              getEquipmentLogger().debug("\tfor topic : " + subscription.getTopicName());
              getEquipmentLogger().debug("\tNOTE : This value will not be propagated");

              errorQualityDescription = "DIP_QUALITY_UNINITIALIZED:" + message.extractQualityString();
              propagateData = false;
              break;

            case DipQuality.DIP_QUALITY_UNCERTAIN:
              getEquipmentLogger().debug("\treceived DipData with quality : DIP_QUALITY_UNCERTAIN, quality descr.: "
                  + message.extractQualityString());
              getEquipmentLogger().debug("\tfor topic : " + subscription.getTopicName());
              getEquipmentLogger().debug("\tNOTE : This value will not be propagated !");

              errorQualityDescription = "DIP_QUALITY_UNCERTAIN:" + message.extractQualityString();
              propagateData = false;
              break;

            case DipQuality.DIP_QUALITY_BAD:
              getEquipmentLogger().debug("\treceived DipData with quality : DIP_QUALITY_BAD, quality descr.: " + message.extractQualityString());
              getEquipmentLogger().debug("\tfor topic : " + subscription.getTopicName());
              getEquipmentLogger().debug("\tNOTE : This value will not be propagated !");

              errorQualityDescription = "DIP_QUALITY_BAD:" + message.extractQualityString();
              propagateData = false;
              break;

            case DipQuality.DIP_QUALITY_GOOD:
              getEquipmentLogger().debug("\treceived DipData with quality : DIP_QUALITY_GOOD");
              break;

            default:
              getEquipmentLogger().warn("\tno data quality matches the options");
              break;
          }

          // if the quality wasn't good - exit
          if (!propagateData) {
            // get all SourceDataTags object related with that
            // item-name (topic)
            t4topic = dipController.getSubscribedDataTags().get(subscription.getTopicName());
            Enumeration<ISourceDataTag> tags4topic = t4topic.elements();
            while (tags4topic.hasMoreElements()) {
              ISourceDataTag sdt = tags4topic.nextElement();
              getEquipmentLogger().info("\tinvalidating SourceDataTag: " + sdt.getName()
                  + " tag id: " + sdt.getId() + " ,with description: " + errorQualityDescription);
              dipController.getEquipmentMessageSender().sendInvalidTag(sdt, SourceDataQuality.DATA_UNAVAILABLE,
                  errorQualityDescription, new Timestamp(timestamp));
            }

            getEquipmentLogger().debug("leaving handleMessage");
            return;
          }

          // get all SourceDataTags object related with that
          // item-name (topic)
          t4topic = dipController.getSubscribedDataTags().get(subscription.getTopicName());
          Enumeration<ISourceDataTag> tags4topic = t4topic.elements();
          if (!tags4topic.hasMoreElements()) {
            getEquipmentLogger().debug("\treceived a callback for unsubscribed tag! Skipping it..");
            getEquipmentLogger().debug("leaving handleMessage");
            return;
          } else {
            getEquipmentLogger().debug("\t" + t4topic.size() + " tags are currently registered for topic " + subscription.getTopicName());
          }

          while (tags4topic.hasMoreElements()) {
            ISourceDataTag sdt = (ISourceDataTag) tags4topic.nextElement();

            getEquipmentLogger().debug("\tSourceDataTag name: " + sdt.getName() + " id: " + sdt.getId());

            // get related subscription object form the
            // collection
            getEquipmentLogger().debug("\tsubscription topicName : " + subscription.getTopicName());

            DIPHardwareAddress dipDataTagAddress = (DIPHardwareAddress) sdt.getHardwareAddress();
            getEquipmentLogger().debug("\tdip-hw-address item name : " + dipDataTagAddress.getItemName());

            boolean boolValue = false;
            byte byteValue = 0;
            short shortValue = 0;
            int intValue = 0;
            long longValue = 0;
            float floatValue = 0.0f;
            double doubleValue = 0.0;
            String stringValue = "";
            Object objToSend = null;

            int valueType = 0;

            // check if this sourceDataTag contains simple or
            // complex values
            if (dipDataTagAddress.isComplexItem()) {
              getEquipmentLogger().debug("\ta complex data received");
              // if the value is complex (structured), we'll
              // taking only one from the
              // structure, by specifying the field name

              getEquipmentLogger().debug("\ttrying to extract the type and value of the filed :" + dipDataTagAddress.getFieldName());

              getEquipmentLogger().debug("\tthe field is ");
              valueType = message.getValueType(dipDataTagAddress.getFieldName());

              switch (valueType) {

                case DipData.TYPE_NULL:
                  getEquipmentLogger().debug("\t ..of type NULL !");
                  if (message.isEmpty()) {
                    getEquipmentLogger().error("\tempty message received !");
                  } else {
                    getEquipmentLogger().warn("\tfield not found!");
                    getEquipmentLogger().warn("\tthis publication does not have field : " + dipDataTagAddress.getFieldName());
                  }
                  break;

                case DipData.TYPE_BOOLEAN:
                  getEquipmentLogger().debug("\t ..of type BOOLEAN");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    boolValue = message.extractBoolean(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, boolValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BOOLEAN_ARRAY:
                  getEquipmentLogger().debug("\t ..of type BOOLEAN_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : "
                        + dipDataTagAddress.getFieldName() + "and array index : " + dipDataTagAddress.getFieldIndex());
                    boolValue = message.extractBooleanArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, boolValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BYTE:
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    byteValue = message.extractByte(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, byteValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+byteValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+byteValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BYTE_ARRAY:
                  getEquipmentLogger().debug("\t ..of type BYTE_ARRAY,    NOTE : Bytes will be converted to Integer");
                  try {
                    getEquipmentLogger().debug(
                        "\tgetting the value using field name : " + dipDataTagAddress.getFieldName()
                          + "and array index : " + dipDataTagAddress.getFieldIndex());
                    byteValue = message.extractByteArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, byteValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+byteValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+byteValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_SHORT:
                  getEquipmentLogger().debug("\t ..of type SHORT");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    shortValue = message.extractShort(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, shortValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+shortValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+shortValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_SHORT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type SHORT_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : "
                        + dipDataTagAddress.getFieldName() + "and array index : " + dipDataTagAddress.getFieldIndex());
                    shortValue = message.extractShortArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, shortValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+shortValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+shortValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_INT:
                  getEquipmentLogger().debug("\t ..of type INT");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    intValue = message.extractInt(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, intValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+intValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+intValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_INT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type INT_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : "
                        + dipDataTagAddress.getFieldName() + "and array index : " + dipDataTagAddress.getFieldIndex());
                    intValue = message.extractIntArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, intValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+intValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+intValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_LONG:
                  getEquipmentLogger().debug("\t ..of type LONG");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    longValue = message.extractLong(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, longValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+longValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+longValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_LONG_ARRAY:

                  getEquipmentLogger().debug("\t ..of type LONG_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : "
                        + dipDataTagAddress.getFieldName() + "and array index : " + dipDataTagAddress.getFieldIndex());
                    longValue = message.extractLongArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, longValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+longValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+longValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_FLOAT:
                  getEquipmentLogger().debug("\t ..of type FLOAT");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    floatValue = message.extractFloat(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, floatValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+floatValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+floatValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_FLOAT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type FLOAT_ARRAY");
                  try {
                    getEquipmentLogger().debug(
                        "\tgetting the value using field name : " + dipDataTagAddress.getFieldName()
                          + "and array index : " + dipDataTagAddress.getFieldIndex());
                    floatValue = message.extractFloatArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, floatValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+floatValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+floatValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_DOUBLE:
                  getEquipmentLogger().debug("\t ..of type DOUBLE");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : " + dipDataTagAddress.getFieldName());
                    doubleValue = message.extractDouble(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, doubleValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+doubleValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+doubleValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_DOUBLE_ARRAY:
                  getEquipmentLogger().debug("\t ..of type DOUBLE_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using field name : "
                        + dipDataTagAddress.getFieldName() + "and array index : " + dipDataTagAddress.getFieldIndex());
                    doubleValue = message.extractDoubleArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, doubleValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+doubleValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+doubleValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_STRING:
                  getEquipmentLogger().debug("\t ..of type STRING");
                  try {
                    stringValue = message.extractString(dipDataTagAddress.getFieldName());
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, stringValue, timestamp)) {
                    getEquipmentLogger().debug("\tNew value ("+stringValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("\tProblems sending new value ("+stringValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_STRING_ARRAY:
                  getEquipmentLogger().debug("\t ..of type STRING_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    stringValue = message.extractStringArray(dipDataTagAddress.getFieldName())[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  } catch (BadParameter ex) {
                    getEquipmentLogger().error("\thandleMessage : BadParameter exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, stringValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+stringValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+stringValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                default:

                  getEquipmentLogger().debug("\t ..of type :" + valueType);
                  getEquipmentLogger().error("The dip is unsupported by the DipMessageHandler!");

              }// switch

            } // if complex
            else {
              // if the value is simple, we'll extracting by
              // specifying it directly
              // exctract data of appropriate type
              getEquipmentLogger().debug("\ta simple data received");
              valueType = message.getValueType();

              switch (valueType) {

                case DipData.TYPE_NULL:
                  getEquipmentLogger().debug("\t ..of type NULL !");
                  if (message.isEmpty()) {
                    getEquipmentLogger().error("\tempty message received !");
                  } else {
                    getEquipmentLogger().error("\tthe tag was not found !");
                    getEquipmentLogger().warn("\tthis publication may not exist!");
                  }
                  break;

                case DipData.TYPE_BOOLEAN:
                  getEquipmentLogger().debug("\t ..of type BOOLEAN");
                  try {
                    boolValue = message.extractBoolean();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, boolValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BOOLEAN_ARRAY:
                  getEquipmentLogger().debug("\t ..of type BOOLEAN_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    boolValue = message.extractBooleanArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, boolValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BYTE:
                  getEquipmentLogger().debug("\t ..of type BYTE    NOTE : Bytes will be converted to Integer");
                  try {
                    byteValue = message.extractByte();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, byteValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_BYTE_ARRAY:
                  getEquipmentLogger().debug("\t ..of type BYTE_ARRAY    NOTE : Bytes will be converted to Integer");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    byteValue = message.extractByteArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, byteValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+boolValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+boolValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_SHORT:
                  getEquipmentLogger().debug("\t ..of type SHORT");
                  try {
                    shortValue = message.extractShort();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, shortValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+shortValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+shortValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_SHORT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type SHORT_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    shortValue = message.extractShortArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, shortValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+shortValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+shortValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_INT:
                  getEquipmentLogger().debug("\t ..of type INT");
                  try {
                    intValue = message.extractInt();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = "
                        + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, intValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+intValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+intValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_INT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type INT_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    intValue = message.extractIntArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, intValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+intValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+intValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_LONG:
                  getEquipmentLogger().debug("\t ..of type LONG");
                  try {
                    longValue = message.extractLong();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, longValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+longValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+longValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_LONG_ARRAY:
                  getEquipmentLogger().debug("\t ..of type LONG_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : "
                        + dipDataTagAddress.getFieldIndex());
                    longValue = message.extractLongArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, longValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+longValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+longValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_FLOAT:
                  getEquipmentLogger().debug("\t ..of type FLOAT");
                  try {
                    floatValue = message.extractFloat();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, floatValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+floatValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+floatValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_FLOAT_ARRAY:
                  getEquipmentLogger().debug("\t ..of type FLOAT_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : "
                        + dipDataTagAddress.getFieldIndex());
                    floatValue = message.extractFloatArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, floatValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+floatValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+floatValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_DOUBLE:
                  getEquipmentLogger().debug("\t ..of type DOUBLE");
                  try {
                    doubleValue = message.extractDouble();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, doubleValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+doubleValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+doubleValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_DOUBLE_ARRAY:
                  getEquipmentLogger().debug("\t ..of type DOUBLE_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : "
                        + dipDataTagAddress.getFieldIndex());
                    doubleValue = message.extractDoubleArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, doubleValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+doubleValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+doubleValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_STRING:
                  getEquipmentLogger().debug("\t ..of type STRING");
                  try {
                    stringValue = message.extractString();
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, stringValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+stringValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+stringValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                case DipData.TYPE_STRING_ARRAY:
                  getEquipmentLogger().debug("\t ..of type STRING_ARRAY");
                  try {
                    getEquipmentLogger().debug("\tgetting the value using array index : " + dipDataTagAddress.getFieldIndex());
                    stringValue = message.extractStringArray()[dipDataTagAddress.getFieldIndex()];
                  } catch (TypeMismatch ex) {
                    getEquipmentLogger().error("\thandleMessage : TypeMismatch exception caugth. Exception message = " + ex.getMessage());
                    getEquipmentLogger().error(ex);
                    break;
                  }

                  // Send the new value to the server
                  if (dipController.getEquipmentMessageSender().sendTagFiltered(sdt, stringValue, timestamp)) {
                    getEquipmentLogger().debug("	New value ("+stringValue+") sent to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  } else {
                    getEquipmentLogger().debug("	Problems sending new value ("+stringValue+") to the server for Tag name : "
                        + sdt.getName() + " tag id : " + sdt.getId());
                  }

                  break;

                default:
                  getEquipmentLogger().debug("\t ..of type :" + valueType);
                  getEquipmentLogger().error("\tThe dip is unsupported by the DipMessageHandler!");

              }// switch

            } // if complex

          }// while
        } catch (Throwable ex) {
          if (t4topic != null) {
            for (ISourceDataTag tag : t4topic) {
              getEquipmentLogger().error("\tReceived exception while treating incoming DIP value update for tag " + tag.getId() + ", " + ex.getStackTrace(), ex);
              dipController.getEquipmentMessageSender().sendInvalidTag(tag, SourceDataQuality.UNKNOWN,
                  "Problem occured when receiving DIP value: " + ex.getMessage() + ". Review tag configuration.");
            }
          }
        }
        getEquipmentLogger().debug("\thandleMessage - leaving handleMessage");
      }
    });
  }

  /**
   * This method is called-back when a publication the driver is
   * subscribed to becomes available. In the subscription has previously
   * been lost, the method requests update of the tags related to the
   * subscription that has been re-established.
   *
   * @param subscription
   *            The subscription who's publication is available
   */
  @Override
  public void connected(DipSubscription subscription) {
    getEquipmentLogger().info("connected - Publication " + subscription.getTopicName() + " is available");
    getEquipmentLogger().debug("\t requesting update!");
    // removed as not used
    // connectionDropped4Subscription.put(subscription,Boolean.FALSE);
    subscription.requestUpdate();
  }

  /**
   * This method is called-back when a publication the driver is
   * subscribed to becomes unavailable. The method invalidates the tags
   * related with to the subscription that has been dropped.
   *
   * @param subscription
   *            The subscription who's publication is unavailable.
   * @param reason
   *            String providing more information about why the
   *            publication is unavailable.
   */
  @Override
  public void disconnected(DipSubscription subscription, String reason) {
    if (getEquipmentLogger().isDebugEnabled()) {
      getEquipmentLogger().debug("disconnected - entering disconnected()..");
      getEquipmentLogger().debug("disconnected - invalidating all tags registered for subscription: " + subscription.getTopicName());
    }

    getEquipmentLogger().warn("disconnected - Publication " + subscription.getTopicName() + " is unavailable. Reason : " + reason);

    // get related SourceDataTag objects
    Vector<ISourceDataTag> tags4topic = dipController.getSubscribedDataTags().get(subscription.getTopicName());
    Iterator<ISourceDataTag> it = tags4topic.iterator();
    while (it.hasNext()) {
      ISourceDataTag sdt = it.next();

      getEquipmentLogger().info("\tinvalidating SourceDataTag: " + sdt.getName() + " tag id: " + sdt.getId());
      this.dipController.getEquipmentMessageSender().sendInvalidTag(sdt, SourceDataQuality.DATA_UNAVAILABLE, "subscription lost. Reason: " + reason);

    } // while

    // removed as not used
    // connectionDropped4Subscription.put(subscription, Boolean.TRUE);

    if (getEquipmentLogger().isDebugEnabled()) {
      getEquipmentLogger().debug("disconnected - exiting disconnected()..");
    }
  }

  /**
   * This method is called-back when some problems with particular
   * subscription appears.
   *
   * @param ds
   *            - dip subscription
   * @param de
   *            - an exception with problem explanation
   */
  @Override
  public void handleException(DipSubscription ds, Exception de) {
    getEquipmentLogger().error("handleException - An error appeared for publication source: "
        + ds.getTopicName() + ", error msg.: " + de.getMessage() + ", error trace: ", de);
    Collection<ISourceDataTag> collection = dipController.getSubscribedDataTags().get(ds.getTopicName());
    if (collection != null) {
      Iterator<ISourceDataTag> iter = collection.iterator();
      while (iter.hasNext()) {
        this.dipController.getEquipmentMessageSender().sendInvalidTag(iter.next(), SourceDataQuality.DATA_UNAVAILABLE,
            "handleException() received from DIP: " + de.getMessage());
      }
    }
  }

  /**
   * @return the equipmentLogger
   */
  public EquipmentLogger getEquipmentLogger() {
      return this.equipmentLogger;
  }
}

