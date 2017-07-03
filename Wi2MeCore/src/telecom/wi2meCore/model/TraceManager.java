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

package telecom.wi2meCore.model;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.persistance.DatabaseHelper.TraceType;
import telecom.wi2meCore.controller.services.trace.IBatteryLevelReceiver;
import telecom.wi2meCore.controller.services.trace.ILocationReceiver;
import telecom.wi2meCore.model.Flag;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.entities.Trace;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

public class TraceManager {
	
	public static final String TRACE_OFF = "GPS_OFF (Device steady - GPS off)";
	public static final String TRACE_ON = "GPS_ON (Device moving - GPS on)";
	
	public static final String BATTERY_LEVEL_EVENT = "NEW_BATTERY_LEVEL";
	public static final String LOCATION_EVENT = "NEW_LOCATION";
	
	private static TraceImpl trace;
	private static final Object traceLock = new Object();
	
	private static boolean firstFixed;
	private static IBatteryLevelReceiver batLevelReceiver;
	private static ILocationReceiver locationReceiver;
	private static ILocationReceiver networkLocationReceiver;
	private static Thread motionCheckerThread;
	private static Location lastLocationUsed;
	
	private TraceManager(){		
	}	
	
	private static Trace copy(TraceImpl t){
		synchronized (traceLock) {
			Trace.copy(trace, t);
		}		
		t.setTimestamp(ControllerServices.getInstance().getTime().getCurrentTimeInMilliseconds());	
		return t;
	}

	/**
	 * This method is needed to make sure the Trace is initialized and being updated with the current information about the environment.
	 * This must be run before any other method of this class.
	 * @param parameters the parameters for the motion options to stop refreshing the trace
	 * @param flagWifi the flag that indicates when to stop refreshing the trace
	 */
	public static void initializeManager(IParameterManager parameters){
		initializeTrace();
		motionCheckerThread = (new TraceManager()).new MotionCheckerThread(parameters);
		motionCheckerThread.start();
	}
	

	public static void finalizeManager(){
		ControllerServices.getInstance().getBattery().unregisterLevelReceiver(batLevelReceiver);
		if (motionCheckerThread != null){
			if (motionCheckerThread.isAlive()){
				motionCheckerThread.interrupt();
				try {
					motionCheckerThread.join(5000);
				} catch (InterruptedException e) {
					//should not happen
					Log.d("TraceManager", "++ " + e.getMessage(), e);
				}
			}
		}
		trace = null;
	}
		
	private static void initializeTrace(){
		if (trace == null){
			trace = (new TraceManager()).new TraceImpl();
			synchronized (traceLock) {				
				firstFixed = false/*true*/;
			}
			
			batLevelReceiver = (new TraceManager()).new BatteryLevelReceiver();	
			locationReceiver = (new TraceManager()).new LocationReceiver();
			networkLocationReceiver = (new TraceManager()).new LocationReceiver();
			ControllerServices.getInstance().getBattery().registerLevelReceiver(batLevelReceiver);
			//ControllerServices.getInstance().getLocation().registerLocationReceiver(locationReceiver);

		}
			
	}
	
	/**
	 * Informs if the TraceManager has able to obtain the first fix of the current location for tha trace
	 * @return true if the first fix of the current location has been obtained, false in other case
	 */
	
	public static boolean isFirstFixed(){
		//initializeTrace(); In case the Trace has not been initialized before (SHOULD NEVER HAPPEN!)
		synchronized (traceLock) {
			return firstFixed;
		}
	}
	
	
	/**
	 * This is the method that allows to get a copy of the current trace.
	 * @return Returns a copy of the trace in its current state
	 */
	public static Trace getTrace(){
		initializeTrace();
		TraceImpl t = (new TraceManager()).new TraceImpl();
		return copy(t);		
	}
	
	
	/**
	 * Allows to instantiate a trace with all the parameters needed. This will only be necessary if a stored trace wants to be recovered.
	 * @param timestamp The current time in milliseconds
	 * @param altitude The current altitude of the device
	 * @param longitude The current longitude of the device
	 * @param latitude The current latitude of the device
	 * @param accuracy The accuracy of current positioning measures
	 * @param speed The current speed of the device
	 * @param bearing The bearing of current positioning measures
	 * @param provider The provider of positioning information
	 * @param batteryLevel The current battery level
	 * @return Returns an instance of a trace with all the parameters passed
	 */
	public static Trace getNewTrace(long timestamp, double altitude, double longitude, double latitude, double accuracy, 
									float speed, float bearing, String provider, int batteryLevel){
		//We need to instantiate the outer class (TraceManager) to be able to instantiate the inner class
		TraceImpl ret = (new TraceManager()).new TraceImpl();
		
		ret.setTimestamp(timestamp);
		ret.setAltitude(altitude);
		ret.setLongitude(longitude);
		ret.setLatitude(latitude);
		ret.setAccuracy(accuracy);
		ret.setSpeed(speed);
		ret.setBearing(bearing);
		ret.setProvider(provider);
		ret.setBatteryLevel(batteryLevel);
		return ret;
	}
	
