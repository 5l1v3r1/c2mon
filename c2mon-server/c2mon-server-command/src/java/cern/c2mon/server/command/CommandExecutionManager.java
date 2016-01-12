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
package cern.c2mon.server.command;

import java.util.Collection;

import cern.c2mon.shared.client.command.CommandExecuteRequest;
import cern.c2mon.shared.client.command.CommandReport;
import cern.c2mon.shared.client.command.CommandTagHandle;

/**
 * The Command module is responsible for authorising and executing
 * commands. It is called on an incoming client request to provide
 * the client with the CommandHandle, which can then be used to
 * execute commands. It also logs executed commands to the CommandTagLog
 * table.
 * 
 * <p>This is the interface that should be used by client modules to
 * interact with the Command module.
 * 
 * <p>For persisting the command tags to the DB a {@link CommandPersistenceListener}
 * must be registered with this bean (provided by STL optional module).
 * 
 * @author Mark Brightwell
 *
 */
public interface CommandExecutionManager {

  /**
   * Processes an incoming request for a collection of commands (coming
   * from the client). Returns the command details needed to execute it
   * from the client layer (including authorization details).
   * 
   * <p>If called with an empty collection, an empty collection is returned
   * (warning logged).
   * 
   * @param commandIds the ids of the requested commands
   * @param <T> the value type of the command
   * @return a collection of CommandTagHandles with the required information
   * @throws NullPointerException if called with null Collection
   */
  <T> Collection<CommandTagHandle<T>> processRequest(Collection<Long> commandIds);
  
  /**
   * Executes a command.
   * 
   * <p>Logs the command (including report) to the CommandTagLog table in
   * the STL schema.
   * 
   * @param commandExecuteRequest the request to execute a command
   * @param <T> the value type of the command
   * @return the report on the success/failure of the execution
   * @throws NullPointerException if passed a null CommandExecuteRequest
   */
  <T> CommandReport execute(CommandExecuteRequest<T> commandExecuteRequest);
  
  /**
   * Register the passed listener as responsible for command tag logging
   * and history queries.
   * 
   * @param commandPersistenceListener the listener to register
   * @throws NullPointerException if called with null parameter
   */
  void registerAsPersistenceListener(CommandPersistenceListener commandPersistenceListener);
}
