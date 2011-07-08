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
package cern.c2mon.client.core.tag;

import java.util.Collection;

import cern.c2mon.client.core.listener.DataTagUpdateListener;
import cern.c2mon.shared.client.supervision.SupervisionEvent;
import cern.c2mon.shared.client.tag.TransferTag;
import cern.tim.shared.common.datatag.TagQualityStatus;
import cern.tim.shared.rule.RuleFormatException;

/**
 * This interface extends the <code>ClientDataTagValue</code> interface 
 * and provides all methods which are necessary to update a
 * <code>ClientDataTag</code> object. This interface  shall only
 * be used within the C2MON client API. In other words no classes outside
 * of the C2MON client API should make directly changes on a
 * <code>ClientDataTag</code> object.
 *
 * @author Matthias Braeger
 */
public interface ClientDataTag extends ClientDataTagValue {

  /**
   * Invalidates the tag with {@link TagQualityStatus#INACCESSIBLE} and sets
   * the quality description to <code>pDescription</code>
   * Notifies all registered <code>DataTagUpdateListeners</code> of the change
   * of state.
   * @param pDescription the quality description
   */
  void invalidate(final String pDescription);

  /**
   * Adds a <code>DataTagUpdateListener</code> to the ClientDataTag and 
   * generates an initial update event for that listener.
   * Any change to the ClientDataTag value or quality attributes will trigger
   * an update event to all <code>DataTagUpdateListener</code> objects 
   * registered.
   * @param pListener the DataTagUpdateListener comments
   * @see #removeUpdateListener(DataTagUpdateListener)
   */
  void addUpdateListener(final DataTagUpdateListener pListener);

  /**
   * 
   * @return All listeners registered to this data tag
   */
  Collection<DataTagUpdateListener> getUpdateListeners();

  /**
   * Returns <code>true</code>, if the given listener is registered
   * for receiving updates of that tag.
   * @param pListener the listener to check
   * @return <code>true</code>, if the given listener is registered
   * for receiving updates of that tag.
   */
  boolean isUpdateListenerRegistered(DataTagUpdateListener pListener);

  /**
   * Removes (synchronized) a previously registered <code>DataTagUpdateListener</code>
   * @see #addUpdateListener
   * @param pListener The listener that shall be unregistered
   */
  void removeUpdateListener(final DataTagUpdateListener pListener);

  /**
   * Returns information whether the tag has any update listeners registered
   * or not
   * @return <code>true</code>, if this <code>ClientDataTag</code> instance has
   *         update listeners registered.
   */
  boolean hasUpdateListeners();

  
  /**
   * This thread safe method updates the given <code>ClientDataTag</code> object.
   * It copies every single field of the <code>TransferTag</code> object and notifies
   * then the registered listener about the update by providing a copy of the
   * <code>ClientDataTag</code> object.
   * 
   * @param transferTag The object that contains the updates.
   * @return <code>true</code>, if the update was successful, otherwise
   *         <code>false</code>
   * @throws RuleFormatException In case that the <code>TransferTag</code>
   *         parameter contains a invalid rule expression.
   */
  boolean update(final TransferTag transferTag) throws RuleFormatException;

  
  /**
   * This thread safe method updates the accessible state of the given
   * <code>ClientDataTag</code> object. Once the accessibility has been updated
   * it notifies the registered listener about the update by providing a copy of
   * the <code>ClientDataTag</code> object.
   * 
   * @param supervisionEvent The supervision event which contains the current
   *                         status of the process or the equipment.
   * @return <code>true</code>, if the update was successful. The returning value
   *         is <code>false</code>, if the supervision event is <code>null</code>
   *         or the tag is not linked to the given equipment or process.
   */
  boolean update(SupervisionEvent supervisionEvent);
  
  
  /**
   * Creates a clone of the this object. The only difference is that
   * it does not copy the registered listeners.
   * @return The clone of this object
   * @throws CloneNotSupportedException Thrown, if one of the field does not support cloning.
   */
  ClientDataTag clone() throws CloneNotSupportedException;
}
