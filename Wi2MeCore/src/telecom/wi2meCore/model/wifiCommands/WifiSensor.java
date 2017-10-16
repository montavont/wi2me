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

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.ConnectionData;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionData;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.util.HashMap; 



public class WifiSensor extends WirelessNetworkCommand{

	public WifiSensor() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public WifiSensor(HashMap<String, String> params) {
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
	}


	@Override
	public void initializeCommand(IParameterManager parameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(IParameterManager parameters) {

		try {
			while ((ControllerServices.getInstance().getWifi().isConnected()))
			{

				Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);

				WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
				if (info != null)
				{
					int ip = info.getIpAddress();
					WifiConnectionData connectionData = WifiConnectionData.getNewWifiConnectionData(TraceManager.getTrace(), 
					WifiAP.getNewWifiAP(info.getBSSID(), info.getSSID(), info.getRssi(), WifiAP.frequencyToChannel(((ScanResult)connectedToObj).frequency), ((ScanResult)connectedToObj).capabilities, info.getLinkSpeed()), 
					ConnectionData.getNewConnectionData(Utils.intToIp(ip), 0, 0, "SENSOR UPDATE", 0, 0, 0));
					Logger.getInstance().log(connectionData);
					Thread.sleep(50);
				}
			}

		}
		catch (InterruptedException e)
		{
			// if we are interrupted, we leave
			Log.d(getClass().getSimpleName(), "++ "+"Sensoring Interrupted", e);
		}

	}

}
