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

import android.net.wifi.ScanResult;

public class WifiAP {
	public static final String WIFI_AP_REFERENCE = WifiAP.TABLE_NAME + "Id";
	
	public static final String TABLE_NAME = "WifiAP";
	public static final String BSSID = "BSSID";
	public static final String SSID = "SSID";
	public static final String LEVEL = "level";
	public static final String CAPABILITIES = "capabilities";
	public static final String CHANNEL = "channel";
	public static final String LINK_SPEED = "linkSpeed";

	private String bssid;
	private String ssid;
	private int level;
	private int channel;
	private String capabilities;
	private int linkSpeed;
	
	private static int STARTING_CHANNEL_FREQUENCY = 2412;
	private static int CHANNEL_FREQUENCY_DIFFERENCE = 5;
	

	private WifiAP(String BSSID, String SSID, int level, int channel, String capabilities, int linkSpeed){
		this.bssid = BSSID;
		this.ssid = SSID;
		this.level = level;
		this.channel = channel;
		this.capabilities = capabilities;
		this.linkSpeed = linkSpeed;
	}
	
	public int getLinkSpeed() {
		return linkSpeed;
	}

	public void setLinkSpeed(int linkSpeed) {
		this.linkSpeed = linkSpeed;
	}	
	
	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public String getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(String capabilities) {
		this.capabilities = capabilities;
	}
	
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getSsid() {
		return this.ssid;
	}

	public String getBSSID() {
		return bssid;
	}
	public void setBSSID(String bSSID) {
		bssid = bSSID;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level =  level;
	}
	
	public static WifiAP getNewWifiAP(String BSSID, String SSID, int level, int channel, String capabilities, int linkSpeed){
		return new WifiAP (BSSID, SSID, level, channel, capabilities, linkSpeed);
	}
	
	private static final String ATRIBUTE_SEPARATOR = ",";
	
	public static int frequencyToChannel(int frequency){
		return (frequency - STARTING_CHANNEL_FREQUENCY)/CHANNEL_FREQUENCY_DIFFERENCE + 1;
	}
	
	public static int channelToFrequency(int channel){
		return (channel - 1)*CHANNEL_FREQUENCY_DIFFERENCE + STARTING_CHANNEL_FREQUENCY;
	}
	
	public String toString(){
		return bssid + ATRIBUTE_SEPARATOR + ssid + ATRIBUTE_SEPARATOR + level + ATRIBUTE_SEPARATOR + channel + ATRIBUTE_SEPARATOR + linkSpeed + ATRIBUTE_SEPARATOR + capabilities;
	}
	
	public static WifiAP getWifiAPFromScanResult(ScanResult scanResult){
		WifiAP retval = null;
		if (scanResult != null)
		{
			retval = getNewWifiAP(scanResult.BSSID, scanResult.SSID, scanResult.level, frequencyToChannel(scanResult.frequency), scanResult.capabilities, 0);
		}
		return retval;
	}
}
