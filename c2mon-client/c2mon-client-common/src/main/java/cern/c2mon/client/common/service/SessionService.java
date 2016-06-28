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
package cern.c2mon.client.common.service;

import java.util.Set;

import cern.c2mon.client.common.listener.SessionListener;
import cern.c2mon.shared.common.command.AuthorizationDetails;

/**
 * This interface describes the methods which are provided by
 * the C2MON session service singleton. The session service
 * handles the user authentication and allows registering
 * <code>SessionListener</code>.
 *
 * @author Matthias Braeger
 */
public interface SessionService {
  
  /**
   * Registers a listener for receiving event updates from the
   * <code>SessionService</code> about users that have logged
   * in or out. <p>
   * In case that somebody is already logged, the new listener will
   * directly be informed. This might for instance happen at startup
   * of the application when RBAC did performe an automatic
   * authentication by location before the listener has been registered. 
   * 
   * @param pListener The listener instance to register
   */
  void addSessionListener(final SessionListener pListener);

  /**
   * Removes the given listener from the <code>SessionManager</code>.
   * 
   * @param pListener The listener instance to remove
   */
  void removeSessionListener(final SessionListener pListener);

  /**
   * Use this message to authenticate with
   * a given user name and password. The {@link SessionManager} will then 
   * use your (valid) session for all authorization checks.
   * 
   * @param pUserName The user name
   * @param pPassword The password of the user
   * @return <code>true</code>, if the authentication was successful.
   */
  boolean login(final String pUserName, final String pPassword);
  
  /**
   * Use this message to authenticate a user for a given application name.
   * The {@link SessionManager} will then use your (valid) session for
   * all authorization checks.
   * 
   * @param appName The name of the application from which you are performing the
   *                authentication.
   * @param pUserName The user name
   * @param pPassword The password of the user
   * @return <code>true</code>, if the authentication was successful.
   */
  boolean login(final String appName, final String pUserName, final String pPassword);

  /**
   * Performs a user logout. Please notice that this method should only
   * be used in combination with the {@link #login(String, String, String)}
   * method provided by the {@link SessionService}. If a user did login
   * through the RBAC GUI tool bar it should also use this way to log out. 
   * @param userName The user to be logged out
   * @return <code>true</code>, if log out was unsuccessful
   */
  boolean logout(String userName);
  
  /**
   * Checks whether a given user is logged in.
   * @param userName The name of the user for which we want to check the
   *                 valid authentication.
   * @return <code>true</code>, if a user is logged.
   */
  boolean isUserLogged(String userName);
  
  /**
   * Checks whether there is at least one user logged
   * @return <code>true</code>, if at least one user is
   *         currently logged in.
   */
  boolean isAnyUserLogged();
  
  /**
   * @return The name of the users which are currently
   *         logged in.
   */
  Set<String> getLoggedUserNames();

  /**
   * Checks whether the logged person has the required authorization rights
   * as specified by the given {@link AuthorizationDetails}. Note that the
   * {@link AuthorizationDetails} are specific to the used authorization
   * method, e.g. RBAC. 
   * @param userName The name of the user for which we need to check the
   *                 privileges.
   * @param authorizationDetails The authorization details against which
   *        the user's privileges shall be checked.
   * @return <code>true</code>, if the user is has the authorized required
   *         privileges. 
   */
  boolean isAuthorized(String userName, AuthorizationDetails authorizationDetails);
}
