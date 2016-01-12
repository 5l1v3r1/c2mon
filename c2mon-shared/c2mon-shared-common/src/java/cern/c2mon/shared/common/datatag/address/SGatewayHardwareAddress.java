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
package cern.c2mon.shared.common.datatag.address;


/**
 * Hardware address interface used by the SGatewayMessageHandler.
 * 
 * @author J. Stowisek
 * @version $Revision: 1.2 $ ($Date: 2005/02/01 17:07:16 $ - $State: Exp $)
 */

public interface SGatewayHardwareAddress extends HardwareAddress {
  //---------------------------------------------------------------------------
  // Public constant definitions
  //---------------------------------------------------------------------------
  /**
   * Data format for BINary data.
   */
  public static final String FORMAT_BIN_DATA = "BIN_DATA";

  /**
   * Data format for TimeStamped data.
   */
  public static final String FORMAT_TS_DATA = "TS_DATA";

  /**
   * Tag type for data tags transporting equipment AVAilability information.
   * There are two types of tags transporting availaibility information: 
   * "alive" tags and "communication fault" (SUAV) tags.
   */
  public static final String TAG_TYPE_AVA = "AVA";

  /**
   * Tag type for CoMmanD tags.
   */
  public static final String TAG_TYPE_CMD = "CMD";

  /**
   * Tag type for "NoRMal" data tags.
   * Normal tags are all data tags that are neither commands nor availability 
   * information.
   */
  public static final String TAG_TYPE_NRM = "NRM";

  //---------------------------------------------------------------------------
  // Public member accessors
  //---------------------------------------------------------------------------
  /** 
   * Get the data format.
   * The data format can only be one of the values defined in the constants
   * FORMAT_BIN_DATA and FORMAT_TS_DATA.
   * @return the tag's data format.
   */
  public String getDataFormat();

  /**
   * Get a String representation of the default value for the data tag.
   * The default value can be either "TRUE" or "FALSE" for boolean tags or a 
   * number for analog tags.
   * @return the default value for the tag
   */
  public String getDefaultValue();

  /**
   * @return the physical minimum value
   */
  public float getPhysicalMinValue();

  /**
   * @return the physical maximum value
   */
  public float getPhysicalMaxValue();

  /**
   * @return the tag type
   */
  public String getTagType();

  /**
   * @return the network name of the PLC.
   */
  public String getPlcName();

  /**
   * @return the block id within the address space of the PLC.
   */
  public int getBlockId();

  /**
   * @return the word id within the data block
   */
  public int getWordId();
  
  /**
   * @return the offset within the word
   */
  public int getBitId();

  /**
   * @return the command pulse length in milliseconds
   */
  public int getCommandPulseLength();
  
}
