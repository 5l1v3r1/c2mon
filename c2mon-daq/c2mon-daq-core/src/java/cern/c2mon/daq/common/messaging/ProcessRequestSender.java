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
package cern.c2mon.daq.common.messaging;

import org.w3c.dom.Document;

import cern.c2mon.shared.daq.process.ProcessConfigurationResponse;
import cern.c2mon.shared.daq.process.ProcessConnectionResponse;

/**
 * The interface that must be implemented by the class responsible for requesting
 * and receiving the XML connection, configuration and disconnection procedures
 * 
 * @author mbrightw
 * @author vilches (refactoring updates)
 *
 */
public interface ProcessRequestSender {

  /**
   * This method publishes the ProcessConfigurationRequest message on the configured topic. 
   * As a result it expects to receive the ProcessConfigurationResponse message. 
   * 
   * If the server does not reply within the specified timeout, null is returned.
   * 
   * @return the ProcessConfigurationResponse object
   */
  ProcessConfigurationResponse sendProcessConfigurationRequest();

  /**
   * This method publishes the unique Process Id Key (PIK) message on the configured topic. 
   * As a result it expects to receive the Process Configuration Response message. 
   * 
   * If the server does not reply within the specified timeout, null is returned.
   * 
   * @return the parsed XML processConnectionResponse object
   */
  ProcessConnectionResponse sendProcessConnectionRequest();

  /**
   *  Send a ProcessDisconnectionRequest to the server. No reply is expected.
   */
  void sendProcessDisconnectionRequest();
}
