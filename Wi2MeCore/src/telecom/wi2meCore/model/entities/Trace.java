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

package telecom.wi2meCore.model.entities;


import telecom.wi2meCore.model.entities.Trace.TraceType;

/**
 * This class represents the Trace itself. Stores information about the environment (time, positioning and battery life), and this is the parent of all other entities.
 * This class is implemented as a singleton, and has a factory method which makes copies of the instance of the class being refreshed each time a change takes place.
 * @author Alejandro
 *
 */
public abstract class Trace {

	public static enum TraceType{
        WIFI_SCAN_RESULT,
        WIFI_CONNECTION_EVENT,
        COMMUNITY_NETWORK_CONNECTION_EVENT,
        WIFI_CONNECTION_DATA,
        WIFI_SNIFFER_DATA,
        WIFI_CONNECTION_INFO,
        BYTES_PER_UID,
        CELL_SCAN_RESULT,
        CELL_CONNECTION_EVENT,
        CELL_CONNECTION_DATA,
        EXTERNAL_EVENT,
        WIFI_PING,
		LOCATION_EVENT,
	};

	/**
	 * Constants for the Database
	 */
	public static final String TRACE_REFERENCE = Trace.TABLE_NAME + "Id";

    public static final String TABLE_NAME = "Trace";
    public static final String TIMESTAMP = "timestamp";
    public static final String ALTITUDE = "altitude";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String ACCURACY = "accuracy";
    public static final String SPEED = "speed";
    public static final String BEARING = "bearing";
    public static final String PROVIDER = "provider";
    public static final String BATT_LEVEL = "batteryLevel";
    public static final String TYPE = "type";

	protected long timestamp;
	protected double latitude;
	protected double longitude;
	protected double altitude;
	protected double accuracy;
	protected float speed;
	protected float bearing;
	protected String provider;
	protected int batteryLevel;

	public double getLatitude() {
		return latitude;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public float getSpeed() {
		return speed;
	}

	public float getBearing() {
		return bearing;
	}

	public String getProvider() {
		return provider;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public double getAltitude() {
		return altitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public int getBatteryLevel() {
		return batteryLevel;
	}

	public abstract TraceType getStoringType();

	/**
	 * Copies the attributes of the Trace from to the Trace to
	 */
	public static void copy(Trace from, Trace to){
		to.altitude = from.altitude;
		to.latitude = from.latitude;
		to.longitude = from.longitude;
		to.accuracy = from.accuracy;
		to.speed = from.speed;
		to.bearing = from.bearing;
		to.provider = from.provider;
		to.batteryLevel = from.batteryLevel;
		to.timestamp = from.timestamp;
	}

	private static final String SEPARATOR = ";";

	/**
	 * The toString method is redefined so that traces can be easily printed
	 */
	public String toString(){
		return "TRACE:" + timestamp + SEPARATOR + altitude + SEPARATOR + longitude + SEPARATOR + latitude + SEPARATOR + accuracy + SEPARATOR + bearing + SEPARATOR + speed + SEPARATOR + provider + SEPARATOR + batteryLevel + SEPARATOR+SEPARATOR;
	}



}
