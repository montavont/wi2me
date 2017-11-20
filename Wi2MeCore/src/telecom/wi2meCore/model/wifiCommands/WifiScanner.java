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
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.ExternalEvent;
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
public class WifiScanner extends WirelessNetworkCommand{

	private static final String WIFI_OFF = "WIFI_OFF (Device Steady - Wifi interface off)";
	private static final String WIFI_ON = "WIFI_ON (Device Moving - Wifi interface on)";

	public WifiScanner() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public WifiScanner(HashMap<String, String> params) {
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
		// DO NOTHING
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING
	}

	@Override
	public void run(IParameterManager parameters) {
		//To change the status of the application in Wi2MeUser
		StatusService.getInstance().changeStatus("Scanning...");

		int scanInterval = (Integer) parameters.getParameter(Parameter.WIFI_SCAN_INTERVAL);
		int maxTimeSteady = (Integer) parameters.getParameter(Parameter.NOT_MOVING_TIME);
		try {
			if (!ControllerServices.getInstance().getWifi().isInterfaceEnabled()){
				try{
					ControllerServices.getInstance().getWifi().enableWithoutNetworks();
					Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), WIFI_ON));
				} catch (TimeoutException e) {
					Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					//If we cannot enable the wifi interface we are unable to continue
					//throw new RuntimeException("FATAL ERROR: Wifi cannot be enabled, unable to continue");
				}
			}

			Thread.sleep(scanInterval);

			try{
				List<ScanResult> results = this.scan();
				if (results != null)
				{
					WifiScanResult wifiScanResult = WifiScanResult.getNewWifiScanResult(TraceManager.getTrace(), getAPs(results));
					parameters.setParameter(Parameter.WIFI_SCAN_RESULT, results);
					Logger.getInstance().log(wifiScanResult);
				}
			}
			catch(TimeoutException e)
			{
				Log.e(getClass().getSimpleName(), "++ "+"Scanning Timeout", e);
			}
		} catch (InterruptedException e) {
			// if we are interrupted, we leave
			Log.d(getClass().getSimpleName(), "++ "+"Scanning Interrupted", e);
		}
	}

	private List<WifiAP> getAPs(List<ScanResult> results) {
		List<WifiAP> ret = new ArrayList<WifiAP>();
		for (ScanResult r : results){
			ret.add(WifiAP.getWifiAPFromScanResult(r));
		}
		return ret;
	}

	public List<ScanResult> scan() throws TimeoutException, InterruptedException{
		return ControllerServices.getInstance().getWifi().scanSynchronously();
	}

}
