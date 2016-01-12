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
package cern.c2mon.notification.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.junit.Test;

import cern.c2mon.notification.shared.Subscription;
import cern.dmn2.core.Status;

public class SubscriptionTest {

    @Test
    public void testIsInterestedInLevel() {
        Subscription s = new Subscription("test", 1L);
        
        assertTrue("isInterestedInLevel() reports " + s.isInterestedInLevel(Status.OK), s.isInterestedInLevel(Status.OK));
        assertTrue("isInterestedInLevel() reports " + s.isInterestedInLevel(Status.WARNING), s.isInterestedInLevel(Status.WARNING));
        assertTrue("isInterestedInLevel() reports " + s.isInterestedInLevel(Status.ERROR), s.isInterestedInLevel(Status.ERROR));

        s.setNotificationLevel(Status.ERROR);
        assertFalse("isInterestedInLevel() reports " + s.isInterestedInLevel(Status.WARNING), s.isInterestedInLevel(Status.WARNING));
        
    }
    
    @Test
    public void testCopyMethod() {
        Subscription s = new Subscription("test", 1L);
        Timestamp tBefore = new Timestamp(System.currentTimeMillis());
        Status sBefore = Status.WARNING;
        Long childBefore = 2L;
        
        /*
         * create a subscription with some values
         */
        s.setEnabled(true);
        s.setLastNotification(tBefore);
        s.setLastNotifiedStatus(sBefore);
        s.addResolvedSubTag(childBefore);
        s.setLastStatusForResolvedTSubTag(childBefore, sBefore);
        s.setMailNotification(false);
        s.setNotificationLevel(Status.ERROR);
        s.setNotifyOnMetricChange(true);
        s.setSmsNotification(true);
        
        /*
         * make a copy of it and change all data of the original to finally compare the fields 
         */
        Subscription after = s.getCopy();
        s.setEnabled(false);
        s.setLastNotification(new Timestamp(System.currentTimeMillis()));
        s.setLastNotifiedStatus(Status.ERROR);
        s.setLastStatusForResolvedTSubTag(childBefore, Status.ERROR);
        s.setMailNotification(true);
        s.setNotificationLevel(Status.WARNING);
        s.setNotifyOnMetricChange(false);
        s.setSmsNotification(false);
        
        
        /*
         * nothing should have changed to the first subscription object
         */
        assertTrue(after.isEnabled());
        assertTrue(after.getLastNotification().equals(tBefore));
        assertTrue(after.getLastNotifiedStatus().equals(sBefore));
        assertTrue(after.getLastStatusForResolvedSubTag(childBefore).equals(sBefore));
        assertFalse(after.isMailNotification());
        assertTrue(after.getNotificationLevel().equals(Status.ERROR));
        assertTrue(after.isNotifyOnMetricChange());
        assertTrue(after.isSmsNotification());
    }
}
