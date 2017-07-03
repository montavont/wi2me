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


public class ConnectionInfo {

	public static final String LOCADD = "local_address";
	public static final String REMADD = "remote_address";
	public static final String LOCPORT = "local_port";
	public static final String REMPORT = "remote_port";
	public static final String STATE = "connection_state";
	public static final String PROTOCOL = "protocol";
	public static final String UID = "uid";
	
	private String localAdd;
	private String remoteAdd;
	private int localPort;
	private int remotePort;
	private ConnectionState cs;
	private String protocol;
	private boolean updated;
	private int uid;
	
	public enum ConnectionState{
		  UNKNOWN,
		  ESTABLISHED,
		  SYN_SENT,
		  SYN_RECV,
		  FIN_WAIT1,
		  FIN_WAIT2,
		  TIME_WAIT,
		  CLOSE,
		  CLOSE_WAIT,
		  LAST_ACK,
		  LISTEN,
		  CLOSING
	}
	
	public ConnectionInfo(String protocol, String localAdd, String remoteAdd, int localPort, int remotePort, ConnectionState cs, int uid){
		this.protocol = protocol;
		this.localAdd = localAdd;
		this.remoteAdd = remoteAdd;
		this.localPort = localPort;
		this.remotePort = remotePort;
		this.cs = cs;
		this.updated = false;
		this.uid = uid;
	}
	
	public int getUid(){
		return uid;
	}
	
	public boolean getUpdated() {
		return updated;
	}
	
	public void setUpdated() {
		this.updated = true;
	}
	
	public void unSetUpdated() {
		this.updated = false;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public String getLocalAdd() {
		return localAdd;
	}

	public String getRemoteAdd() {
		return remoteAdd;
	}
	
	public int getLocalPort() {
		return localPort;
	}

	public int getRemotePort() {
		return remotePort;
	}
	
	public ConnectionState getConnectionState() {
		return cs;
	}
	
	private static final String SEPARATOR = "-";
	
	public String toString(){
		return protocol + SEPARATOR + localAdd + SEPARATOR + remoteAdd + SEPARATOR + localPort + SEPARATOR + remotePort + SEPARATOR + cs + SEPARATOR + uid;
	}
	
	public static ConnectionInfo getNewConnectionInfo(String protocol, String localAdd, String remoteAdd, int localPort, int remotePort, ConnectionState cs, int uid){
		//Log.w("ConnectionData", "++ " + " TX " + txPackets + " RX " + rxPackets + " RET " + retries);
		return new ConnectionInfo(protocol, localAdd, remoteAdd, localPort, remotePort, cs, uid);
	}
	public boolean equalsExceptState(ConnectionInfo ci){
		if ((ci.protocol.equals(this.protocol)) && (ci.localAdd.equals(this.localAdd)) && (ci.localPort == this.localPort)
		&& (ci.remoteAdd.equals(this.remoteAdd)) && (ci.remotePort == this.remotePort)) return true;
		else return false;
	}
	
}
