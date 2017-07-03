
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

import java.util.ArrayList;
import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.services.ControllerServices;
/*import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;*/
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiScanResult;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.net.wifi.ScanResult;
import android.util.Log;



/**
 * Wireless network command used to scan for wifi AP.
 * @author XXX
 *
 */
public class WifiPassiveScanner extends WirelessNetworkCommand
{
	long scanTimestamp = -1;


	public WifiPassiveScanner(HashMap<String, String> params) 
	{

	}
	
	@Override
	public void initializeCommand(IParameterManager parameters)
	{
		// DO NOTHING
	}

	@Override
	public void finalizeCommand(IParameterManager parameters)
	{
		// DO NOTHING
	}

	@Override
	public void run(IParameterManager parameters)
	{
		int scanInterval = (Integer) parameters.getParameter(Parameter.WIFI_SCAN_INTERVAL);
	
		long lastScanTimestamp = ControllerServices.getInstance().getWifi().getScanResultTimestamp();
	
		if (lastScanTimestamp != this.scanTimestamp)
		{
			this.scanTimestamp = lastScanTimestamp;
			List<WifiAP> APs = new ArrayList<WifiAP>();
			List<ScanResult> scanResultList = ControllerServices.getInstance().getWifi().getScanResults();
			
			for (ScanResult r : scanResultList)
			{
				APs.add(WifiAP.getWifiAPFromScanResult(r));
			}

			
			WifiScanResult wifiScanResult = WifiScanResult.getNewWifiScanResult(TraceManager.getTrace(), APs);
			parameters.setParameter(Parameter.WIFI_SCAN_RESULT, scanResultList);
			Logger.getInstance().log(wifiScanResult);

		}

		try
		{
			Thread.sleep(scanInterval);
		
		} catch (InterruptedException e) {
			// if we are interrupted, we leave
			Log.d(getClass().getSimpleName(), "++ "+"Post Passive Scanning wait Interrupted", e);
		}

	}

}	
