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

import java.io.File; 
import java.io.BufferedReader; 
import java.io.InputStreamReader; 
import java.io.FileInputStream; 

import java.util.ArrayList; 
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
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
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Logger;
import android.net.wifi.ScanResult;
import android.util.Log;




public class WifiBestRssiSelector extends WirelessNetworkCommand{

	private static final String SYNCHRONIZATION = "WIFI_CELL_SYNCHRONIZATION_FINISHED";
	private static final String CONNECTING_TIMEOUT = "TIMEOUT";

	private IParameterManager parameters;
	private String bssidRestrictionPath = "";

	private static final String BSSID_RESTRICTION_PATH_KEY = "bssid_file";

	public WifiBestRssiSelector(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
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
		Object thresholdObj = parameters.getParameter(Parameter.WIFI_THRESHOLD);
		Boolean connected = false;

		File restrictFile = new File(bssidRestrictionPath);
		ArrayList<String> restrictedBssids = new ArrayList<String>();

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
			Log.d(getClass().getSimpleName(), "++ bssidRestrictionFile " + bssidRestrictionPath + " does not exist");
		}
		catch (java.io.IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ Error reading bssid restriction file " + bssidRestrictionPath);
		}

		if (resultsObj != null && thresholdObj != null)
		{
			int trsh = (Integer)thresholdObj;
			List<ScanResult> results = (List<ScanResult>) resultsObj;
			WifiTransferrerContainer transferrers = (WifiTransferrerContainer) parameters.getParameter(Parameter.WIFI_TRANSFER_COMMANDS);
			Collections.sort(results, new APComparator());
			Log.d(getClass().getSimpleName(), "++ " + results.toString());
			for (ScanResult r : results)
			{
				if (r.level >= trsh)
				{

						//We inform that we will attempt to connect (this helps synchronization with the cell thread)
						parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, true);
						if (restrictedBssids.size() == 0 || restrictedBssids.contains(r.BSSID))
						{

							connected = true;
							//We keep a reference of the AP we are connected to
							parameters.setParameter(Parameter.WIFI_CONNECTED_TO_AP, r);
							break;
						} 

					}
			}

		}
		parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, connected);
		parameters.setParameter(Parameter.WIFI_CONNECTED, connected);

	}

	private class APComparator implements Comparator<ScanResult>
	{

		public APComparator()
		{
		}

		@Override
		public int compare(ScanResult res1, ScanResult res2)
		{
			return res2.level - res1.level; 
		}
	}
}
