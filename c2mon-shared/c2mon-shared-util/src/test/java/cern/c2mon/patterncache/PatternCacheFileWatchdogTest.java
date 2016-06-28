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

package cern.c2mon.patterncache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

/**
 * J-unit test of the <code>PatternCacheFileWatchdog</class> class
 *
 * @author wbuczak
 */
public class PatternCacheFileWatchdogTest {

    private static final String TEST_CREDENTIALS_FILE = "testcredentials_03.txt";

    @Test
    public void testPatternCacheFileWatchdog() throws Exception {

        String filePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
            + System.getProperty("file.separator") + TEST_CREDENTIALS_FILE;
        System.out.println(filePath);

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, false)));
            out.println(".*abc   user1  pass1");
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PatternCache<UserCredentials> pcache = new PatternCache<UserCredentials>(UserCredentials.class);
        PatternCacheFileWatchdog<UserCredentials> fwatchdog = new PatternCacheFileWatchdog<UserCredentials>(pcache,
            filePath, 500);

        // this should match immediately 
        assertNotNull(pcache.findMatch("abc"));

        assertNull(pcache.findMatch("JMX:TEST01"));
        assertEquals(1, pcache.getSize());

        // start file watcher
        fwatchdog.start();

        Thread.sleep(1000);

        // append file
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
            out.println(".*JMX:TEST01.*   user5 pass5");
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread.sleep(2000);

        assertEquals(2, pcache.getSize());
        assertNotNull(pcache.findMatch("JMX:TEST01"));

        UserCredentials uc = pcache.findMatch("JMX:TEST01");

        assertEquals("user5", uc.getUserName());
        assertEquals("pass5", uc.getUserPasswd());

    }

}
