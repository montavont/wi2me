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

public class SnifferData {
	
	public static final String TX_BYTES = "tx_bytes";
	public static final String RX_BYTES = "rx_bytes";
	public static final String TX_PACKETS = "txPackets";
	public static final String RX_PACKETS = "rxPackets";
	public static final String RETRIES = "retries";
	public static final String IP = "ip";
	
	private int txBytes;
	private int rxBytes;
	private int txPackets;
	private int rxPackets;
	private int retries;
	private String ip;
	
	public SnifferData(int txBytes, int rxBytes, int txPackets, int rxPackets, int retries, String ip){		
		this.txBytes = txBytes;
		this.rxBytes = rxBytes;
		this.txPackets = txPackets;
		this.rxPackets = rxPackets;
		this.retries = retries;
		this.ip = ip;
	}	
	
	public String getIp() {
		return ip;
	}
	
	public int getTxBytes() {
		return txBytes;
	}
	
	public int getRxBytes() {
		return rxBytes;
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
		return txBytes + SEPARATOR + rxBytes + SEPARATOR + txPackets+ SEPARATOR + rxPackets+ SEPARATOR + retries;
	}
	
	public static SnifferData getNewSnifferData(int txBytes, int rxBytes, int txPackets, int rxPackets, int retries, String ip){
		//Log.w("ConnectionData", "++ " + " TX " + txPackets + " RX " + rxPackets + " RET " + retries);
		return new SnifferData(txBytes, rxBytes, txPackets, rxPackets, retries, ip);
	}

}
