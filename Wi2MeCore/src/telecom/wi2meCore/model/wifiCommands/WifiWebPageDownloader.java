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


import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.configuration.Timers;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.DownloadingFailException;
import telecom.wi2meCore.controller.services.exceptions.DownloadingInterruptedException;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.controller.services.web.WebService;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.Utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.util.HashMap;


/**
 * Wireless network command used to download a file using the wifi network.
 * @author XXX
 *
 */
public class WifiWebPageDownloader extends WirelessNetworkCommand
{

	private static String URL_KEY = "page";
	private static String THREADS_KEY = "threads";
	private static String TAG_KEY = "tag";

	private String url;
	private int numThreads;
	private String tag;	

	public WifiWebPageDownloader(HashMap<String, String> params)
	{

		this.url = params.get(URL_KEY);
		this.numThreads = Integer.parseInt(params.get(THREADS_KEY));
		this.tag = params.get(TAG_KEY);
	}

	public WifiWebPageDownloader(String url, int numThreads)
	{
		this.url = url;
		this.numThreads = numThreads;
		this.tag = "WEB";
	}

	@Override
	public void initializeCommand(IParameterManager parameters)
	{
	}

	@Override
	public void finalizeCommand(IParameterManager parameters)
	{
	}

	@Override
	public void run(IParameterManager parameters)
	{
		WifiBytesTransferedReceiver receiver = null;
		WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
	
		if (url.length() > 0 && (Boolean)parameters.getParameter(Parameter.COMMUNITY_NETWORK_CONNECTED))
		{
			if (ControllerServices.getInstance().getWifi().isConnected())
			{

				Object connectedObj = parameters.getParameter(Parameter.WIFI_CONNECTED);
				Object connectedToObj = parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);
				if (connectedObj != null && connectedToObj != null)
				{
					if ((Boolean)connectedObj)
					{
						if (ControllerServices.getInstance().getWifi().isConnected() && info != null)
						{
							receiver = new WifiBytesTransferedReceiver(Utils.TYPE_DOWNLOAD, parameters);
						}
					}
				}	

				ControllerServices.getInstance().getWeb().downloadWebPage(url, WebService.Route.WIFI, numThreads, receiver, tag);
			}
		}
	}
}
