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
package cern.c2mon.client.ext.rbac.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cern.accsoft.security.rba.RBAToken;
import cern.accsoft.security.rba.TokenExpiredException;
import cern.accsoft.security.rba.TokenFormatException;
import cern.accsoft.security.rba.login.DefaultCallbackHandler;
import cern.accsoft.security.rba.login.LoginPolicy;
import cern.accsoft.security.rba.login.RBALoginContext;
import cern.c2mon.client.ext.rbac.AuthenticationListener;
import cern.c2mon.client.ext.rbac.AuthenticationManager;
import cern.rba.util.holder.ClientTierRbaTokenChangeListener;
import cern.rba.util.holder.ClientTierSubjectHolder;

/**
 * The RBAC implementation of the {@link AuthenticationManager}.
 *
 * @author Matthias Braeger
 */
@Service
public class RbacAuthenticationManager implements AuthenticationManager, RbaTokenManager, ClientTierRbaTokenChangeListener {
  
  /** Log4j logger instance */
  private static final Logger LOG = LoggerFactory.getLogger(RbacAuthenticationManager.class);
  
  /** Used to synchronize login and logout requests */
  private static final Object SYNC_LOCK = new Object(); 
  
  /**
   * This Map keeps a reference to the RBALoginContext for each authenticated user (key)
   */
  private final Map<String, RBALoginContext> userContexts = new ConcurrentHashMap<String, RBALoginContext>();
  
  /**
   * This variable points to the {@link RBAToken} that is provided by a
   * user login through the RBAC GUI. Since RBAC allows only one user being
   * logged at the same time we do not need a Map for this.
   */
  private RBAToken rbaGUILoginToken = null;
  
  /** The list of registered {@link AuthenticationListener} instances */
  private Set<AuthenticationListener> authenticationListeners =
    Collections.synchronizedSet(new HashSet<AuthenticationListener>());

  /**
   * Registering this class as {@link ClientTierRbaTokenChangeListener}
   */
  @PostConstruct
  protected void init() {
    ClientTierSubjectHolder.addRbaTokenChangeListener(this);
  }
  
  /**
   * This method is called by SPRING before this Service gets destroyed 
   */
  @PreDestroy
  protected void cleanup() {
    authenticationListeners.clear();
    userContexts.clear();
  }
  
  /**
   * To check, if the user is logged, we have to have
   * a valid token from the user.
   * @param userName The name of the user
   */
  @Override
  public boolean isUserLogged(final String userName) {
    RBAToken token;
    boolean tokenValid = false;
    if (userName != null) {
      RBALoginContext ctx = userContexts.get(userName); 
      if (ctx != null) {
        token  = ctx.getRBASubject().getAppToken();
        if (token != null) {
          tokenValid = token.isValid();
        }
      }
      else {
        if (rbaGUILoginToken != null && rbaGUILoginToken.getUser().getName().equalsIgnoreCase(userName)) {
          tokenValid = rbaGUILoginToken.isValid();
        }
      }
    }
    
    return tokenValid;
  }

  /**
   * Returns all user names which for which the <code>AuthenticationManager</code>
   * has a registered context. Additionally it checks with an <code>RbaTokenLookup</code>
   * whether someone has logged in through the RBAC GUI tool bar.
   * 
   * @return The list of logged users
   */
  @Override
  public Set<String> getLoggedUserNames() {
    Set<String> userNames = new HashSet<String>(userContexts.keySet());
    if (rbaGUILoginToken != null) {
      userNames.add(rbaGUILoginToken.getUser().getName());
    }
    
    return userNames;
  }

  /**
   * Executes a RBAC user login with the default application name
   * "c2mon-client-api"
   * @param userName The user name
   * @param userPassw The password of the user
   * @return <code>true</code>, if the authentication
   *         was successfull
   */
  @Override
  public boolean login(final String userName, final String userPassw) {
    return login("c2mon-client-api", userName, userPassw);
  }
  
