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

import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.communityNetworks.ICNConnectionEventReceiver;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.CommunityNetworkConnectionEvent;
import telecom.wi2meCore.model.entities.User;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;



/**
 * Wireless network command used to authenticate in a community network.
 * This class uses the javascript of the concerned community network to fill in the data on the login webpage of the community network.
 * @author XXX
 *
 */
public class CommunityNetworkConnector extends WirelessNetworkCommand{

	private static final String CONNECTING_TIMEOUT = "TIMEOUT";
	private static final String INTERRUPTED = "INTERRUPTED";

	private ScanResult connectedTo = null;
	private ICNConnectionEventReceiver receiver;
	private String usernameToConnect;
	private ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

	public CommunityNetworkConnector() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public CommunityNetworkConnector(HashMap<String, String> params) {
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
		receiver = new CNConnectionEventReceiver();
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING HERE
	}

	@Override
	public void run(IParameterManager parameters) {
		Boolean connected = false;
		if ((Boolean)parameters.getParameter(Parameter.WIFI_CONNECTED))
		{
			if (ControllerServices.getInstance().getWifi().isConnectedToAP()){ //We check we are actually connected
				//To change the status of the application in Wi2MeUser
				StatusService.getInstance().changeStatus("Hotspot Authentication...");

				Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
				Object comNetsObj = parameters.getParameter(Parameter.COMMUNITY_NETWORKS);
				Object usersObj = parameters.getParameter(Parameter.COMMUNITY_NETWORK_USERS);
				if (connectedToObj != null){
					ScanResult connectedTo = (ScanResult) connectedToObj;
					//check if we are connected to a community network, if not we are in an open one
					if (comNetsObj != null){
						@SuppressWarnings("unchecked")
						List<CommunityNetworks> comNets = (List<CommunityNetworks>) comNetsObj;
						if (communityService.isCommunityNetwork(connectedTo.SSID, comNets)){
							//if it is a community network, do the hard work of authenticating
							if (usersObj != null)
							{
								try {
									@SuppressWarnings("unchecked")
									List<User> users = (List<User>) usersObj;
									for (User user : users){
										if (connectedTo.SSID.matches(communityService.getRegExp(user.getCommunityNetwork()))){
											connected = connectToCommunityNetwork(user, connectedTo, receiver);
										}
									}
								} catch (TimeoutException e) {
									Log.d(getClass().getSimpleName(), "++ "+"Connecting to Community Network Timeout", e);
									//in this case we want the connection not to take place later
									communityService.stopCommunityNetworkConnection();

									receiver.receiveConnectionEvent(CONNECTING_TIMEOUT+"("+ TimeoutConstants.COMMUNITY_NETWORK_CONNECTION_TIMEOUT +"ms)");
									connected = false;

								} catch (InterruptedException e) {
									Log.d(getClass().getSimpleName(), "++ "+"Connecting to Community Network Interrupted", e);
									//in this case we want the connection not to take place later
									communityService.stopCommunityNetworkConnection();

									receiver.receiveConnectionEvent(INTERRUPTED);
									connected = false;

								}
							}
						}else{
							//if it is not a community network, simulate we are connected!
							connected = true;
						}
					}

				}
			}

		}
		parameters.setParameter(Parameter.COMMUNITY_NETWORK_CONNECTED, connected);
	}

	public boolean connectToCommunityNetwork(User user, ScanResult apConnectedTo, ICNConnectionEventReceiver rec) throws TimeoutException, InterruptedException{
		connectedTo = apConnectedTo;
		usernameToConnect = user.getName();


		return communityService.connectToCommunityNetwork(rec, user.getName(), user.getPassword(), communityService.getAuthenticationRoutine(user.getCommunityNetwork(), user.getName(), user.getPassword()));
	}

	private class CNConnectionEventReceiver implements ICNConnectionEventReceiver{

		@Override
		public void receiveConnectionEvent(String event) {
			CommunityNetworkConnectionEvent connectionEvent = null;
			WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
			if (info != null){
				connectionEvent = CommunityNetworkConnectionEvent.getNewCommunityNetworkConnectionEvent(TraceManager.getTrace(), event,
					WifiAP.getNewWifiAP(info.getBSSID(), info.getSSID(), info.getRssi(), WifiAP.frequencyToChannel(connectedTo.frequency), connectedTo.capabilities, info.getLinkSpeed()),
					usernameToConnect);
			}else{
				connectionEvent = CommunityNetworkConnectionEvent.getNewCommunityNetworkConnectionEvent(TraceManager.getTrace(), event,
						WifiAP.getWifiAPFromScanResult(connectedTo), usernameToConnect);
			}
			Logger.getInstance().log(connectionEvent);

		}

	}

}
