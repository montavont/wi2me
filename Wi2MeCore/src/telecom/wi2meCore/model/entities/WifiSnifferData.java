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

public class WifiSnifferData extends Trace{

	public static final String TABLE_NAME = "WifiSnifferData";
	public static final String SNIFF_SEQUENCE = "SniffSequence";

	private WifiAP connectedTo;
	private SnifferData snifferData;
	private long sniffSequence;

	protected WifiSnifferData(Trace trace, WifiAP connectedTo, SnifferData snifferData, long sniffSequence){
		Trace.copy(trace, this);
		this.connectedTo = connectedTo;
		this.snifferData = snifferData;
		this.sniffSequence = sniffSequence;
	}

	public SnifferData getSnifferData(){
		return snifferData;
	}

	public WifiAP getConnectedTo() {
		return connectedTo;
	}

	public long getSniffSequence() {
		return sniffSequence;
	}

	public static WifiSnifferData getNewWifiSnifferData(Trace trace, WifiAP connectedTo, SnifferData snifferData, long sniffSequence){
		return new WifiSnifferData(trace, connectedTo, snifferData, sniffSequence);
	}

	private static final String SEPARATOR = "-";

	public String toString(){
		return super.toString() + "WIFI_SNIFFER_DATA:" + snifferData.toString() + SEPARATOR + connectedTo.toString() + SEPARATOR + sniffSequence;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.WIFI_SNIFFER_DATA;
	}

}
