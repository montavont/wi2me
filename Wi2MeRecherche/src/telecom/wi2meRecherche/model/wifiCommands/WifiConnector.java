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

package telecom.wi2meRecherche.model.wifiCommands;

import java.io.BufferedReader;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.wifi.IWifiConnectionEventReceiver;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.CommunityNetworkConnector;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Logger;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.util.Log;



public class WifiConnector extends WirelessNetworkCommand{
	
	private static final String SYNCHRONIZATION = "WIFI_CELL_SYNCHRONIZATION_FINISHED";
	private static final String CONNECTING_TIMEOUT = "TIMEOUT";
	
	private ScanResult connectionTo = null;
	private WifiConnectionEventMonitor connectionEventMonitor;
	private IParameterManager parameters;
	
	private static final String BSSID_RESTRICTION_PATH_KEY = "bssid_file";

	private String bssidRestrictionPath = "";
	
	public WifiConnector(HashMap<String, String> params)
	{
		if (params.containsKey(BSSID_RESTRICTION_PATH_KEY))
		{
			this.bssidRestrictionPath = params.get(BSSID_RESTRICTION_PATH_KEY);
			File testPath = new File(this.bssidRestrictionPath);
			if (!testPath.exists())
			{
				this.bssidRestrictionPath = "";
			}
		}
	}

	@Override
	public void initializeCommand(IParameterManager parameters)
	{
		this.parameters = parameters;
	}

	@Override
	public void finalizeCommand(IParameterManager parameters)
	{
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void run(IParameterManager parameters)
	{
		Object resultsObj = parameters.getParameter(Parameter.WIFI_SCAN_RESULT);
		Object comNetsObj = parameters.getParameter(Parameter.COMMUNITY_NETWORKS);
		Object thresholdObj = parameters.getParameter(Parameter.WIFI_THRESHOLD);
		Boolean connected = false;
		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();
	
		ArrayList<String> restrictedBssids = new ArrayList<String>();
		
		if (this.bssidRestrictionPath.length() > 0)
		{
			File restrictFile = new File(this.bssidRestrictionPath);
			try
			{
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(restrictFile)));
				String line;	
				while ((line = bufferedReader.readLine()) != null)
				{	
		    			restrictedBssids.add(line);
				}
			}
			catch (java.io.FileNotFoundException e)
			{
				Log.d(getClass().getSimpleName(), "++ bssidRestrictionFile " + this.bssidRestrictionPath + " does not exist");
			}
			catch (java.io.IOException e)
			{
				Log.e(getClass().getSimpleName(), "++ Error reading bssid restriction file " + this.bssidRestrictionPath);
			}
		}


		if (resultsObj != null && comNetsObj != null && thresholdObj != null)
		{
			try
			{
				int trsh = (Integer)thresholdObj;
				List<ScanResult> results = (List<ScanResult>) resultsObj;
				List<CommunityNetworks> comNets = (List<CommunityNetworks>) comNetsObj;
				WifiTransferrerContainer transferrers = (WifiTransferrerContainer) parameters.getParameter(Parameter.WIFI_TRANSFER_COMMANDS);
				sortResultsBySignalLevelAndCommunityNetworks(results, comNets);
				Log.d(getClass().getSimpleName(), "++ " + results.toString());
				for (ScanResult r : results)
				{		
					if (r.level >= trsh)
					{			
						//Check if it is an opened network, if it is Community Network, it will be one of the first in the list
						if (r.capabilities.equals("")||r.capabilities.equals("[ESS]")) //this means it is an open network
						{
							if (!(Boolean)parameters.getParameter(Parameter.CONNECT_TO_OPEN_NETWORKS))	
							{
								//If open networks are not enabled, connect only to community networks
								if (!communityService.isCommunityNetwork(r.SSID, comNets))
								{
									//we break because all CNs are first in the list, so when we find one that is not there is no need to continue
									break;
								}
								
							}


							//We inform that we will attempt to connect (this helps synchronization with the cell thread)
							parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, true);			
							if ((Boolean)parameters.getParameter(Parameter.CELL_CONNECTED) || (Boolean)parameters.getParameter(Parameter.CELL_CONNECTING))
							{
								//If the cell connection means that it is transferring, we must interrupt that, if not, only wait for the cell to be disconnected
								if ((Boolean)parameters.getParameter(Parameter.CELL_TRANSFERRING))
								{
									//We interrupt to cancel the transfer
									ControllerServices.getInstance().getSync().syncCellThread(true);
								}
								else
								{
									//We do not interrupt, just wait for disconnection, because activating the WIFI_CONNECTION_ATTEMPT will make commands not work and that will release disconnection
									ControllerServices.getInstance().getSync().syncCellThread(false);
								}
								//now we can make sure that the cell network is disconnected, and it should not attempt to connect while the WIFI_CONNECTION_ATTEMPT parameter is in true
								//we log the synchronization event
								Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), SYNCHRONIZATION));
							}


							if (restrictedBssids.size() == 0 || restrictedBssids.contains(r.BSSID))
							{
								connected = connectTo(r);
								if (connected)
								{
									//We keep a reference of the AP we are connected to
									parameters.setParameter(Parameter.WIFI_CONNECTED_TO_AP, connectionTo);
									if ((Boolean)parameters.getParameter(Parameter.NOTIFY_WHEN_WIFI_CONNECTED))
									{
										notifyConnection();
									}											
									break;
								}
							} 
	
						}

