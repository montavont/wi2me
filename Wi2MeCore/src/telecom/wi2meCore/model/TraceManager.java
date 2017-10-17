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
import telecom.wi2meCore.controller.services.trace.IBatteryLevelReceiver;
import telecom.wi2meCore.model.Flag;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.entities.Trace;
import telecom.wi2meCore.model.entities.Trace.TraceType;
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
				firstFixed = false;
			}

			batLevelReceiver = (new TraceManager()).new BatteryLevelReceiver();
			ControllerServices.getInstance().getBattery().registerLevelReceiver(batLevelReceiver);
		}

	}

	/**
	 * Informs if the TraceManager has able to obtain the first fix of the current location for tha trace
	 * @return true if the first fix of the current location has been obtained, false in other case
	 */

	public static boolean isFirstFixed(){
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
}
