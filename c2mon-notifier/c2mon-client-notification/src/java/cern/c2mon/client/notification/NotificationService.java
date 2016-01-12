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
package cern.c2mon.client.notification;

import cern.c2mon.notification.shared.ServiceException;
import cern.c2mon.notification.shared.Subscriber;
import cern.c2mon.notification.shared.Subscription;
import cern.c2mon.notification.shared.UserNotFoundException;

/**
 * @author felixehm
 *
 */
public interface NotificationService {

	/**
	 * 
	 * @param sub the {@link Subscriber} to set.
     * @throws ServiceException in case of a problem while using the service.
	 */
	public Subscriber setSubscriber(Subscriber sub) throws ServiceException;
	
	/**
	 * Adds a new notification user to the system.
	 * 
	 * @param sub the {@link Subscriber} object representing the information on this user.
	 * @throws ServiceException in case of a problem with the notification service.
	 */
	public void addSubscriber(Subscriber sub) throws ServiceException;
	
	/** Subscribes to the tagId for notification.<br><br>
	 * 
	 * SMS will not be enabled by default.<br>
	 * WARING level is the threshold<br>
	 * 
	 * @param userId the id of the user
	 * @param tagId the tagId to subscribe to.
	 * @throws UserNotFoundException in case the user with userId was not found.
     * @throws ServiceException in case of a problem while using the service.
	 */
	public Subscriber subscribe(String userId, Long tagId) throws UserNotFoundException, ServiceException;
	
	/**
	 * @param userName the unique identifier of the user. 
	 * @return the {@link Subscriber} object with all its {@link Subscription} objects. 
	 * @throws UserNotFoundException in case the user cannot be found
     * @throws ServiceException in case of a problem while using the service.
	 */
	public Subscriber getSubscriber(String userName) throws UserNotFoundException, ServiceException;
	
	/**
	 * 
	 * @param user the unique identifier of the user. 
	 * @param tagId the data tag id [long]
	 * @return true, in case the user is subscribed to this data tag.
	 * @throws UserNotFoundException in case the user can't be found.
     * @throws ServiceException in case of a problem while using the service.
	 */
	public boolean isSubscribed(String user, Long tagId) throws UserNotFoundException, ServiceException;

	/**
	 * @param user the unique identifier of the user. 
     * @param tagId the data tag id [long]
	 * @throws UserNotFoundException in case the associated user cannot be found
     * @throws ServiceException in case of a problem while using the service.
	 */
	public void removeSubscription(String user, Long tagId) throws UserNotFoundException, ServiceException;
	
}
