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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationService implements ILocationService{

	private static final int TIME_INTERVAL = 3000;
	private static final int MAX_METERS = 0;

	private LocationManager locationManager;
	private List<LocationListener> locationListeners;

	public LocationService(Context context){
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationListeners = new ArrayList<LocationListener>();
	}

	@Override
	public void registerLocationReceiver(ILocationReceiver receiver, int timeInterval, int maxMeters) {
		synchronized (this){
			LocationListener listener = new ServiceLocationListener(receiver);
			locationListeners.add(listener);
			locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, timeInterval, maxMeters, listener);
		}
	}

	@Override
	public void registerLocationReceiver(ILocationReceiver receiver) {
		registerLocationReceiver(receiver, TIME_INTERVAL, MAX_METERS);
	}


	@Override
	public void registerNetworkLocationReceiver(ILocationReceiver receiver) {
		synchronized (this){
			LocationListener listener = new ServiceLocationListener(receiver);
			locationListeners.add(listener);

			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			{
				locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, TIME_INTERVAL, MAX_METERS, listener);
			}
			else
			{
				locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, TIME_INTERVAL, MAX_METERS, listener);
			}
		}
	}

	@Override
	public void unregisterLocationReceiver(ILocationReceiver receiver) {
		synchronized (this){
			try{
				int index = locationListeners.indexOf(new ServiceLocationListener(receiver));
				if (index != -1){ //listener found
					LocationListener toUnregister = locationListeners.remove(index);
					locationManager.removeUpdates(toUnregister);
				}else{
					Log.w(getClass().getSimpleName(), "++ "+"Location Listener to unregister, not registered.");
				}

	    	}catch(Exception e){
	    		Log.e("Location", "Unregistering Location Receiver " + e.getMessage(), e);
	    	}
		}

	}


	@Override
	public void finalizeService() {
		//if there is any listener that was forgotten to remove, this will do
		for (LocationListener l : this.locationListeners){
			locationManager.removeUpdates(l);
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

		private ILocationReceiver receiver;

		public ServiceLocationListener(ILocationReceiver receiver){
			this.receiver = receiver;
		}

		@Override
		public boolean equals(Object o) {
			ServiceLocationListener other = (ServiceLocationListener) o;
			return receiver == other.receiver;
		}

		@Override
		public void onLocationChanged(Location location) {
			/*
			  double alt = location.getAltitude();
	          double lat = location.getLatitude();
	          double lng = location.getLongitude();
	          float speed = location.getSpeed();
	          float acc = location.getAccuracy();
	          float bear = location.getBearing();
	          String prov = location.getProvider();
	          */
	          this.receiver.receiveLocation(location);
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
