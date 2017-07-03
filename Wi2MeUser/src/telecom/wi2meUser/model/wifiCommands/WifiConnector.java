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

package telecom.wi2meUser.model.wifiCommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.wifi.IWifiConnectionEventReceiver;
import telecom.wi2meCore.controller.services.wifi.APStatusService;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.CommunityNetworkConnector;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.util.Log;


/**
 * Wireless network command used to connect to a wifi AP.
 * After sorting the scan results using different parameters (user preference, protected/open, isCommunityNetwork, level in dBm),
 * the connector will attempt a connection to the correct AP.
 * @author XXX
 *
 */
public class WifiConnector extends WirelessNetworkCommand{

	private static final String SYNCHRONIZATION = "WIFI_CELL_SYNCHRONIZATION_FINISHED";
	private static final String CONNECTING_TIMEOUT = "TIMEOUT";

	private ScanResult connectionTo = null;

	//private WifiConnectionEventMonitor connectionEventReceiver;
	private IParameterManager parameters;

	public WifiConnector(){}
	public WifiConnector(HashMap<String, String> params){}

	@Override
	public void initializeCommand(IParameterManager parameters) {
		this.parameters = parameters;
		/*connectionEventReceiver = new WifiConnectionEventMonitor();
		ControllerServices.getInstance().getWifi().registerConnectionEventsReceiver(connectionEventReceiver);*/
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) 
	{
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(IParameterManager parameters) {
		//To change the status of the application in Wi2MeUser
		String checkedNetwork="";
		StatusService.getInstance().changeStatus("Connecting...");
           	ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

		Object resultsObj = parameters.getParameter(Parameter.WIFI_SCAN_RESULT);
		Object comNetsObj = parameters.getParameter(Parameter.COMMUNITY_NETWORKS);
		Object thresholdObj = parameters.getParameter(Parameter.WIFI_THRESHOLD);
		Object apGradeMapObj = parameters.getParameter(Parameter.AP_GRADE_MAP);
		Boolean connected = false;

		if (resultsObj != null && comNetsObj != null && thresholdObj != null && apGradeMapObj != null){
			try {			
				int trsh = (Integer)thresholdObj;
				List<ScanResult> results = (List<ScanResult>) resultsObj;
				List<CommunityNetworks> comNets = (List<CommunityNetworks>) comNetsObj;
				HashMap<String,Integer> apGradeMap = (HashMap<String,Integer>) apGradeMapObj;
				List<WifiConfiguration> knownNetworks = ControllerServices.getInstance().getWifi().getKnownNetworks();
				WifiTransferrerContainer transferrers = (WifiTransferrerContainer) parameters.getParameter(Parameter.WIFI_TRANSFER_COMMANDS);
				sortResultsBySignalLevelAndCommunityNetworks(results, comNets, knownNetworks, apGradeMap);
				Log.d(getClass().getSimpleName(), "++ " + results.toString());
				for (ScanResult r : results){	
					checkedNetwork="";
					boolean connectTo = false;
					String type = "";
					int apNumber=0;
					if (r.level >= trsh){
						checkedNetwork=r.SSID;
						Log.d(getClass().getSimpleName(), "++ " + "Checked Network : " + r.SSID);
						if((!r.capabilities.equals("")) &&  (!r.capabilities.equals("[ESS]"))) 
						{
							for (WifiConfiguration ap : knownNetworks){
								Log.d(getClass().getSimpleName(), "++ " + "PreferredNetwork" + ap.SSID);
								if (r.SSID.equals(ap.SSID.replaceAll("\"",""))&&!communityService.isCommunityNetwork(r.SSID, comNets)){
									connectTo=true;
									type="Preferred";
									apNumber = knownNetworks.indexOf(ap);
									Log.d(getClass().getSimpleName(), "++ " + "Preferred AP found");
									break;
								}
							}
						}
						//Check if it is an opened network, if it is Community Network, it will be one of the first in the list
						else {//this means it is an open network
							if (!(Boolean)parameters.getParameter(Parameter.CONNECT_TO_OPEN_NETWORKS)){
								//If open networks are not enabled, connect only to community networks
								if (!communityService.isCommunityNetwork(r.SSID, comNets)){
									//we break because all CNs are first in the list, so when we find one that is not there is no need to continue
									break;
								}
							}
							connectTo=true;
							type="Community";
							Log.d(getClass().getSimpleName(), "++ " + "CommunityNetwork AP found");
						}
						if(connectTo==true){
							//We inform that we will attempt to connect (this helps synchronization with the cell thread)
							parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, true);			
							if ((Boolean)parameters.getParameter(Parameter.CELL_CONNECTED) || (Boolean)parameters.getParameter(Parameter.CELL_CONNECTING)){
								//If the cell connection means that it is transferring, we must interrupt that, if not, only wait for the cell to be disconnected
								if ((Boolean)parameters.getParameter(Parameter.CELL_TRANSFERRING)){
									//We interrupt to cancel the transfer
									ControllerServices.getInstance().getSync().syncCellThread(true);
								}else{
									//We do not interrupt, just wait for disconnection, because activating the WIFI_CONNECTION_ATTEMPT will make commands not work and that will release disconnection
									ControllerServices.getInstance().getSync().syncCellThread(false);
								}
								//now we can make sure that the cell network is disconnected, and it should not attempt to connect while the WIFI_CONNECTION_ATTEMPT parameter is in true
								//we log the synchronization event
								Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), SYNCHRONIZATION));
							}
							if (type=="Community"){
								connected = connectTo(r);
							}else if (type=="Preferred"){
								WifiConfiguration ap = knownNetworks.get(apNumber);
								connected = connectTo(r, ap);
							}
							if (connected){
								Log.d(getClass().getSimpleName(), "++ " + "Connected");
								//We keep a reference of the AP we are connected to
								parameters.setParameter(Parameter.WIFI_CONNECTED_TO_AP, connectionTo);
		
								break;
							}
						}
						if (connected){
							break;
						}						
					}
				}

			}catch(TimeoutException e){
				Log.d(getClass().getSimpleName(), "++ "+"Connecting Timeout", e);
				APStatusService.getInstance().addFailing(checkedNetwork);
				//We must forget the network not to connect later
				ControllerServices.getInstance().getWifi().cleanNetworks();				
				//Log the timeout event
				WifiAP connectionToWifiAP = WifiAP.getWifiAPFromScanResult(connectionTo);
				WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
				WifiConnectionEvent timeoutEvent = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), CONNECTING_TIMEOUT+"("+ TimeoutConstants.WIFI_CONNECTING_TIMEOUT +"ms)", connectionToWifiAP);
				Logger.getInstance().log(timeoutEvent);
				connected = false;
			} catch (InterruptedException e) {
				//We must forget the network not to connect later
				ControllerServices.getInstance().getWifi().cleanNetworks();
				// If connection is interrupted, we finish here
				connected = false;
			} 
		}
		parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, connected);
		parameters.setParameter(Parameter.WIFI_CONNECTED, connected);

	}

	/**
	 * Sorts the scan results using different parameters (user preference, protected/open, isCommunityNetwork, level in dBm).
	 * Uses the private class APComparator.
	 * @param results
	 * @param comNets
	 * @param knownNets
	 * @param apGradeMap
	 */
	private void sortResultsBySignalLevelAndCommunityNetworks(List<ScanResult> results, List<CommunityNetworks> comNets, List<WifiConfiguration> knownNets, HashMap<String,Integer> apGradeMap) {
		Collections.sort(results, new APComparator(comNets, knownNets, apGradeMap));
	}

	/**
	 * This class is used to compare two Scan Results.
	 * To compare, we assign different values to each case:
	 * if this network has a grade[0..5], we assign the value 2000*grade
	 * if it is a known Network which is not opened, we assign the value 1000
	 * if it is a community Network, we assign the value 500.
	 * if at least 3 connection to this network failed, we assign the value -12000 to make sure this network will be the last one selected by the connector.
	 * Those value were chosen to make sure the signal level wouldn't interfere in the calculation if we have a known or community network.
	 * Rule used (importance of criteria) : Grade >> KnownNetworks (not opened) >> Community Networks >> Signal Strength >> Open Network
	 * @author Gilles Vidal
	 */
	private class APComparator implements Comparator<ScanResult>{
		private List<CommunityNetworks> comNets;
		private List<WifiConfiguration> knownNets;
		private HashMap<String,Integer> apGradeMap;
           	ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

		public APComparator(List<CommunityNetworks> comNets, List<WifiConfiguration> knownNets, HashMap<String,Integer> apGradeMap){
			this.comNets = comNets;
			this.knownNets = knownNets;
			this.apGradeMap=apGradeMap;
		}		

		@Override
		public int compare(ScanResult res1, ScanResult res2) {
			/* To compare, we assign different values to each case:
			 * if this network has a grade[0..5], we assign the value 2000*grade
			 * if it is a known Network which is not opened, we assign the value 1000
			 * if it is a community Network, we assign the value 500.
			 * if at least 3 connection to this network failed, we assign the value -12000 to make sure this network will be the last one selected by the connector.
			 * Those value were chosen to make sure the signal level wouldn't interfere in the calculation if we have a known or community network.
			 * Rule used (importance of criteria) : Grade >> KnownNetworks (not opened) >> Community Networks >> Signal Strength >> Open Network
			 */
			CommunityNetworks CNres1=communityService.whichCommunityNetwork(res1.SSID, comNets);
			CommunityNetworks CNres2=communityService.whichCommunityNetwork(res2.SSID, comNets);
			int isCNres1=0;
			int isCNres2=0;
			int isKnownres1=0;
			int isKnownres2=0;
			int res1Grade=0;
			int res2Grade=0;
			int res1Failures=0;
			int res2Failures=0;
			if(CNres1!=null){
				isCNres1=500;
			}
			if(CNres2!=null){
				isCNres2=500;
			}
			for (WifiConfiguration ap : knownNets){
				if (res1.SSID.equals(ap.SSID.replaceAll("\"",""))){
					isKnownres1 = 1000;
				}
				if (res2.SSID.equals(ap.SSID.replaceAll("\"",""))){
					isKnownres2 = 1000;
				}
				if(isKnownres1==1000&&isKnownres2==1000){
					break;
				}
			}
			if(CNres1!=null){
				//If it is a community network, we compare the regular expressions and not the SSID.
				//We check if any of the SSID in the APGradeMap is the same community network as res1.
				ArrayList<CommunityNetworks> CNres1List = new ArrayList<CommunityNetworks>();
				CNres1List.add(CNres1);
				for(String ap : apGradeMap.keySet()){
					if(communityService.isCommunityNetwork(ap, CNres1List)){
						res1Grade=apGradeMap.get(ap)*2000;
					}
				}
			}else if(apGradeMap.containsKey(res1.SSID)){
				res1Grade=apGradeMap.get(res1.SSID)*2000;
				if(isKnownres1!=1000){//means it is an open known network or a community network which doesn't have an account configured
					res1Grade=60*apGradeMap.get(res1.SSID);//To make sure a community network with no account doesn't get first.
				}
			}
			if(CNres2!=null){
				//If it is a community network, we compare the regular expressions and not the SSID.
				//We check if any of the SSID in the APGradeMap is the same community network as res2.
				ArrayList<CommunityNetworks> CNres2List = new ArrayList<CommunityNetworks>();
				CNres2List.add(CNres2);
				for(String ap : apGradeMap.keySet()){
					if(communityService.isCommunityNetwork(ap, CNres2List)){
						res2Grade=apGradeMap.get(ap)*2000;
					}
				}
			}else if(apGradeMap.containsKey(res2.SSID)){
				res2Grade=apGradeMap.get(res2.SSID)*2000;
				if(isKnownres2!=1000){//means it is an open known network or a community network which doesn't have an account configured
					res2Grade=60*apGradeMap.get(res2.SSID);//To make sure a community network with no account doesn't get first.
				}
			}
			if(APStatusService.getInstance().getFailureNumber(res1.SSID)>=3){
				res1Failures=-12000;
			}
			if(APStatusService.getInstance().getFailureNumber(res2.SSID)>=3){
				res2Failures=-12000;
			}
			int result = res2Grade-res1Grade+isKnownres2-isKnownres1+isCNres2-isCNres1+res2.level-res1.level+res2Failures-res1Failures; //the order is inverted as we sort the list from best to worst
			if(result<0){
				Log.d(getClass().getSimpleName(),"++ " +res1.SSID+" > "+res2.SSID);
			}else{
				Log.d(getClass().getSimpleName(),"++ "+res2.SSID+" > "+res1.SSID);
			}
			return(result);
		}
	}

	/**
	 * Connects to the AP. Used when the AP is not in the list of known networks.
	 * @param ap
	 * @param conEvRec
	 * @return True if the connection was successful, false if it failed.
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public boolean connectTo(ScanResult ap) throws TimeoutException, InterruptedException{
		connectionTo = ap;//we assign the ap which we want to connect to
		return ControllerServices.getInstance().getWifi().connect(getWifiConfigurationFromScanResult(ap), ap);
	}

	/**
	 * Connects to the AP. Used when the AP is in the list of known networks.
	 * @param ap
	 * @param conEvRec
	 * @return True if the connection was successful, false if it failed.
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public boolean connectTo(ScanResult ap, WifiConfiguration preferredAP) throws TimeoutException, InterruptedException{
		connectionTo = ap;//we assign the ap which we want to connect to
		Log.d(getClass().getSimpleName(), "++ " + "Connecting");
		ControllerServices.getInstance().getWifi().connect(preferredAP, ap);
		return true;
	}

	/**
	 * Transforms a scan result into a WifiConfiguration. Does not allow to get the pre-shared key.
	 * @param result
	 * @return The WifiConfiguration corresponding to the ScanResult
	 */
	private static WifiConfiguration getWifiConfigurationFromScanResult(ScanResult result){
		WifiConfiguration wifiConfig = new WifiConfiguration(); 
		//wifiConfig.hiddenSSID = true;
		wifiConfig.SSID = "\"" + result.SSID + "\"";
		wifiConfig.BSSID = result.BSSID; 
		wifiConfig.priority = 1;
		wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
		wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN); 
		wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
		return wifiConfig;
	}

	/*private class WifiConnectionEventMonitor implements IWifiConnectionEventReceiver{

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

	}*/

}
