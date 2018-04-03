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

package telecom.wi2meCore.controller.services.trace;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.entities.LocationEvent;
import telecom.wi2meCore.model.entities.Trace;
import telecom.wi2meCore.controller.services.ControllerServices;

import android.annotation.SuppressLint; //TKE TODO Remove and handle refiusal below


public class LocationService implements ILocationService{

	private static final int TIME_INTERVAL = 3000;
	private static final int MAX_METERS = 0;

	private LocationManager locationManager;
	private ServiceLocationListener listener;
	private Context context;
	private Location currentLocation;

	public LocationService(Context context){
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		listener = new ServiceLocationListener();
		context = context;
	}


	@Override
	@SuppressLint("MissingPermission") //TODO TKE Remove and handle refusale
	public void monitorLocation()
	{
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(true);
		criteria.setSpeedRequired(true);

		locationManager.requestLocationUpdates( TIME_INTERVAL, MAX_METERS, criteria, listener, null);

	}

	@Override
	public void unMonitorLocation()
	{
		locationManager.removeUpdates(listener);
	}

	@Override
	public void finalizeService() {
		unMonitorLocation();
	}

	@Override
	public synchronized Location getLocation()
	{
		return currentLocation;
	}

	@Override
	public void setLocation(Location location)
	{
		synchronized(this){
			currentLocation = location;
		}
	}

	@Override
	public boolean isGPSEnabled() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	@Override
	public boolean isNetworkLocationEnabled() {
		return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	private class ServiceLocationListener implements LocationListener {

		public ServiceLocationListener(){//ILocationReceiver receiver){
			//this.receiver = receiver;
		}

		@Override
		public void onLocationChanged(Location location) {

			  Logger.getInstance().log(LocationEvent.getNewLocationEvent(TraceManager.getTrace() ,location));
			  ControllerServices.getInstance().getLocation().setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// DO NOT IMPLEMENT
		}

		@Override
		public void onProviderEnabled(String provider) {
			// DO NOT IMPLEMENT
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// DO NOT IMPLEMENT
		}

	}
}
