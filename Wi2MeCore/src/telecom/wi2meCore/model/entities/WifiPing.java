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

import telecom.wi2meCore.model.entities.Trace.TraceType;
import telecom.wi2meCore.model.entities.WifiAP;

public class WifiPing extends Trace{

	public static final String TABLE_NAME = "WifiPing";
	public static final String IP = "pingedIp";
	public static final String SENT = "packetsSent";
	public static final String RECEIVED = "packetsReceived";
	public static final String RTT_MIN = "rttMin";
	public static final String RTT_MAX = "rttMax";
	public static final String RTT_AVG = "rttAvg";
	public static final String RTT_MDEV = "rttMdev";

	private String ip;
	private int sent;
	private int received;
	private float rttMin;
	private float rttAvg;
	private float rttMax;
	private float rttMdev;
	private WifiAP connectionTo;

	protected WifiPing(Trace trace, String ip, int sent, int received, float rttMin, float rttMax,
			float rttAvg, float rttMdev, WifiAP connectionTo){
		Trace.copy(trace, this);
		this.ip = ip;
		this.sent = sent;
		this.received = received;
		this.rttMin = rttMin;
		this.rttMax = rttMax;
		this.rttAvg = rttAvg;
		this.rttMdev = rttMdev;
		this.connectionTo = connectionTo;
	}


	public String getPingedIp() {
		return ip;
	}

	public int getPacketsSent() {
		return sent;
	}

	public int getPacketsReceived() {
		return received;
	}

	public float getRttMin() {
		return rttMin;
	}

	public float getRttAvg() {
		return rttAvg;
	}

	public float getRttMax() {
		return rttMax;
	}

	public float getRttMdev() {
		return rttMdev;
	}

	public WifiAP getConnectionTo() {
		return connectionTo;
	}

	public static WifiPing getNewWifiPing(Trace trace, String ip, int sent, int received, float rttMin, float rttMax,
			float rttAvg, float rttMdev, WifiAP connectionTo){
		return new WifiPing(trace, ip, sent, received, rttMin, rttMax, rttAvg, rttMdev, connectionTo);
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.WIFI_PING;
	}

	private static final String SEPARATOR = "-";

	public String toString(){
		return super.toString() + "WIFI_PING:" + ip + SEPARATOR + sent + SEPARATOR + received + SEPARATOR
		 + rttMin + SEPARATOR  + rttMax + SEPARATOR + rttAvg + SEPARATOR + rttMdev + SEPARATOR + connectionTo.toString();
	}

}
