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

import java.util.List;

import telecom.wi2meCore.controller.services.persistance.DatabaseHelper.TraceType;
import telecom.wi2meCore.model.entities.WifiAP;


public class WifiScanResult extends Trace {
	
	public static final String TABLE_NAME = "WifiScanResult";
	
	private List<WifiAP> results;
	
	protected WifiScanResult(Trace trace, List<WifiAP> results){
		Trace.copy(trace, this);
		this.results = results;
	}

	public List<WifiAP> getResults() {
		return results;
	}
	
	public static WifiScanResult getNewWifiScanResult(Trace trace, List<WifiAP> results){
		return new WifiScanResult(trace, results);
	}
	
	public String toString(){
		return super.toString() + "WIFI_SCAN_RESULT:" + getAPsAsString();
	}
	
	private static final String WIFI_AP_SEPARATOR = "-";

	private String getAPsAsString() {
		String ret = "";
		for (WifiAP ap : results){
			ret += ap.toString() + WIFI_AP_SEPARATOR;
		}
		//Remove the last separator
		if (ret.endsWith(WIFI_AP_SEPARATOR))
			ret = ret.substring(0, ret.lastIndexOf(WIFI_AP_SEPARATOR));
		return ret;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.WIFI_SCAN_RESULT;
	}

}
