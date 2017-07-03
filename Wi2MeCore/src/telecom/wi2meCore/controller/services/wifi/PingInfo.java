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

package telecom.wi2meCore.controller.services.wifi;

public class PingInfo {
	
	public String ip;
	public int sent;
	public int received;
	public float rttMin;
	public float rttAvg;
	public float rttMax;
	public float rttMdev;
	
	public PingInfo(String ip){
		this.ip = ip;
		this.sent = 0;
		this.received = 0;
		this.rttMin = 0;
		this.rttMax = 0;
		this.rttAvg = 0;
		this.rttMdev = 0;
	}
	
	public PingInfo(String ip, int sent, int received, float rttMin, float rttAvg, float rttMax, float rttMdev){
		this.ip = ip;
		this.sent = sent;
		this.received = received;
		this.rttMin = rttMin;
		this.rttMax = rttMax;
		this.rttAvg = rttAvg;
		this.rttMdev = rttMdev;
	}

}
