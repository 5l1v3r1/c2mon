/*******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 * 
 * Copyright (C) 2004 - 2011 CERN. This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with this program; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 * 
 * Author: TIM team, tim.support@cern.ch
 ******************************************************************************/
package cern.c2mon.client.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class is the main facade for all applications using the
 * C2MON client API.
 * <p>
 * The C2MON service gateway provides access to the different
 * C2MON manager singleton instances. A client application should
 * only use functionality which are provided by these classes.
 *
 * @author Matthias Braeger
 */
public final class C2monServiceGateway {
  
  /** Static reference to the <code>C2monTagManager</code> singleton instance */
  private static C2monTagManager tagManager = null;
  
  /** Static reference to the <code>C2monSessionManager</code> singleton instance */
  private static C2monSessionManager sessionManager = null;
  
  /** Static reference to the <code>C2monSupervisionManager</code> singleton instance */
  private static C2monSupervisionManager heartbeatManager = null;
 
  
  /**
   * Hidden constructor
   */
  private C2monServiceGateway() {
    // Do nothing
  }

  
  /**
   * @return The C2MON tag manager, which is managing
   *         the tag subscribtion and unsubscription.
   */
  public static C2monTagManager getTagManager() {
    return tagManager;
  }


  /**
   * @return the sessionManager
   */
  public static C2monSessionManager getSessionManager() {
    return sessionManager;
  }

  /**
   * @return the heartbeatManager
   */
  public static C2monSupervisionManager getHeartbeatManager() {
    return heartbeatManager;
  }


  /**
   * The lifecycle of this inner class is managed by the Spring
   * context. It's purpose is to set the static fields of the
   * gateway.
   *
   * @author Matthias Braeger
   */
  @Service
  private class SpringGatewayInitializer {
    
    /**
     * Default Constructor used by the Spring container
     * @param pTagManager The tag manager singleton 
     * @param pSessionManager The session manager singleton
     * @param pHeartbeatManager The heartbeat singleton
     */
    @Autowired
    private SpringGatewayInitializer(
        final C2monTagManager pTagManager,
        final C2monSessionManager pSessionManager,
        final C2monSupervisionManager pHeartbeatManager) {
      
      tagManager = pTagManager;
      sessionManager = pSessionManager;
      heartbeatManager = pHeartbeatManager;
    }
  }
}