						if (connected)
						{
							break;
						}						
					}
				}

			}
			catch(TimeoutException e)
			{
				Log.d(getClass().getSimpleName(), "++ "+"Connecting Timeout", e);
				//We must forget the network not to connect later
				ControllerServices.getInstance().getWifi().cleanNetworks();				
				//Log the timeout event
				WifiAP connectionToWifiAP = WifiAP.getWifiAPFromScanResult(connectionTo);
				WifiConnectionEvent timeoutEvent = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), CONNECTING_TIMEOUT+"("+ TimeoutConstants.WIFI_CONNECTING_TIMEOUT +"ms)", connectionToWifiAP);
				WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
				Logger.getInstance().log(timeoutEvent);

				connected = false;
			}
			catch (InterruptedException e)
			{
				Log.d(getClass().getSimpleName(), "++ "+"Connection Interrupted ", e);
				//We must forget the network not to connect later
				ControllerServices.getInstance().getWifi().cleanNetworks();
				// If connection is interrupted, we finish here
				connected = false;
			} 
		}
		parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, connected);
		parameters.setParameter(Parameter.WIFI_CONNECTED, connected);

	}
	private void notifyConnection() {
		ControllerServices.getInstance().getNotification().playNotificationSound();
	}

	private void sortResultsBySignalLevelAndCommunityNetworks(
			List<ScanResult> results, List<CommunityNetworks> comNets) {
		Collections.sort(results, new APComparator(comNets));
	}
	
	private class APComparator implements Comparator<ScanResult>{
		private List<CommunityNetworks> comNets;
		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

		public APComparator(List<CommunityNetworks> comNets){
			this.comNets = comNets;
		}
		

		@Override
		public int compare(ScanResult res1, ScanResult res2) {
			boolean isCNres2 = communityService.isCommunityNetwork(res2.SSID, comNets);
			boolean isCNres1 = communityService.isCommunityNetwork(res1.SSID, comNets);
			if (isCNres2 && isCNres1){ //if both are community networks, order by signal level
				return res2.level - res1.level; //the order is inverted, as we need the higher (signal level) first
			}else{
				if (isCNres2){ //if this is a community network, this has more priority, so return positive value cause of the inverted order
					return 1;					
				}else{
					if (isCNres1){
						return -1;
					}else{//in this case none of them is CN
						return res2.level - res1.level;
					}
					
				}
			}
		}
		
	}
	
	
	//ublic boolean connectTo(ScanResult ap, IWifiConnectionEventReceiver conEvRec) throws TimeoutException, InterruptedException{
	public boolean connectTo(ScanResult ap) throws TimeoutException, InterruptedException{
		connectionTo = ap;//we assign the ap which we want to connect to
		return ControllerServices.getInstance().getWifi().connect(getWifiConfigurationFromScanResult(ap), ap);
	}
	
	private static WifiConfiguration getWifiConfigurationFromScanResult(ScanResult result){
	    	WifiConfiguration wifiConfig = new WifiConfiguration(); 
	    	wifiConfig.SSID = "\"" + result.SSID + "\"";
	        wifiConfig.BSSID = result.BSSID; 
	        wifiConfig.priority = 1;
	        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
	        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN); 
	        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
	        return wifiConfig;
	}
	
	private class WifiConnectionEventMonitor implements IWifiConnectionEventReceiver
	{

		@Override
		public void receiveEvent(String event) {			
			String extraInfo = "";
			WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
			//If we have connection info, we use it for the AP information. If we are not connected we should not use it, as it may have erroneous info
			if (info != null && ControllerServices.getInstance().getWifi().isConnected()){
				extraInfo = "(BSSID:" + info.getBSSID() + "-SSID:" + info.getSSID() + "-IP:" + Utils.intToIp(info.getIpAddress()) + "-DG:" + Utils.intToIp(ControllerServices.getInstance().getWifi().getDhcpInfo().gateway) + ")" ;				

			}
			Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(),"WIFI_CONNECTION_MONITOR:" + event+extraInfo)); 

		}
		
	}

}
