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

package cern.c2mon.daq.clic;

import static java.lang.String.format;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.datatag.address.JAPCHardwareAddress;
import cern.c2mon.shared.common.process.IEquipmentConfiguration;
import cern.dmn2.agentlib.AgentClient;
import cern.dmn2.agentlib.AgentContext;
import cern.dmn2.agentlib.CommandType;
import cern.dmn2.agentlib.FieldDataType;

/**
 * The <code>ClicConfigurationTask</code> class implements a task responsible for automatic CLIC reconfiguration
 *
 * @author wbuczak
 */
public class ClicConfigurationTask implements Runnable {

    private static final int RECONF_QUEUE_MAX_CAPACITY = 2000;

    protected static int MIN_TIME_BETWEEN_TWO_RECONFIGURATIONS = 10000;

    private static BlockingQueue<ClicMessageHandler> reconfQueue = new ArrayBlockingQueue<ClicMessageHandler>(
            RECONF_QUEUE_MAX_CAPACITY);

    private static Map<String, Long> lastReconfigurred = new ConcurrentHashMap<String, Long>();

    private static final Logger logger = LoggerFactory.getLogger(ClicConfigurationTask.class);

    private static AgentClient client = AgentClient.getInstance();

    private static final String CLIC_CONFIGURATION_PROPERTY = "Setting";
    private static final String CLIC_CONFIGURATION_CONTEXT_FIELD = "entries";

    public static void registerForReconfiguration(ClicMessageHandler handler) {
        if (!reconfQueue.offer(handler)) {
            IEquipmentConfiguration econf = handler.getEquipmentConfiguration();
            logger.warn(format(
                    "could not register equipment: [%d ,%s] for reconfiguration. maximum capacity of the reconfiguration queue reached",
                    econf.getId(), econf.getName()));
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // take the CLIC equipment that needs to be reconfigured
                // wait for one if none available

                ClicMessageHandler mHandler = reconfQueue.take();

                IEquipmentConfiguration econf = mHandler.getEquipmentConfiguration();

                // check if this CLIC has already been reconfigured
                if (lastReconfigurred.containsKey(econf.getName())) {
                    long lastReconfiguration = lastReconfigurred.get(econf.getName());

                    if (System.currentTimeMillis() - lastReconfiguration < MIN_TIME_BETWEEN_TWO_RECONFIGURATIONS) {
                        // skip the reconfiguration this time
                        continue;
                    }
                }

                sendClicConfiguration(mHandler.getEquipmentConfiguration());

                lastReconfigurred.put(econf.getName(), System.currentTimeMillis());
            } catch (InterruptedException ex) {
                logger.warn("InterruptedException caught", ex);
            }
        }
    }// run

    private void sendClicConfiguration(IEquipmentConfiguration equipmentConf) {

        if (logger.isTraceEnabled()) {
            logger.trace(format("entering sendClicConfiguration(%s)..", equipmentConf.getName()));
        }

        String device = null;
        StringBuilder payload = new StringBuilder();

        int metricsCount = equipmentConf.getSourceDataTags().values().size();
        int counter = 0;
        for (ISourceDataTag tag : equipmentConf.getSourceDataTags().values()) {
            JAPCHardwareAddress addr = (JAPCHardwareAddress) tag.getHardwareAddress();
            device = addr.getDeviceName();
            String field = addr.getDataFieldName();

            payload.append(field);
            if (counter++ < metricsCount - 1) {
                payload.append(",");
            }
        }

        if (logger.isDebugEnabled()) {
            logger.info(format("CLIC: %s is about to be re-configured with %d metrics", device, metricsCount));
        }

        AgentContext context = new AgentContext();
        context.add(CLIC_CONFIGURATION_CONTEXT_FIELD, FieldDataType.TYPE_STRING, payload.toString());

        try {
            client.sendCommand(device, CLIC_CONFIGURATION_PROPERTY, CommandType.SET, context);
        } catch (Exception ex) {
            logger.warn(format("Reconfiguration of CLIC: %s failed. Problem: %s", device, ex.getMessage()), ex);
        }

        logger.trace("leaving sendClicConfiguration()");
    }
}
