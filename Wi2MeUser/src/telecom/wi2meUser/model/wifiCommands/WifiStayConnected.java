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

import java.io.IOException;
import java.util.HashMap; 

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.web.WebService;
import telecom.wi2meCore.controller.services.wifi.APStatusService;
import telecom.wi2meCore.controller.services.wifi.PingInfo;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiSnifferData;
import telecom.wi2meCore.model.entities.SnifferData;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.wifiCommands.CommunityNetworkConnector;
import telecom.wi2meCore.model.wifiCommands.WifiBytesTransferedReceiver;
import telecom.wi2meUser.controller.ApplicationService.ServiceBinder;



/**
 * Wireless network command used to sniff network traffic.
 * After verifying internet connection with a HTTP test,
 * the sniffer will begin to listen to the network traffic.
 * @author XXX
 *
 */

public class WifiStayConnected extends WirelessNetworkCommand{

	private IParameterManager parameters;
	private WifiInfo wifiInfo;
	private static final long CONNECTION_CHECK = 10000;
	private static final int MINLEVEL = -90; //TODO Confi File or Prefs !!!
	//private static final int MINLEVEL = -80;
	private static final int SNIFF_INTERVAL = 200;
	private long lastConnectivityCheckTimestamp = 0;
	private boolean firstTimeNotification=true;

	public WifiStayConnected(){}
	public WifiStayConnected(HashMap<String, String> params){}

	@Override
	public void initializeCommand(IParameterManager parameters) {
		this.parameters = parameters;		
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(IParameterManager parameters) {
		//We reset the timestamp to force a connectivity check on the first time.
		lastConnectivityCheckTimestamp=0;
		//We reset the notification counter to force a notification at the end of the connectivity check.
		firstTimeNotification=true;
		if ((Boolean) parameters.getParameter(Parameter.WIFI_CONNECTED)) {
			parameters.setParameter(Parameter.IPC_RECONNECTION_NEEDED, false);
			if (ControllerServices.getInstance().getWifi().isConnectedToAP()){ //We check we are actually connected
				try {
					WifiBytesTransferedReceiver snifferReceiver = new WifiBytesTransferedReceiver(Utils.TYPE_SNIFF, parameters);
					while (isConnected())
					{
						Object connectedObj = parameters.getParameter(Parameter.WIFI_CONNECTED);
						Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
						ScanResult currentAP = (ScanResult) connectedToObj;

						for (int i = 0; i < (CONNECTION_CHECK / SNIFF_INTERVAL); i++) 
						{
							if((Boolean)parameters.getParameter(Parameter.IPC_RECONNECTION_NEEDED))
							{
								Log.w(getClass().getSimpleName(), "++ " + "Reconnection needed");
								break;
							}

							wifiInfo = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
							if(wifiInfo==null||!ControllerServices.getInstance().getWifi().isConnected()){
								Log.d(getClass().getSimpleName(), "++ " + "Disconnected");
								break;
							}

							try {
								snifferReceiver.sniffer();
							} catch (Exception e) {
								Log.d(getClass().getSimpleName(), "++ " + "Sniffer error: " + e.toString());
								e.printStackTrace();
							}
							Thread.sleep(SNIFF_INTERVAL);
						}

						if(wifiInfo==null||!ControllerServices.getInstance().getWifi().isConnected()){
							break;
						}
						if (wifiInfo.getRssi()<= MINLEVEL){
							Log.w(getClass().getSimpleName(), "++ " + "Reached below threshold : " + wifiInfo.getRssi());
							break;
						}

						if((Boolean)parameters.getParameter(Parameter.IPC_RECONNECTION_NEEDED))
						{
							parameters.setParameter(Parameter.IPC_RECONNECTION_NEEDED, false);
							break;
						}
					}
					Log.d(getClass().getSimpleName(), "++ " + "StayConnected end");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Uses the notification service to notify the user of a successful connection.
	 */
	private void notifyConnection() {
		ControllerServices.getInstance().getNotification().playNotificationSound();
	}

	/**
	 * This method checks every Parameter.CONNECTIVITY_CHECK_FREQUENCY milliseconds whether there is a connection to the Internet or not.
	 * The method calls the WebService.isOnline() method which performs an HTTP request to a server in Telecom Rennes.
	 * If the time between the last check and current check is below the Parameter.CONNECTIVITY_CHECK_FREQUENCY milliseconds, returns true without checking the connectivity.
	 * If the check is NOK, attempts a new Authentication to the AP.
	 * If after a re-Authentication, still NOK, return false.
	 * @return True if connected or not checked, false if not connected.
	 */
	private boolean isConnected(){
		boolean performConnectivyCheck = (Boolean) parameters.getParameter(Parameter.PERFORM_CONNECTIVITY_CHECK);
		if(performConnectivyCheck){
			long currentTimestamp = ControllerServices.getInstance().getTime().getCurrentTimeInMilliseconds();
			int connectivityCheckFrequency = (Integer) parameters.getParameter(Parameter.CONNECTIVITY_CHECK_FREQUENCY);
			// We check the connectivity every XXXXX milliseconds.
			if(currentTimestamp-lastConnectivityCheckTimestamp>connectivityCheckFrequency){
				//To change the status of the application in Wi2MeUser
				StatusService.getInstance().changeStatus("Testing connectivity...");
				if (!ControllerServices.getInstance().getWeb().isOnline(WebService.Route.WIFI))
				{
					Log.w(getClass().getSimpleName(),"++ "+"Connectivity lost, attempting authentication again.");
					//To notify again if the re-authentication solves the connectivity problem.
					firstTimeNotification=true;
					//We attempt a re-authentication to try to solve the connectivity problem.
					CommunityNetworkConnector cnConnector = new CommunityNetworkConnector();
					cnConnector.initializeCommand(parameters);
					cnConnector.run(parameters);
					//We check again the connectivity.
					if(!ControllerServices.getInstance().getWeb().isOnline(WebService.Route.WIFI))
					{
						Log.w(getClass().getSimpleName(), "++ Unable to execute http request, no connectivity.");
						//We increase the number of failures for this AP.
						WifiInfo currentAP = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
						if(currentAP!=null){
							APStatusService.getInstance().addFailing(currentAP.getSSID());
						}
						return false;
					}
				}
				lastConnectivityCheckTimestamp=currentTimestamp;
			}
		}
		//If we are indeed connected, do the following.
		APStatusService.getInstance().reset();
		//To change the status of the application in Wi2MeUser
		StatusService.getInstance().changeStatus("Connected");
		if (firstTimeNotification&&(Boolean)parameters.getParameter(Parameter.NOTIFY_WHEN_WIFI_CONNECTED)){
			notifyConnection();
		}	
		//We don't notify every time, but only on the first check of connectivity.
		firstTimeNotification=false;
		return true;
	}
}
