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
package cern.c2mon.shared.daq.process;


import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.w3c.dom.Document;

/**
 * This class is used by:
 *  - the DAQ core to receive a Process Configuration respond from C2MON
 *  - the C2MON to send the respond to the DAQ core after a request
 * 
 * The class is serialized as XML before being sent and
 * deserialized as Object after being received.
 * 
 * Simpleframework XML needs always a value
 * 
 * @author vilches
 */
@Root(name = "process-configuration-response")
public class ProcessConfigurationResponse implements ProcessResponse {
  /** Log4j instance */
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessConfigurationResponse.class);
  
  /**
   * Constant of the CONF_REJECTED. All number which are zero or below
   * will lead to a stop of the DAQ.
   */
  public static final String CONF_REJECTED = "CONF_REJECTED";
  
  /**
   * Constant of NO_PROCESS as default value for process name
   */
  public static final String NO_PROCESS = "NO_PROCESS";
  
  /**
   * Constant of NO_XML as default value for process configuration
   */
  public static final String NO_XML = "NO_XML";

  /**
   * Unique name of the Process that wishes to connect.
   */
  @Element
  protected String processName = NO_PROCESS;
  
  /** 
   *  Configuration to return to the DAQ
   */
  @Element
  private String configurationXML = NO_XML;

  
  /**
   * Constructor
   *  - processName is NO_PROCESS by default
   *  - configurationXML is NO_XML by default
   * 
   * Empty constructor
   */
  public ProcessConfigurationResponse() {}

  /**
   * Constructor
   *  - configurationXML is NO_XML by default
   * 
   * @param processName name of the Process that wishes to connect
   */
  public ProcessConfigurationResponse(final String processName) {
    this.processName = processName;
  }

  /**
   * Sets the process name of the process.
   * 
   * @param processName The name of the process.
   */
  public final void setProcessName(final String processName) {
    this.processName = processName;
  }
  
  /**
   * @return the name of the process that wants to connect.
   */
  public final String getProcessName() {
    return this.processName;
  }
  
  /**
   * Sets the configurationXML of the process.
   * 
   * @param configurationXML The process configuration XML
   */
  public final void setConfigurationXML(final String configurationXML) {
    this.configurationXML = configurationXML;
  }
  
  /**
   * @return The process configurationXML
   */
  public final String getConfigurationXML() {
    return this.configurationXML;
  }

  
  @Override
  public final String toString() {
    return ("Process Name: " + this.processName + ", Configuration: " + this.configurationXML );
  }
}
