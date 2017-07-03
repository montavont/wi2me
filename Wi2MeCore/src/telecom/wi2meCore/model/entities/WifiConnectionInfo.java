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

package telecom.wi2meCore.model.entities;

import telecom.wi2meCore.controller.services.persistance.DatabaseHelper.TraceType;

public class WifiConnectionInfo extends Trace{

	public static final String TABLE_NAME = "WifiConnectionInfo";
	public static final String SNIFF_SEQUENCE = "SniffSequence";
	
	private long sniffSequence;
	private ConnectionInfo connectionInfo;

	protected WifiConnectionInfo(Trace trace, long sniffSequence, ConnectionInfo connectionInfo){
		Trace.copy(trace, this);
		this.sniffSequence = sniffSequence;
		this.connectionInfo = connectionInfo;
	}
	
	public ConnectionInfo getConnectionInfo(){
		return connectionInfo;
	}
	
	public long getSniffSequence() {
		return sniffSequence;
	}
	
	public static WifiConnectionInfo getNewWifiConnectionInfo(Trace trace, long sniffSequence, ConnectionInfo connectionInfo){
		return new WifiConnectionInfo(trace, sniffSequence, connectionInfo);
	}
	
	private static final String SEPARATOR = "-";
	
	public String toString(){
		return super.toString() + "WIFI_CONNECTION_INFO:" + connectionInfo.toString() + SEPARATOR + sniffSequence;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.WIFI_CONNECTION_INFO;
	}

}
