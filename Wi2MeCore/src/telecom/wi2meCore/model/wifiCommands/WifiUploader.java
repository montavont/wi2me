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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile; 
import java.util.HashMap; 

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.configuration.Timers;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.exceptions.UploadingFailException;
import telecom.wi2meCore.controller.services.exceptions.UploadingInterruptedException;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Environment;
import android.util.Log;




/**
 * Wireless network command used to upload a file using the wifi network.
 * @author XXX
 *
 */
public class WifiUploader extends WirelessNetworkCommand{
	
	private String server;
	private String script;
	private String lastTestedBSSID = "";
	private byte[] file = null;
			
	private static String DATA_DIR = "upload_files/";

	private static String SERVER_KEY = "server";
	private static String SCRIPT_KEY = "script";
	private static String SIZE_KEY = "size";

	public WifiUploader(HashMap<String, String> params)
	{
		this.server = params.get(SERVER_KEY);
		this.script = params.get(SCRIPT_KEY);
		try
		{
			File uploadDataDir = new File(Environment.getExternalStorageDirectory() + ConfigurationManager.WI2ME_DIRECTORY + DATA_DIR);
			uploadDataDir.mkdir();

			RandomAccessFile f = new RandomAccessFile(Environment.getExternalStorageDirectory() +  ConfigurationManager.WI2ME_DIRECTORY + DATA_DIR + params.get(SIZE_KEY) , "rw");
			f.setLength(Integer.parseInt(params.get(SIZE_KEY)));
			file = Utils.RAFToByteArray(f);

		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ "+e.getMessage());
		}

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
		boolean canUpload = false;
		WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
		if ((Boolean)parameters.getParameter(Parameter.COMMUNITY_NETWORK_CONNECTED)){
			WifiBytesTransferedReceiver uploadReceiver = null;
			Object connectedObj = parameters.getParameter(Parameter.WIFI_CONNECTED);
			Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
			if (connectedObj != null && connectedToObj != null){
				if ((Boolean)connectedObj){
					if (ControllerServices.getInstance().getWifi().isConnected() && info != null){
						uploadReceiver = new WifiBytesTransferedReceiver(Utils.TYPE_UPLOAD, parameters);
						canUpload = true;						
					}
				}
			}
			if (canUpload){
				try {					
					if (upload(uploadReceiver)){
						WifiInfo current = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
						if (current != null){
							if (info.getBSSID().equals(current.getBSSID())){
								//We keep the bssid, only if it did not change in the middle of the upload
								lastTestedBSSID = info.getBSSID();
							}							
						}
					}
				} catch (UploadingInterruptedException e) {
					// If we are interrupted, just finish execution
					Log.d(getClass().getSimpleName()+"-INTERRUPTED", "++ "+e.getMessage(), e);
				} catch (UploadingFailException e) {
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
		
	public boolean upload(IBytesTransferredReceiver rec) throws UploadingInterruptedException, UploadingFailException, TimeoutException{
		Log.d(getClass().getSimpleName(), "++ "+ "LENGTH: " + file.length);
		return ControllerServices.getInstance().getWeb().uploadFile(server, script, rec, file, TimeoutConstants.WIFI_UPLOAD_CONNECT_TIMEOUT, TimeoutConstants.WIFI_UPLOAD_SOCKET_TIMEOUT, Timers.WIFI_UPLOAD_RECEIVER_CALL_TIMER);
	}
	
	public boolean isLastTested(String bssid){
		return lastTestedBSSID.equals(bssid);
	}
}
