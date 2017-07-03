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

import android.util.Log;

public class ConnectionData {
	
	public static final String IP = "ipAddress";
	public static final String BYTES_TRANSFERRED = "bytesTransferred";
	public static final String TOTAL_BYTES = "totalBytes";
	public static final String TYPE = "type";
	public static final String TX_PACKETS = "txPackets";
	public static final String RX_PACKETS = "rxPackets";
	public static final String RETRIES = "retries";
	//public static final String CONNECTION_ID = "connectionId";
	
	private String ip;
	private int bytesTransferred;
	private int totalBytes;
	private String type;
	private int txPackets;
	private int rxPackets;
	private int retries;
	//private int connectionId;
	
	public ConnectionData(String ip, int bytesTransferred, int totalBytes, String type, int txPackets, int rxPackets, int retries){
		this.type = type;		
		this.ip = ip;
		this.bytesTransferred = bytesTransferred;
		this.totalBytes = totalBytes;
		this.txPackets = txPackets;
		this.rxPackets = rxPackets;
		this.retries = retries;
	}	

	public String getType() {
		return type;
	}
	
	public String getIp() {
		return ip;
	}

	public int getBytesTransferred() {
		return bytesTransferred;
	}

	public int getTotalBytes() {
		return totalBytes;
	}
	
	public int getTxPackets() {
		return txPackets;
	}
	
	public int getRxPackets() {
		return rxPackets;
	}
	
	public int getRetries() {
		return retries;
	}
	
	private static final String SEPARATOR = "-";
	
	public String toString(){
		return type + SEPARATOR + ip + SEPARATOR + bytesTransferred + SEPARATOR + totalBytes + SEPARATOR + txPackets+ SEPARATOR + rxPackets+ SEPARATOR + retries;
	}
	
	public static ConnectionData getNewConnectionData(String ip, int bytesTransferred, int totalBytes, String type, int txPackets, int rxPackets, int retries){
		//Log.w("ConnectionData", "++ " + " TX " + txPackets + " RX " + rxPackets + " RET " + retries);
		return new ConnectionData(ip, bytesTransferred, totalBytes, type, txPackets, rxPackets, retries);
	}

}
