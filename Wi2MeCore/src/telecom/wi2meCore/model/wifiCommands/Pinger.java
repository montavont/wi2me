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

package telecom.wi2meCore.model.wifiCommands;

import java.io.IOException;
import java.util.HashMap; 

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.wifi.PingInfo;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiPing;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;



/**
 * Wireless network command used to ping the gateway of a server of Telecom Bretagne Rennes.
 * @author XXX
 *
 */
public class Pinger extends WirelessNetworkCommand{

	private String lastTestedBSSID = "";
	private boolean gateway;

	private static String GATEWAY_KEY = "gateway";

	public Pinger(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.gateway = Boolean.valueOf(params.get(GATEWAY_KEY));

	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
	}

	@Override
	public void run(IParameterManager parameters) {
		//if ((Boolean)parameters.getParameter(Parameter.COMMUNITY_NETWORK_CONNECTED)){
		Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
		if (connectedToObj != null &&
			(Boolean)parameters.getParameter(Parameter.WIFI_CONNECTED) &&
			(Boolean)parameters.getParameter(Parameter.COMMUNITY_NETWORK_CONNECTED)){
			WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
			if (info != null && ControllerServices.getInstance().getWifi().isConnectedToAP()){
				if (isLastTested(info.getBSSID())){
					Log.d(getClass().getSimpleName(), "++ "+ "ALREADY TESTED. BSSID:"+info.getBSSID());
					return;
				}
				String ip;
				if (gateway){
					ip = Utils.intToIp(ControllerServices.getInstance().getWifi().getDhcpInfo().gateway);
				}else{
					ip = (String)parameters.getParameter(Parameter.PING_SERVER_IP);
				}
				try {
					ScanResult connectedTo = (ScanResult) connectedToObj;
					PingInfo pingInfo = ping(ip, (Float)parameters.getParameter(Parameter.PING_DEADLINE),
											(Integer)parameters.getParameter(Parameter.PING_PACKETS),
											(Float)parameters.getParameter(Parameter.PING_INTERVAL));

					WifiInfo current = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
					if (current != null){
						if (info.getBSSID().equals(current.getBSSID())){
							//We keep the bssid, only if it did not change in the middle of the ping
							lastTestedBSSID = info.getBSSID();
						}
					}

					WifiPing wifiPing = WifiPing.getNewWifiPing(TraceManager.getTrace(), pingInfo.ip, 
							pingInfo.sent, pingInfo.received, pingInfo.rttMin, pingInfo.rttMax, 
							pingInfo.rttAvg, pingInfo.rttMdev, WifiAP.getNewWifiAP(info.getBSSID(), info.getSSID(), info.getRssi(), WifiAP.frequencyToChannel(connectedTo.frequency), connectedTo.capabilities, info.getLinkSpeed()));

					Logger.getInstance().log(wifiPing);

				} catch (IOException e) {
					// finish here, log nothing
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				} catch (InterruptedException e) {
					// finish here, log nothing
					Log.d(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				} catch (RuntimeException e) {
					// UNEXPECTED! finish here
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
			}

		}

	}

	public PingInfo ping(String ip, float deadline, int packets, float interval) throws IOException, InterruptedException{
		return ControllerServices.getInstance().getWifi().ping(ip, deadline, packets, interval);
	}

	public boolean isLastTested(String bssid) {
		return lastTestedBSSID.equals(bssid);
	}

}
