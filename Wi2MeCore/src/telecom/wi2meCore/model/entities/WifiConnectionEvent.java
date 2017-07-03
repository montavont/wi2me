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
import telecom.wi2meCore.model.entities.WifiAP;


public class WifiConnectionEvent extends Trace{
	
	public static final String TABLE_NAME = "WifiConnectionEvent";
	public static final String EVENT = "event";
	
	private String event;
	private WifiAP connectionTo;

	protected WifiConnectionEvent(Trace trace, String event, WifiAP connectionTo){
		Trace.copy(trace, this);
		this.event = event;
		this.connectionTo = connectionTo;
	}
	
	public String getEvent() {
		return event;
	}

	public WifiAP getConnectionTo() {
		return connectionTo;
	}
	
	public static WifiConnectionEvent getNewWifiConnectionEvent(Trace trace, String event, WifiAP connectionTo)
	{
		WifiConnectionEvent retval = null;
		if (connectionTo != null)
		{
			retval = new WifiConnectionEvent(trace, event, connectionTo);
		}
		return retval;
	}
	
	private static final String AP_SEPARATOR = "-";
	
	public String toString(){
		return super.toString() + "WIFI_CONN_EVENT:" + event + AP_SEPARATOR + connectionTo.toString();
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.WIFI_CONNECTION_EVENT;
	}

}
