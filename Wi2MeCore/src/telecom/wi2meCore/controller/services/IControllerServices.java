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

package telecom.wi2meCore.controller.services;

import telecom.wi2meCore.controller.services.IAssetServices;
import telecom.wi2meCore.controller.services.INotificationServices;
import telecom.wi2meCore.controller.services.IThreadSynchronizingService;
import telecom.wi2meCore.controller.services.ITimeService;
import telecom.wi2meCore.controller.services.cell.ICellService;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.move.IMoveService;
import telecom.wi2meCore.controller.services.trace.IBatteryService;
import telecom.wi2meCore.controller.services.trace.ILocationService;
import telecom.wi2meCore.controller.services.web.IWebService;
import telecom.wi2meCore.controller.services.wifi.IWifiService;

/**
 * Interface of the ControllerServices
 * @author XXX
 *
 */
public interface IControllerServices {

	/**Gives the assets service 
	 * @return IAssetServices
	 */
	IAssetServices getAssets();

	/**Gives the thread synchronizing service 
	 * @return IThreadSynchronizingService
	 */
	IThreadSynchronizingService getSync();

	/**Gives the time service 
	 * @return ITimeService
	 */
	ITimeService getTime();

	/**Gives the cell service 
	 * @return ICellService
	 */
	ICellService getCell();

	/**Gives the move service 
	 * @return IMoveService
	 */
	IMoveService getMove();

	/**Gives the web service 
	 * @return IWebService
	 */
	IWebService getWeb();

	/**Gives the wifi service 
	 * @return IWifiService
	 */
	IWifiService getWifi();

	/**Gives the battery service 
	 * @return IBatteryService
	 */
	IBatteryService getBattery();

	/**Gives the location service 
	 * @return ILocationService
	 */
	ILocationService getLocation();
	
	/**Gives the notification services 
	 * @return INotificationServices
	 */
	INotificationServices getNotification();

	/**Gives the communityNetworks services 
	 * @return ICommunityNetworkService
	 */
	ICommunityNetworkService getCommunity();

}