	static private void resetTrace() {
		trace.setAltitude(0);
		trace.setLongitude(0);
		trace.setLatitude(0);
		trace.setAccuracy(0);
		trace.setSpeed(0);
		trace.setBearing(0);
		trace.setProvider(null);
		//trace.setBatteryLevel(0);			
		
	}
	
	private class TraceImpl extends Trace{
		
		
		private void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		private void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		private void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		private void setAltitude(double altitude) {
			this.altitude = altitude;
		}

		private void setAccuracy(double accuracy) {
			this.accuracy = accuracy;
		}

		private void setSpeed(float speed) {
			this.speed = speed;
		}

		private void setBearing(float bearing) {
			this.bearing = bearing;
		}

		private void setProvider(String provider) {
			this.provider = provider;
		}

		private void setBatteryLevel(int batteryLevel) {
			this.batteryLevel = batteryLevel;
		}

		/*@Override
		public long save() {
			// FOR THE CHILDREN TO IMPLEMENT
			return -1;
		}*/

		@Override
		public TraceType getStoringType() {
			// RETURNS NULL BECAUSE IT DOES NOT GET STORED
			return null;
		}
		
	}
	
	private class BatteryLevelReceiver implements IBatteryLevelReceiver{

		@Override
		public void receiveBatteryLevel(int batteryLevel) {
			synchronized (traceLock) {
				if (trace != null){
					trace.setBatteryLevel(batteryLevel);
					Logger.getInstance().log(ExternalEvent.getNewExternalEvent(getTrace(), BATTERY_LEVEL_EVENT));
				}
			}
		}
		
	}
	
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	private class LocationReceiver implements ILocationReceiver{

		@Override
		public void receiveLocation(Location location) {
			synchronized (traceLock) {
				if (trace != null){
					if (isBetterLocation(location, lastLocationUsed)){
						trace.setAltitude(location.getAltitude());
						trace.setLongitude(location.getLongitude());
						trace.setLatitude(location.getLatitude());
						trace.setAccuracy(location.getAccuracy());
						trace.setSpeed(location.getSpeed());
						trace.setBearing(location.getBearing());
						trace.setProvider(location.getProvider());
						lastLocationUsed = location;
						Logger.getInstance().log(ExternalEvent.getNewExternalEvent(getTrace(), LOCATION_EVENT));
					}
					firstFixed = true;					
				}
				
			}
		}
		
	}	
	/*
	private class NetworkLocationReceiver implements ILocationReceiver{

		@Override
		public void receiveLocation(double altitude, double longitude, double latitude,
									double accuracy, float speed, float bearing, String provider) {
			synchronized (traceLock) {
				if (trace != null){
					if (!firstFixed){
						trace.setAltitude(altitude);
						trace.setLongitude(longitude);
						trace.setLatitude(latitude);
						trace.setAccuracy(accuracy);
						trace.setSpeed(speed);
						trace.setBearing(bearing);
						trace.setProvider(provider);						
					}else{
						ControllerServices.getInstance().getLocation().unregisterLocationReceiver(this);
					}
			
				}
				
			}
		}
		
		
	}
	*/
	private class LocationLooper extends Thread{
		private IParameterManager parameters;
		private Looper myLooper;
		private boolean useGPS;
		public LocationLooper(IParameterManager parameters){
			super();
			this.parameters=parameters;
		}
		public void quitLooper(){
			if (myLooper != null)
				myLooper.quit();
		}
		public void run(){
			useGPS = (Boolean) parameters.getParameter(Parameter.USE_GPS_POSITION);
			Looper.prepare();	
			if(useGPS==true){
				ControllerServices.getInstance().getLocation().registerLocationReceiver(locationReceiver);
			}
			ControllerServices.getInstance().getLocation().registerNetworkLocationReceiver(networkLocationReceiver);
			myLooper = Looper.myLooper();
			Log.d("LOOPER","++ "+"ABOUT TO LOOP");
			Looper.loop();
			Log.d("LOOPER","++ "+"FINISHED LOOP");
			if(useGPS==true){
				ControllerServices.getInstance().getLocation().unregisterLocationReceiver(locationReceiver);
			}
			ControllerServices.getInstance().getLocation().unregisterLocationReceiver(networkLocationReceiver);
		}
	}
	
