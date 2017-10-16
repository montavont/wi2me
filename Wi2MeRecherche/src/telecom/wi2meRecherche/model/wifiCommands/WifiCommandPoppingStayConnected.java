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

import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.entities.ConnectionData;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionData;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;

import android.net.wifi.ScanResult;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.System;
import java.util.ArrayList;
import java.util.HashMap; 


/**
 * Wireless network command used pop an iw command to monitor the current access point state by sending probe requests
 * @author XXX
 *
 */
public class WifiCommandPoppingStayConnected extends WirelessNetworkCommand
{
	private IParameterManager parameters;
	private String iface = "";

	private int minRssi = -85;
	private int maxFailures = 5;

	private static String IFACE_KEY = "iface";
	private static String MIN_RSSI_KEY = "min_rssi";
	private static String MAX_FAILURES_KEY = "max_fail";


	public WifiCommandPoppingStayConnected(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.iface = params.get(IFACE_KEY);
		this.minRssi = Integer.parseInt(params.get(MIN_RSSI_KEY));
		this.maxFailures = Integer.parseInt(params.get(MAX_FAILURES_KEY));
	}

	@Override
	public void initializeCommand(IParameterManager parameters)
	{
		this.parameters = parameters;


	}

	@Override
	public void finalizeCommand(IParameterManager parameters)
	{
		// DO NOTHING HERE
	}

	@Override
	public void run(IParameterManager parameters)
	{
		int rssi = 0;
		int failures = 0;
		int progressIndex = 1; //Dummy data progression index to separate ocurences on data parsing
		ScanResult ap = (ScanResult)parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
		if (ap != null)
		{
			while (failures < maxFailures)
			{
				rssi = findRssiForBssid(popCommand("iw dev " + iface + " scan freq " + ap.frequency), ap.BSSID);
				WifiConnectionData connectionData = WifiConnectionData.getNewWifiConnectionData
				(
					TraceManager.getTrace(),
					WifiAP.getNewWifiAP(
						ap.BSSID,
						ap.SSID,
						rssi,
						WifiAP.frequencyToChannel(ap.frequency),
						ap.capabilities,
						0
					),
					ConnectionData.getNewConnectionData(
						"0.0.0.0",
						progressIndex, 
						0, 
						"DUMMY MONITORING",
						0,
						0,
						0
					)
				);
			
				if (rssi < minRssi)
				{
					failures += 1;
				}
				else
				{
					failures = 0;
				}

				Logger.getInstance().log(connectionData);

				//Sleep to avoid popping too many commands
				try
				{
					Thread.sleep(300); // Once every 800ms (scanning 1 freq takes around 500ms)
				}
				catch (java.lang.InterruptedException e)
				{
					//Whatever
					break;
				}
				progressIndex += 1;
			}
		}
	}

	private int findRssiForBssid(ArrayList<String> data, String bssid)
	{
		String bssidKey = "BSS ";
		String rssiKey = "signal: ";

		int retval = -200;
		String currentBssid = "00:00:00:00:00:00";

		for (String line : data)
		{
			

			if (line.startsWith(bssidKey))
			{
				currentBssid = line.split(" ", 16)[1];
			}
			else if (currentBssid.equals(bssid) && line.contains(rssiKey))
			{
				line = line.substring(line.indexOf(rssiKey), line.indexOf('.'));
				int tmpRssi = Integer.parseInt(line.split(" ", 16)[1]);
				//Same bssid twice in a scan ? The best value is probably the one we are monitoring
				if (tmpRssi > retval)
				{
					retval = tmpRssi;
				}
			}
		}
		return retval;
	}

	private ArrayList<String> popCommand(String command)
	{	
		ArrayList<String> retval = new ArrayList<String>();	
		OutputStreamWriter osw = null;
		try
		{
			Process process = Runtime.getRuntime().exec("su");
			osw = new OutputStreamWriter(process.getOutputStream());
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;	

			osw.write(command);
			osw.flush();
			osw.close();
 
			while ((line = bufferedReader.readLine()) != null)
			{	
		    		retval.add(line);	
			}

		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "Error popping command : " + command + " "+ e.getMessage(), e);
		}
		return retval;
	}
	
}
