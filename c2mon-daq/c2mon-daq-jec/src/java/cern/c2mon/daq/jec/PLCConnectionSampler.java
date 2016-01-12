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
package cern.c2mon.daq.jec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.c2mon.daq.common.logger.EquipmentLogger;

/**
 * This class is used to handle the connection status of the handler. Every
 * time a message arrives to the handler, the actual timestamp is updated.
 * This way, this thread tests if the time difference between the host
 * timestamp and the actual time stamp (time when last message was received)
 * isn't bigger than the maximum delay time. If it is, kills the handler and
 * restarts a new one, reconfiguring the PLC from the beginning.
 */
public class PLCConnectionSampler extends Thread {
    
    /**
     * Private logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(PLCConnectionSampler.class);
    /**
     * The message handler to control.
     */
    private IJECRestarter jecRestarter;
    /**
     * Timestamp of the current AliveTag received
     */
    private long currentAliveTagTime;
    /**
     * The time between checks.
     */
    private long samplerPeriod = 0;
    /**
     * Timestamp of the previous AliveTag received
     */
    private long previousAliveTagTime = 0;
    /**
     * The logger to use.
     */
    private EquipmentLogger equipmentLogger;
    /**
     * Flag indicating final shutdown request of this sampler.
     */
    private volatile boolean stopRequested = false;
    
    /**
     * The PLC connection sampler to use.
     * 
     * @param jecRestarter Object to restart the JEC DAQ.
     * @param equipmentLogger The equipment logger to use.
     * @param samplerPeriod The sampler period.
     */
    public PLCConnectionSampler(final IJECRestarter jecRestarter, 
            final EquipmentLogger equipmentLogger, final long samplerPeriod) {
        super("plc-connection-sampler");
        // Defines the Thread priority to 1
        this.jecRestarter = jecRestarter;
        setPriority(Thread.MAX_PRIORITY);
        setDaemon(true);
        this.samplerPeriod = samplerPeriod;
        this.equipmentLogger = equipmentLogger;
    }

    /**
     * the run method which implements the behavior of this thread.
     */
    public void run() {
        // Forever cycle (while connection is established)
        while (true && !stopRequested) {
            try {
                Thread.sleep(samplerPeriod);
            } catch (java.lang.InterruptedException ex) {
                equipmentLogger.debug("Interrupted during sampler thread sleep.");
            }
            // No new timestamp - CONNECTION LOST
            if (!stopRequested && previousAliveTagTime == currentAliveTagTime) {
                equipmentLogger.debug("PLCConnectionSampler : Connection LOST Detected !!, Setting 'connected' to OFF");
                jecRestarter.forceImmediateRestart();
                break;
            }
            // All went fine - STILL CONNECTED
            else {
                equipmentLogger.debug("PLCConnectionSampler : Connection OK!");
                previousAliveTagTime = currentAliveTagTime;
            }
        }
    }
    
    /**
     * Assigns the current system time to a field.
     * With this field the AliveSampler can check if the connection
     * to the equipment is still open.
    */
    public synchronized void updateAliveTimer() {
        currentAliveTagTime = System.currentTimeMillis();
    }

    /**
     * Returns the current alive timer value.
     * 
     * @return The current alive timer value.
     */
    public synchronized long getAliveTimer() {
        return currentAliveTagTime;
    }
    
    /**
     * Shuts down the sampler. Needs re-instantiating.
     */
    public void shutdown() {
      stopRequested = true;
      this.interrupt();
    }
}
