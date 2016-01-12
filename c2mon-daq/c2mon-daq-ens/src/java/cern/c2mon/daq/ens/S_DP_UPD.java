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
package cern.c2mon.daq.ens;

import java.nio.ByteBuffer;

/**
 * DP_UPD Structure
 * @author EFACEC
 *
 */
public class S_DP_UPD extends C_Efa_Structures {
	/**
	 * Constructor
	 *
	 */
	S_DP_UPD(){
		active++;
		//System.out.println("new S_DP_UPD = "+active);

		this.setTam(DEF_S_DP_UPD); 
		}
	
	/** Entity kidx */
	private int nKidx=0;
	/**
	 * Gets kidx
	 * @return kidx
	 */
	public final int getNKidx(){ return nKidx; }
	/**
	 * Sets kidx
	 * @param _value - kidx
	 */
	public final void setNKidx( int _value){ nKidx = _value; }

	/** entity type */
	private short sDpType=0;
	/**
	 * Gets entity type
	 * @return entity type
	 */
	public final short getSDpType(){ return sDpType; }
	/**
	 * Sets entity type
	 * @param _value - entity type
	 */
	public final void setSDpType(short _value){ sDpType = _value; }
	
	/** Data Value Structure */
	private S_DATA_VALUE sDpValue= new S_DATA_VALUE();
	/**
	 * Gets Data Value Structure
	 * @return Data Value Structure
	 */
	public final S_DATA_VALUE getSDpValue(){ return sDpValue; }
	/**
	 * Sets Data Value Structure
	 * @param _value - Data Value Structure
	 */
	public final void setSDpValue(S_DATA_VALUE _value){ sDpValue = _value; }

	/** Entity status */
	private int nStatus=0;
	/**
	 * Gets Entity status
	 * @return Entity status
	 */
	public final int getNStatus(){ return nStatus; }
	/**
	 * It will map ScateX status into GateX equivalent status
	 * @return GateX equivalent status
	 */
	public int getEquivalentStatus(){ 
		if ((nStatus & (1<<0)) != 0){	// auto offscan
			return CEfaEntity.EFAENT_AOFFSCAN;
		}
		if ((nStatus & (1<<1)) != 0){	// manual offscan
			return CEfaEntity.EFAENT_MOFFSCAN;
		}
		if ((nStatus & (1<<2)) != 0){	// invalido
			return CEfaEntity.EFAENT_IMPOSED;
		}
		if ((nStatus & (1<<4)) != 0){	// imposto
			return CEfaEntity.EFAENT_IMPOSED;
		}
		if ((nStatus & (1<<5)) != 0){	// tagged
			return CEfaEntity.EFAENT_UNINITIALISED;
		}
		
		return CEfaEntity.EFAENT_VALID;
	}
	/**
	 * Sets Status
	 * @param _value - entity status
	 */
	public final void setNStatus( int _value){ nStatus = _value; }

	/** Entity event attribute */
	private S_EVENT_ATTR sEvTtag = new S_EVENT_ATTR(); 
	/**
	 * Gets Entity event attribute 
	 * @return Entity event attribute 
	 */
	public final S_EVENT_ATTR getSEvTtag(){ return sEvTtag; }
	/**
	 * Sets Entity event attribute 
	 * @param _value - Entity event attribute 
	 */
	public final void setSEvTtag( S_EVENT_ATTR _value ){ sEvTtag = _value; }
	/**
	 * Fills buffer with all data to send to ScateX
	 */
	public void getSerialized(ByteBuffer msg)
	{
		msg.putInt(nKidx);
		msg.putShort(sDpType);
		sDpValue.getSerialized(msg);
		msg.putInt(nStatus);
		sEvTtag.getSerialized(msg);
	}
	/**
	 * Retrieve data from buffer into variables
	 */
	public boolean enqueue(ByteBuffer msg)
	{
		try {
			nKidx = msg.getInt();
			sDpType = msg.getShort();
			if (sDpType == 5){	// digital 
				sDpValue.bIntValue=true;	
			}
			else{				
				sDpValue.bIntValue=false;
			}
			
			if (sDpValue.enqueue(msg)==false){
				return false;
			}
			
			nStatus = msg.getInt();
			if (sEvTtag.enqueue(msg) == false){
				return false;
			}
			
			return true;
		} catch (Exception e) {
			
			return false;
		}
	}
	
	/**
	 * implementation of memory destructor
	 *
	 */
	public void myDestruct()
	{
//		 free mem
		if(sDpValue!=null){
			sDpValue.myDestruct();
		}
		sDpValue = null;
		if(sEvTtag!=null){
			sEvTtag.myDestruct();
		}
		sEvTtag = null;
	}
	/**
	 * finalize method
	 */
	protected void finalize(){
		myDestruct();
		active--;
		try { super.finalize(); } catch (Throwable e) { e.printStackTrace(); }
	}
}
