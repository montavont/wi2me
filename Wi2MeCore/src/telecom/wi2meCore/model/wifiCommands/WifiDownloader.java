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


import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.configuration.Timers;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.DownloadingFailException;
import telecom.wi2meCore.controller.services.exceptions.DownloadingInterruptedException;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.Utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.util.HashMap; 


/**
 * Wireless network command used to download a file using the wifi network.
 * @author XXX
 *
 */
public class WifiDownloader extends WirelessNetworkCommand{
	
	private String server;
	private String filePath;
	private long length;
	private String lastTestedBSSID = "";
		
	private static String SERVER_KEY = "server";
	private static String PATH_KEY = "path";
	private static String LENGTH_KEY = "size";

	public WifiDownloader(HashMap<String, String> params)
	{
		this.server = params.get(SERVER_KEY);
		this.filePath = params.get(PATH_KEY);
		this.length = Integer.parseInt(params.get(LENGTH_KEY));
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING HERE
	}

	@Override
	public void run(IParameterManager parameters) {
		boolean canDownload = false;
		lastTestedBSSID = "";
		WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
		if ((Boolean)parameters.getParameter(Parameter.COMMUNITY_NETWORK_CONNECTED)){
			WifiBytesTransferedReceiver downloadReceiver = null;
			Object connectedObj = parameters.getParameter(Parameter.WIFI_CONNECTED);
			Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
			if (connectedObj != null && connectedToObj != null){
				if ((Boolean)connectedObj){
					if (ControllerServices.getInstance().getWifi().isConnected() && info != null){
						downloadReceiver = new WifiBytesTransferedReceiver(Utils.TYPE_DOWNLOAD, parameters); 
						canDownload = true;						
					}

				}
			}				
			if (canDownload){
				try {					
					WifiInfo current = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
					if (download(downloadReceiver, current.getBSSID(), current.getSSID())){
						//if download was complete and successful
						current = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
						if (current != null){
							if (info.getBSSID().equals(current.getBSSID())){
								//We keep the bssid, only if it did not change in the middle of the download
								lastTestedBSSID = info.getBSSID();
							}
						}
					}else{
						//if download was not complete, it was unsuccessful, so an error ocurred (normally, we downloaded something we did not wan to, as an Authentication page of a Captive Portal)
						// We will simulate disconnection of community network, so that the following pings, uploads and downloads won't take place, and the cleaner will disconnect the network properly
						parameters.setParameter(Parameter.COMMUNITY_NETWORK_CONNECTED, false);
					}
					
				} catch (DownloadingInterruptedException e) {
					// If we are interrupted, just finish execution
					Log.d(getClass().getSimpleName()+"-INTERRUPTED", "++ "+e.getMessage(), e);
				} catch (DownloadingFailException e) {
					// If other failure happens, log it (probably connection was lost, but that is normal). Then, make sure you are disconnected
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We will simulate disconnection of community network, so that the following pings, uploads and downloads won't take place, and the cleaner will disconnect the network properly
					parameters.setParameter(Parameter.COMMUNITY_NETWORK_CONNECTED, false);
				} catch (TimeoutException e) {
					// If timeout was obtained, log and finish connection
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We will simulate disconnection of community network, so that the following pings, uploads and downloads won't take place, and the cleaner will disconnect the network properly
					parameters.setParameter(Parameter.COMMUNITY_NETWORK_CONNECTED, false);
				}
			}
		}
	}
	
	public boolean download(IBytesTransferredReceiver rec, String bssid, String ssid) throws DownloadingInterruptedException, DownloadingFailException, TimeoutException {
		
		return ControllerServices.getInstance().getWeb().downloadFile(server, filePath, rec, length, TimeoutConstants.WIFI_DOWNLOAD_CONNECT_TIMEOUT, TimeoutConstants.WIFI_DOWNLOAD_SOCKET_TIMEOUT, Timers.WIFI_DOWNLOAD_RECEIVER_CALL_TIMER, bssid, ssid);
	}
	
	public boolean isLastTested(String bssid){
		return lastTestedBSSID.equals(bssid);
	}
}
