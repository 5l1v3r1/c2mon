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
package cern.c2mon.daq.common.conf.core;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cern.c2mon.daq.tools.CommandParamsHandler;

/**
 * Class holding fields describing the runtime state of the DAQ (running in test
 * or filter mode, start up time, etc.)
 * 
 * It also holds a reference to the process name and id, which are used at start
 * up and shut down.
 * 
 * @author mbrightw
 * 
 */
@Service
public class RunOptions {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RunOptions.class);

    /**
     * Reference to the command line parameter object.
     */
    @Resource
    private CommandParamsHandler commandParamsHandler;

    /**
     * Time (in milliseconds since 1/1/1970) of the daq startup.
     */
    private long startUp;

    /**
     * Flag indicating whether the filtered messages should or should not be
     * sent to the filter module.
     */
    private boolean filterMode = true;

    /**
     * This flag is used for switching on/off separate equipment logger's
     * appenders
     */
    private boolean eqLoggers = false;

    /**
     * This flag is used in pair with eqLoggers flag, to define whether
     * equipment loggers should append both process and equipment appenders or
     * only equipment
     */
    private boolean eqAppendersOnly = false;

    /**
     * Initialization of the bean.
     */
    @PostConstruct
    public void init() {

        // set start up time on bean initialization
        setStartUp(System.currentTimeMillis());

        // set process name
        // check if user wants separate loggers for equipment units
        if (commandParamsHandler.hasParam("-eqLoggers")) {
            setEqLoggers(true);
            if (commandParamsHandler.hasParam("-eqAppendersOnly")) {
                setEqAppendersOnly(true);
            }
        }

        // check if the filtering should be turned off (default is on)
        if (commandParamsHandler.hasParam("-noFilter") || commandParamsHandler.hasParam("-nf")) {
            LOGGER.info("The DAQ process is starting without filtering (no JMS connections will be opened with Filter module)");
            setFilterMode(false);
        }
    }

    /**
     * returns the filterMode of the process
     * 
     * @return the filterMode
     */
    public final boolean isFilterMode() {
        return filterMode;
    }

    /**
     * sets the filterMode for the process
     * 
     * @param filterMode
     *            to be set
     */
    public final void setFilterMode(final boolean filterMode) {
        this.filterMode = filterMode;
    }

    /**
     * This method sets the eqLoggers boolean flag
     * 
     * @param eqLoggers
     *            boolean flag stating whether each equipment unit should have
     *            separate file appender or not
     */
    public final void setEqLoggers(final boolean eqLoggers) {
        this.eqLoggers = eqLoggers;
    }

    /**
     * This method returns eqLoggers boolean flag
     * 
     * @return boolean
     */
    public boolean getEqLoggers() {
        return this.eqLoggers;
    }

    /**
     * This method sets the setEqAppendersOnly boolean flag
     * 
     * @param eqAppendersOnly
     *            boolean flag stating whether each equipment logger should
     *            append only its related appender or also the process logger's
     *            appender.
     */
    public final void setEqAppendersOnly(final boolean eqAppendersOnly) {
        this.eqAppendersOnly = eqAppendersOnly;
    }

    /**
     * This method returns eqAppendersOnly boolean flag
     * 
     * @return boolean
     */
    public boolean getEqAppendersOnly() {
        return this.eqAppendersOnly;
    }

    /**
     * Setter method.
     * 
     * @param commandParamsHandler
     *            the commandParamsHandler to set
     */
    public final void setCommandParamsHandler(final CommandParamsHandler commandParamsHandler) {
        this.commandParamsHandler = commandParamsHandler;
    }

    /**
     * This method sets the startup time of the process (in milliseconds)
     * 
     * @param pStartUp
     *            time in milliseconds
     */
    public final void setStartUp(final long pStartUp) {
        startUp = pStartUp;
    }

    /**
     * This method gets the startup time of the process (in milliseconds)
     * 
     * @return long
     */
    public long getStartUp() {
        return startUp;
    }
}
