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
package cern.c2mon.server.cache.dbaccess.impl;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import cern.c2mon.server.common.config.ServerConstants;

/**
 * Manages the closing of the cache DB connections.
 * @author Mark Brightwell
 *
 */
@Service
@ManagedResource(objectName="cern.c2mon:type=datasource,name=cacheDbLifecycle")
public class CacheDbLifecycle implements SmartLifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheDbLifecycle.class);

  /**
   * The cache datasource to close down.
   */
  private DataSource cacheDataSource;

  /**
   * Only designed to
   */
  private volatile boolean started = false;

  @Autowired
  public CacheDbLifecycle(@Qualifier("cacheDataSource") DataSource cacheDataSource) {
    super();
    this.cacheDataSource = cacheDataSource;
  }


  /**
   * For management only.
   * @return the number of active DB connections in the cache datasource pool
   */
  @ManagedOperation(description="The number of active DB connections in the cache datasource pool (only works for Apache BasicDataSource)")
  public int getNumActiveDbConnections() {
    if (cacheDataSource instanceof BasicDataSource)
      return ((BasicDataSource) cacheDataSource).getNumActive();
    else
      return 0;
  }

  @Override
  public boolean isRunning() {
    return started;
  }

  /**
   * Nothing to start, as already done in datasource initialisation.
   */
  @Override
  public void start() {
    started = true;
  }

  @Override
  public void stop() {
    LOGGER.info("Closing down cache DB connections (only available for Apache BasicDataSource)");
    try {
      if (cacheDataSource instanceof BasicDataSource)
        ((BasicDataSource) cacheDataSource).close();
    } catch (SQLException ex) {
      LOGGER.error("Exception caught while closing down cache DB connections.", ex);
      ex.printStackTrace();
    }
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  /**
   * Smart lifecycle stop implementation.
   * Closes the DB connection pool.
   */
  @Override
  public void stop(Runnable arg0) {
    stop();
    arg0.run();
  }

  @Override
  public int getPhase() {
    return ServerConstants.PHASE_STOP_LAST - 1;
  }

}
