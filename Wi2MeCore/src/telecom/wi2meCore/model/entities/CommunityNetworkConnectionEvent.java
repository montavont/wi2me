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


public class CommunityNetworkConnectionEvent extends Trace{	

	public static final String TABLE_NAME = "CommunityNetworkConnectionEvent";
	public static final String EVENT = "event";
	public static final String USERNAME = "username";
	
	private String event;
	private WifiAP connectedTo;
	private String username;

	protected CommunityNetworkConnectionEvent(Trace trace, String event, WifiAP connectedTo, String username){
		Trace.copy(trace, this);
		this.event = event;
		this.connectedTo = connectedTo;
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getEvent() {
		return event;
	}

	public WifiAP getConnectedTo() {
		return connectedTo;
	}
	
	public static CommunityNetworkConnectionEvent getNewCommunityNetworkConnectionEvent(Trace trace, String event, WifiAP connectionTo, String username){
		return new CommunityNetworkConnectionEvent(trace, event, connectionTo, username);
	}
	
	private static final String AP_SEPARATOR = "-";
	
	public String toString(){
		return super.toString() + "CN_CONN_EVENT:" + event + AP_SEPARATOR + username + AP_SEPARATOR + connectedTo.toString();
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.COMMUNITY_NETWORK_CONNECTION_EVENT;
	}

}
