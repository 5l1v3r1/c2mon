/*
 * Copyright CERN 2011, All Rights Reserved.
 */
package cern.c2mon.daq.jmx.mbeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wbuczak
 */
public class Cache extends NotificationBroadcasterSupport implements CacheMBean {

    private int cacheSize = DEFAULT_CACHE_SIZE;
    private int cachedObjects = 0;

    private List<String> cacheListeners = new ArrayList<String>();

    private List<String> values = new ArrayList<String>(10);

    private static final int DEFAULT_CACHE_SIZE = 200;

    static Logger log = LoggerFactory.getLogger(Cache.class);

    Map<String, Integer> testMap = new HashMap<String, Integer>();
    
    private long sequenceNumber = 1;

    // instance init block. put some "listeners" for tests
    {

        cacheListeners.add("a");
        cacheListeners.add("b");
        
        testMap.put("CATEGORY1/process1/metric1", 123);
        testMap.put("CATEGORY1/process2/metric1", 128);
        
        for (int i=0; i< 10; i++ ) {
            values.add("" + i);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.cern.tim.driver.jmx.CacheMBean#getCacheSize()
     */
    @Override
    public synchronized int getCacheSize() {
        return this.cacheSize;
    }

    @Override
    public synchronized long getCurrentSrvTime() {
        return System.currentTimeMillis();
    }

    @Override
    public synchronized void setCachedObjects(int objects) {
        this.cachedObjects = objects;
    }

    @Override
    public synchronized int getCachedObjects() {
        return this.cachedObjects;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.cern.tim.driver.jmx.CacheMBean#setCacheSize(int)
     */
    @Override
    public synchronized void setCacheSize(int size) {
        int oldSize = this.cacheSize;
        this.cacheSize = size;

        if (log.isDebugEnabled())
            log.debug("Cache size now " + this.cacheSize);

        Notification n = new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                "CacheSize changed", "CacheSize", "int", oldSize, this.cacheSize);

        sendNotification(n);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] { info };
    }

    @Override
    public String test1() {
        return "test string 1";
    }

    @Override
    public String test2(boolean condition) {
        if (condition)
            return "test string 2: true";
        return "test string 2: false";
    }

    @Override
    public String test2(Integer val) {
        if (val == 0)
            return "A";
        return "B";
    }

    @Override
    public List<String> getCacheListeners() {
        return this.cacheListeners;
    }

    @Override
    public List<String> getValues() {
        return this.values;
    }
    
    @Override
    public void setValues(List<String> newVals) {
        this.values = newVals;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.cern.tim.driver.jmx.CacheMBean#getTestMap()
     */
    @Override
    public Map<String, Integer> getTestMap() {
        return testMap;
    }
    
    @Override
    public void setTestMap(Map<String,Integer> newElements) {
        testMap = newElements;
    }
}