  @Override
  public boolean login(final String appName, final String userName, final String userPassw) {
    synchronized (SYNC_LOCK) {
      boolean validToken = false;

      try {
        RBALoginContext ctx = new RBALoginContext(LoginPolicy.EXPLICIT,
            new DefaultCallbackHandler(appName, userName, userPassw.toCharArray()));
        userContexts.put(userName, ctx);
        ctx.login();
        validToken = ctx.getRBASubject().getAppToken().isValid();
      }
      catch (LoginException e) {
        LOG.info("login() - Login attempt for user " + userName + " failed. Reason: " + e.getMessage());
      }
      
      return validToken;
    }
  }

  @Override
  public boolean logout(final String userName) {
    synchronized (SYNC_LOCK) {
      RBALoginContext ctx = userContexts.get(userName);
      if (ctx != null) {
        try {
          ctx.logout();
          ClientTierSubjectHolder.clear();
          userContexts.remove(userName);
        }
        catch (LoginException e) {
          LOG.warn("User logout was unsuccessul. Reason: " + e.getMessage());
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void addAuthenticationListener(final AuthenticationListener listener) {
    if (listener != null && !authenticationListeners.contains(listener)) {
      authenticationListeners.add(listener);
    }
  }

  @Override
  public void removeAuthenticationListener(final AuthenticationListener listener) {
    if (listener != null) {
      authenticationListeners.remove(listener);
    }
  }

  /**
   * This listener method is called every time, when somebody is logging
   * in or out. The SessionManager will accordingly inform all registered
   * {@link SessionListener}.
   * 
   * @param rbaToken The RBAC token
   * @throws TokenFormatException In case of problems with the token.
   * @throws TokenExpiredException In case the token is already expired
   */
  @Override
  public void rbaTokenChanged(final RBAToken rbaToken) throws TokenFormatException, TokenExpiredException {
    if (rbaToken != null) {
      String userName = rbaToken.getUser().getName();
      if (rbaToken.isEmpty() || !rbaToken.isValid()) {
        LOG.info("User " + userName + " has logged out.");
        // Remove the context, if not yet done
        userContexts.remove(userName);
        
        // remove GUI login token reference, if it is from the same user  
        if (rbaGUILoginToken != null && rbaGUILoginToken.getUser().getName().equalsIgnoreCase(userName)) {
          rbaGUILoginToken = null;
        }
        
        // the user has logged out
        for (AuthenticationListener listener : authenticationListeners) {
          listener.onLogout(userName);
        }
      }
      else {
        boolean onRbaGUITokenRenew = false;
        
        if (userContexts.get(userName) == null) {
          // if we had already a RBA GUI login token, then we do not 
          // need to inform again the registered listeners about it.
          onRbaGUITokenRenew = rbaGUILoginToken != null;
          
          rbaGUILoginToken = rbaToken;
        }
        
        if (!onRbaGUITokenRenew) {
          LOG.info("User " + userName + " has successfully logged in.");
          for (AuthenticationListener listener : authenticationListeners) {
            listener.onLogin(userName);
          }
        }
      }
    }
    else if (rbaGUILoginToken != null) {
      LOG.debug("rbaTokenChanged() - Received logout notification from the RBAC toolbar.");
      String userName = rbaGUILoginToken.getUser().getName();
      // remove GUI login token reference
      rbaGUILoginToken = null;
      // the user has logged out
      for (AuthenticationListener listener : authenticationListeners) {
        listener.onLogout(userName);
      }
    }
    else {
      LOG.debug("rbaTokenChanged() - Unsuccessful login attempt. Nobody got informed.");
    }
  }

  @Override
  public RBAToken findRbaToken(final String userName) {
    RBAToken token = null;
    RBALoginContext ctx = userContexts.get(userName); 
    if (ctx != null) {
      token  = ctx.getRBASubject().getAppToken();
    }
    else {
      if (rbaGUILoginToken != null && rbaGUILoginToken.getUser().getName().equalsIgnoreCase(userName)) {
        token = rbaGUILoginToken;
      }
    }
    
    return token;
  }
}
