/*******************************************************************************
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
 ******************************************************************************/
package cern.c2mon.daq;

import java.io.IOException;

import cern.c2mon.daq.common.DriverKernel;
import cern.c2mon.daq.config.DaqCoreModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.AbstractEnvironment;

import static com.sun.tools.internal.jxc.SchemaGenerator.run;
import static java.lang.String.format;
import static java.lang.System.getProperty;

/**
 * This class is responsible for bootstrapping a C2MON DAQ process.
 *
 * @author Justin Lewis Salmon
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Import({
    DaqCoreModule.class
})
@Slf4j
public class DaqStartup {

  private static SpringApplication APPLICATION = null;
  private static ConfigurableApplicationContext CONTEXT = null;

  public static void main(String[] args) throws IOException {
    start(args);
  }

  public static void start(String[] args) throws IOException {
    String daqName = getProperty("c2mon.daq.name");
    if (daqName == null) {
      throw new RuntimeException("Please specify the DAQ process name using 'c2mon.daq.name'");
    }

    // The JMS mode (single, double, test) is controlled via Spring profiles
    String mode = getProperty("c2mon.daq.jms.mode");
    if (mode != null) {
      System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, mode);
    }

    if (APPLICATION == null) {
      APPLICATION = new SpringApplicationBuilder(DaqStartup.class)
              .bannerMode(Banner.Mode.OFF)
              .build();
    }
    CONTEXT = APPLICATION.run(args);


    DriverKernel kernel = CONTEXT.getBean(DriverKernel.class);
    kernel.init();

    log.info("DAQ core is now initialized");
  }

  public static void stop() {
    DriverKernel kernel = CONTEXT.getBean(DriverKernel.class);
    kernel.shutdown();
    CONTEXT.close();
  }

}