	private static LocationLooper looper;
	
	private class MotionCheckerThread extends Thread{
		
		private IParameterManager parameters;
		private Flag flagWifi;
		private Flag flagCell;
		
		public MotionCheckerThread(IParameterManager parameters){
			this.parameters = parameters;
			this.flagWifi = (Flag) parameters.getParameter(Parameter.WIFI_WORKING_FLAG);
			this.flagCell = (Flag) parameters.getParameter(Parameter.CELL_WORKING_FLAG);
		}
		
		public void run(){
			looper = (new TraceManager()).new LocationLooper(parameters);
			looper.start();
			int maxTimeSteady = (Integer) parameters.getParameter(Parameter.NOT_MOVING_TIME);
			while (flagWifi.isActive() || flagCell.isActive()){
				try {
					long lastMoveTimestamp = ControllerServices.getInstance().getMove().getLastMovementTimestamp();
					long now = ControllerServices.getInstance().getTime().getCurrentTimeInMilliseconds();
					//we get the difference, to know how much time it has been steady
					long steadyTime = now - lastMoveTimestamp;
					if (steadyTime < maxTimeSteady){//if it was not steady enough time, sleep until it is enough
						Thread.sleep(maxTimeSteady - steadyTime);
					}else{
						looper.quitLooper();
						try {
							looper.join();
						} catch (InterruptedException e) {
							//should not happen
							Log.d(getClass().getSimpleName(), "++ " + e.getMessage(), e);
						};
						//if enough time passed, we unregister the receivers until the device moves again
						//ControllerServices.getInstance().getBattery().unregisterLevelReceiver(batLevelReceiver);
						//ControllerServices.getInstance().getLocation().unregisterLocationReceiver(locationReceiver);
						//ControllerServices.getInstance().getLocation().unregisterLocationReceiver(networkLocationReceiver);
						//we mark the trace as not first fixed, so that when it starts moving again, it is not marked as first fixed until it gets the first broadcast
						synchronized (traceLock) {
							firstFixed = false/*true*/;
						}
						Log.d(getClass().getSimpleName(), "++ "+"Locking until device moves");
						//We log this in our log file
						Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), TRACE_OFF));
						//and we get a lock to wait until it moves (can be interrupted)
						ControllerServices.getInstance().getMove().getMovingLock().waitForMovement();
						
						//we reset the trace, as it needs a new first fix
						synchronized (traceLock) {
							resetTrace();
						}
						Log.d(getClass().getSimpleName(), "++ "+"Device moved! Reregistering receivers for location and battery");
												
						//once it is up again, we just register the receivers again		
						//ControllerServices.getInstance().getBattery().registerLevelReceiver(batLevelReceiver);
						looper = (new TraceManager()).new LocationLooper(parameters);
						looper.start();
						//ControllerServices.getInstance().getLocation().registerLocationReceiver(locationReceiver);
						
						//We log this in our log file
						Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), TRACE_ON));
					}
						
				} catch (InterruptedException e) {
					//if we are interrupted, leave the loop
					break;
				} 
			}
			looper.quitLooper();
			//ControllerServices.getInstance().getBattery().unregisterLevelReceiver(batLevelReceiver);
			try {
				looper.join();
			} catch (InterruptedException e) {
				//should not happen
				Log.d(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			};
		}

	}

}
