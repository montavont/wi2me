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
import telecom.wi2meCore.controller.services.ITimeService;
import telecom.wi2meCore.controller.services.ble.IBLEService;
import telecom.wi2meCore.controller.services.cell.ICellService;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.move.IMoveService;
import telecom.wi2meCore.controller.services.trace.IBatteryService;
import telecom.wi2meCore.controller.services.trace.ILocationService;
import telecom.wi2meCore.controller.services.web.IWebService;
import telecom.wi2meCore.controller.services.wifi.IWifiService;

/**
 * This class regroups all the services. Can be accessed in a static way.
 * Services are used by all the other classes.
 * @author XXX
 */
public class ControllerServices implements IControllerServices {

	private static final String NULL_INSTANCE = "The service needs to be initialized first.";

	private ITimeService time;
	private IBLEService ble;
	private ICellService cell;
	private IMoveService move;
	private IWebService web;
	private IWifiService wifi;
	private IBatteryService battery;
	private ILocationService location;
	private IAssetServices assets;
	private INotificationServices notification;
	private ICommunityNetworkService community;

	@Override
	public INotificationServices getNotification() {
		return notification;
	}

	@Override
	public IAssetServices getAssets() {
		return assets;
	}

	@Override
	public ITimeService getTime() {
		return time;
	}

	@Override
	public ICellService getCell() {
		return cell;
	}

	@Override
	public IMoveService getMove() {
		return move;
	}

	@Override
	public IWebService getWeb() {
		return web;
	}

	@Override
	public IBLEService getBLE() {
		return ble;
	}

	@Override
	public IWifiService getWifi() {
		return wifi;
	}

	@Override
	public IBatteryService getBattery() {
		return battery;
	}

	@Override
	public ILocationService getLocation() {
		return location;
	}

	@Override
	public ICommunityNetworkService getCommunity() {
		return community;
	}

	private static ControllerServices instance;

	/**
	 * The constructor is private.
	 */
	private ControllerServices(){

	}

	/**
	 * Initialize the instance of ControllerServices and links all the services to it.
	 * @param time
	 * @param cell
	 * @param move
	 * @param web
	 * @param wifi
	 * @param battery
	 * @param location
	 * @param assets
	 * @param notification
	 * @param community
	 */
	public static void initializeServices(ITimeService time, ICellService cell, IMoveService move, IWebService web, IWifiService wifi,
			IBatteryService battery, ILocationService location, IAssetServices assets, INotificationServices notification, ICommunityNetworkService community, IBLEService ble){
		instance = new ControllerServices();
		instance.time = time;
		instance.cell = cell;
		instance.move = move;
		instance.web = web;
		instance.wifi = wifi;
		instance.battery = battery;
		instance.location = location;
		instance.assets = assets;
		instance.notification = notification;
		instance.community = community;
		instance.ble = ble;
	}

	/**
	 * Gives the only instance of ControllerServices.
	 * If it doesn't exist, it throws an exception (IllegalStateException).
	 * @return ControllerServices
	 */
	public static IControllerServices getInstance(){
		if (instance == null)
			throw new IllegalStateException(NULL_INSTANCE);
		return instance;
	}

	/**
	 * Calls the finalize method of the services and removes the instance.
	 */
	public static void finalizeServices() {
		if (instance != null){
			instance.cell.finalizeService();
			instance.move.finalizeService();
			instance.wifi.finalizeService();
			instance.battery.finalizeService();
			instance.location.finalizeService();
		}
		instance = null;
	}

}
