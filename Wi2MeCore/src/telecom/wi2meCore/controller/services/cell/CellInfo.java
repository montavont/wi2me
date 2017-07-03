/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meCore.controller.services.cell;

import java.util.List;

import android.telephony.NeighboringCellInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class CellInfo{
	
	private static final String GSM = "GSM";
	private static final String CDMA = "CDMA";
	private static final String NONE = "NONE";
	private static final String NOT_AVAILABLE = "NA";
	private static final String GPRS = "GPRS";
	private static final String EDGE = "EDGE";
	private static final String UMTS = "UMTS";
	private static final String EDV0_0 = "EDV0_0";
	private static final String EDV0_A = "EDV0_A";
	private static final String ONExRTT = "1xRTT";
	private static final String HSUPA = "HSUPA";
	private static final String HSDPA = "HSDPA";
	private static final String HSPA = "HSPA";
	private static final String CELL_INFO = " Operator Name: %s \n Operator: %s \n CID: %d \n LAC: %d \n Type: %s \n Signal(dBm): %d \n Phone Type: %s \n Neighbours: %d";
	
	public String operatorName;		
	public String networkType;
	public String phoneType;
	public String operator;
	public int cid;
	public int lac;
	private int leveldBm;
	public List<NeighboringCellInfo> neighbors;
	private ServiceState serviceState;
	private boolean changed;
	
	/**
	 * Constructor. Sets the parameters to 0.
	 */
	public CellInfo(){
		operator = "0";
		cid = 0;
		lac = 0;
		changed = false;
	}
	
	/**
	 * Says whether the cell has changed or not.
	 * @return boolean
	 */
	public boolean isChanged() {
		return changed;
	}
	
	/**
	 * Sets the "changed" parameter.
	 * @param changed
	 */
	public void setChanged(boolean changed){
		this.changed = changed;
	}

	/**
	 * Gives the received level in dBm
	 * @return int
	 */
	public int getLeveldBm() {
		return leveldBm;
	}
	
	/**
	 * Sets the serviceState
	 * @param serviceState
	 */
	public synchronized void setServiceState(ServiceState serviceState){
		this.serviceState = serviceState;
	}

	/**
	 * Sets the level (in dBm)
	 * @param level
	 */
	public synchronized void setLevel(int level) {
		this.leveldBm = level;
		this.changed = true;
	}
	
	public String toString(){
		return String.format(CELL_INFO, operatorName, operator, cid, lac, networkType, leveldBm, phoneType, neighbors.size());
	}
	
	/**
	 * Get a copy of the object with its last changes 
	 * @return The copy of the object
	 */
	public synchronized CellInfo getCopyOfCurrentCell(){
		CellInfo copy = new CellInfo();
		copy(copy);
		//changed = false;
		return copy;
	}
	
	/**
	 * Updates the parameters of this CellInfo..
	 * @param telephonyManager
	 */
	public synchronized void update(TelephonyManager telephonyManager){	
			operatorName = telephonyManager.getNetworkOperatorName();			
			networkType = getNetworkType(telephonyManager.getNetworkType());
			phoneType = getPhoneType(telephonyManager.getPhoneType());
			neighbors = telephonyManager.getNeighboringCellInfo();
			
			if (telephonyManager.hasIccCard())
			{
				if (phoneType.equals(GSM))
				{
					GsmCellLocation location = (GsmCellLocation) telephonyManager.getCellLocation();
					operator = telephonyManager.getNetworkOperator();
				
					if (location != null)
					{
						cid = location.getCid();
						lac = location.getLac();
	
						Log.d(getClass().getSimpleName(), "CID "+cid+" LAC "+lac);				
					}
				}
				else
				{
					if (phoneType.equals(CDMA))
					{
						CdmaCellLocation location = (CdmaCellLocation) telephonyManager.getCellLocation();
						operator = serviceState.getOperatorNumeric() + location.getSystemId();
						cid = location.getBaseStationId();
						lac = location.getNetworkId();
						Log.d(getClass().getSimpleName(), "CID "+cid+" LAC "+lac);				
					}
				}
			} else
			{
				operator = "null";
				cid=0;
				lac=0;
			}
			
			changed = true;
	}
	
	/**
	 * Copies the current cellInfo into a new one.
	 * @param copy
	 * @return The copied cellInfo
	 */
	public CellInfo copy(CellInfo copy){
			copy.operatorName = operatorName;
			copy.operator = operator;
			copy.networkType = networkType;
			copy.phoneType = phoneType;
			copy.cid = cid;
			copy.lac = lac;
			copy.leveldBm = leveldBm;
			copy.neighbors = neighbors;
			copy.changed = changed;
			return copy;
	}
		
	/**
	 * Gives the phone type (GSM, CDMA)
	 * @param type
	 * @return The type
	 */
	public static String getPhoneType(int type) {
		switch (type){
		case TelephonyManager.PHONE_TYPE_GSM:
			return GSM;
		case TelephonyManager.PHONE_TYPE_CDMA:
			return CDMA;
		case TelephonyManager.PHONE_TYPE_NONE:
			return NONE;
		}
		return NONE;
	}
	
	/**
	 * Gives the network type (GPRS, EDGE, etc.)
	 * @param atype
	 * @return The network type
	 */
	public static String getNetworkType(int atype) {
		switch(atype) {
		case TelephonyManager.NETWORK_TYPE_UNKNOWN :
			return NOT_AVAILABLE;
		case TelephonyManager.NETWORK_TYPE_GPRS:
			return GPRS;
		case TelephonyManager.NETWORK_TYPE_EDGE:
			return EDGE;
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return UMTS;
		case TelephonyManager.NETWORK_TYPE_CDMA:
			return CDMA;
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			return EDV0_0;
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			return EDV0_A;
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			return ONExRTT;
			/* Use numbers for backward compatibility */
		case 8:
			return HSDPA;
		case 9:
			return HSUPA;
		case 10:
			return HSPA;
		}
		return NOT_AVAILABLE;

	}

	/**
	 * Calculates the level in dBm.
	 * @param gsmSignalStrength
	 * @return The level in dBm
	 */
	public static int getLeveldBm(int gsmSignalStrength) {
		return -113 + 2 * gsmSignalStrength;
	}
	
	/**
	 * Returns a new object, class CellInfo with the identifier attributes in 0. This is called the null object of type CellInfo
	 * @return The null object cell info
	 */
	/*
	public static CellInfo getNullCellInfo(){
		CellInfo ret = new CellInfo();
		ret.operator = "0";
		ret.cid = 0;
		ret.lac = 0;
		return ret;
	}
	*/
	/**
	 * Compares 2 CellInfo
	 * @param otherCellInfo
	 * @return Whether they are equal or not (boolean).
	 */
	public boolean equals(Object otherCellInfo){
		CellInfo other = (CellInfo) otherCellInfo;		
		return this.cid == other.cid && this.lac == other.lac && this.operator.equals(other.operator);
	}
}
