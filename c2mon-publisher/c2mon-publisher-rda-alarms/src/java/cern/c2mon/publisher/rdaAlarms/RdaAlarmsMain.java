/**
 * Copyright (c) 2015 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.c2mon.publisher.rdaAlarms;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class RdaAlarmsMain extends Thread {

    private static Logger log; 
    private static RdaAlarmsPublisher client;
    
    public static void main(String[] args)
    {
        String log4jConfigFile = System.getProperty("log4j.configuration", "log4j-diamon.properties");
        PropertyConfigurator.configureAndWatch(log4jConfigFile, 60 * 1000);
        log = LoggerFactory.getLogger(RdaAlarmsMain.class);
        log.info("Logging system ready ({})...", log4jConfigFile);

        Runtime.getRuntime().addShutdownHook(new RdaAlarmsMain());
        FileSystemXmlApplicationContext context = null;
        try
        {
             context = new FileSystemXmlApplicationContext(System.getProperty("context",
                        "classpath:cern/c2mon/publisher/rdaAlarms/alarms_publisher.xml"));
                          
             client = context.getBean("publisher", RdaAlarmsPublisher.class);
             client.start();
             client.join();                                                  
        }
        catch (Exception e)
        {          
            e.printStackTrace();
            System.exit(1);
        }
        finally
        {
            if (context != null) {
                context.close();
            }
        }
        log.info("Halt.");
        System.exit(0);
    }

    //
    // --- Implements Runnable -----------------------------------------------------------
    //
    /**
     * This is the method called when ArchiverMain is declared to be its own shutdown
     * hook. In the deployed version, this will be activated in case of ctrl-c or
     * wreboot of the process (will NOT work from within Eclipse!).
     */
    @Override
    public void run()
    {
        log.info("Going down ...");
        if (client != null) {
            client.shutdown();        
        }
    }

}
