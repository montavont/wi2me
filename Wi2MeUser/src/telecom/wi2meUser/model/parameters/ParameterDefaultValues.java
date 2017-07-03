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

package telecom.wi2meUser.model.parameters;


import java.util.ArrayList;

import telecom.wi2meCore.model.cellCommands.CellDownloader;
import telecom.wi2meCore.model.cellCommands.CellTransferrerContainer;
import telecom.wi2meCore.model.cellCommands.CellUploader;
import telecom.wi2meCore.model.wifiCommands.Pinger;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.WifiDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiUploader;
import telecom.wi2meCore.model.parameters.Parameter;

/**
 * This class contains the static default values for the parameters used between the classes.
 * These default values are used when no other values are found.
 * @author XXX
 *
 */
public class ParameterDefaultValues {
	
	public static final int WIFI_SCAN_INTERVAL = 3000; //in milliseconds
	public static final int NOT_MOVING_TIME = 99999999; //in milliseconds
	public static final boolean WIFI_CONNECTION_ATTEMPT = false;
	public static final boolean WIFI_CONNECTED = false;
	public static final boolean INTERNET_CONNECTED = false;
	public static final int CELL_CONNECTION_DELAY = 5000; //in milliseconds
	public static final boolean CELL_SCANNING = false;
	public static final boolean CELL_CONNECTION_ATTEMPT = false;
	public static final boolean CELL_CONNECTED = false;
	public static final boolean CELL_CONNECTING = false;
	public static final boolean CELL_TRANSFERRING = false;
	public static final boolean CELL_CONTINUE_TRANSFERRING = false;
	public static final boolean FIRST_FIX_WAITING = false;
	public static final boolean RUN_WIFI = true;
	public static final boolean RUN_CELLULAR = true;
	public static final boolean CONNECT_CELLULAR = false;
	public static final int MIN_BATTERY_LEVEL = 20; //percentage
	public static final int PING_PACKETS = 10; //amount
	public static final float PING_DEADLINE = 3f;//in seconds
	public static final float PING_INTERVAL = 0.2f;//in seconds
	public static final boolean NOTIFY_WHEN_WIFI_CONNECTED = true;
	public static final boolean CONNECT_OPEN_NETWORKS = false;
	public static final boolean NOTIFY_SERVICE_STATUS = true;
	public static final int WIFI_UPLOAD_MAX=0;
	public static final int WIFI_DOWNLOAD_MAX=11000000;
	public static final int WIFI_UPLOAD_MIN=0;
	public static final int WIFI_DOWNLOAD_MIN=9000000;
	public static final int CELL_UPLOAD_MAX=0;
	public static final int CELL_DOWNLOAD_MAX=0;
	public static final int CELL_UPLOAD_MIN=0;
	public static final int CELL_DOWNLOAD_MIN=0;
	public static final boolean SENSOR_ONLY=false;
	public static final int WIFI_THRESHOLD=-85;
	public static final boolean USE_GPS_POSITION = false;
	public static final boolean ALLOW_TRACE_CONNECTIONS = false;
	public static final boolean ALLOW_UPLOAD_TRACES = false;
	public static final int CONNECTIVITY_CHECK_FREQUENCY = 60000;//1 minute
	public static final boolean PERFORM_CONNECTIVITY_CHECK = true;
	public static final boolean IS_FIRST_LOOP = false;
	public static final String REMOTE_UPLOAD_DIRECTORY = "wi2me/";
	public static final String SERVER_IP = "192.108.119.26";
	public static final String DOWNLOAD_URL = "192.108.119.26";
	public static final String DOWNLOAD_PATH = "/mnt/vg1/volume1/wi2me/";
	public static final String UPLOAD_URL = "192.108.119.26";
	public static final String UPLOAD_SCRIPT = "/cgi-bin/upload.cgi";
	public static final String CONNECTION_CHECK_URL = "http://192.108.119.26/mnt/vg1/volume1/wi2me/page.html";
	public static final String WI2ME_DIRECTORY = "/Wi2MeUser/";
	public static final String MONITORED_INTERFACES = "wlan0 eth0 rmnet0";
	public static final String SPDY_PAGE = "";
	public static final String WEB_PAGE = "";
	public static final int WEB_THREADS = 1;
	public static final boolean LOCK_NETWORK = true;

	public static final String BSSID_RESTRICTION_PATH = "";
	public static final boolean IPC_RECONNECTION_NEEDED = false;

	/**
	 * Gives the default value of the given parameter.
	 * @param type
	 * @return The default value
	 */
	public static Object getDefaultValue(Parameter type) {
		switch (type){
		case MIN_BATTERY_LEVEL:
			return new Integer(MIN_BATTERY_LEVEL);
		case RUN_WIFI:
			return new Boolean(RUN_WIFI);
		case RUN_CELLULAR:
			return new Boolean(RUN_CELLULAR);
		case CONNECT_CELLULAR:
			return new Boolean(CONNECT_CELLULAR);
		case FIRST_FIX_WAITING:
			return new Boolean(FIRST_FIX_WAITING);
		case WIFI_SCAN_INTERVAL:
			return new Integer(WIFI_SCAN_INTERVAL);
		case NOT_MOVING_TIME:
			return new Integer(NOT_MOVING_TIME);
		case WIFI_CONNECTION_ATTEMPT:
			return new Boolean(WIFI_CONNECTION_ATTEMPT);
		case WIFI_CONNECTED:
			return new Boolean(WIFI_CONNECTED);
		case PING_PACKETS:
			return new Integer(PING_PACKETS);
		case PING_DEADLINE:
			return new Float(PING_DEADLINE);
		case PING_INTERVAL:
			return new Float(PING_INTERVAL);
		case COMMUNITY_NETWORK_CONNECTED:
			return new Boolean(INTERNET_CONNECTED);
		case CELL_CONNECTION_DELAY:
			return new Integer(CELL_CONNECTION_DELAY);
		case CELL_SCANNING:
			return new Boolean(CELL_SCANNING);
		case CELL_CONNECTION_ATTEMPT:
			return new Boolean(CELL_CONNECTION_ATTEMPT);
		case CELL_CONNECTED:
			return new Boolean(CELL_CONNECTED);
		case CELL_CONNECTING:
			return new Boolean(CELL_CONNECTING);
		case CELL_TRANSFERRING:
			return new Boolean(CELL_TRANSFERRING);
		case CELL_CONTINUE_TRANSFERRING:
			return new Boolean(CELL_CONTINUE_TRANSFERRING);
		case CELL_TRANSFER_COMMANDS:
			//this default value is a container with empty lists of uploaders and downloaders
			return new CellTransferrerContainer(new ArrayList<CellUploader>(), new ArrayList<CellDownloader>());
		case WIFI_TRANSFER_COMMANDS:
			//this default value is a container with empty lists of uploaders and downloaders
			return new WifiTransferrerContainer(new ArrayList<WifiUploader>(), new ArrayList<WifiDownloader>(), new ArrayList<Pinger>());
		case NOTIFY_WHEN_WIFI_CONNECTED:
			return new Boolean(NOTIFY_WHEN_WIFI_CONNECTED);
		case CONNECT_TO_OPEN_NETWORKS:
			return new Boolean(CONNECT_OPEN_NETWORKS);
		case NOTIFY_SERVICE_STATUS:
			return new Boolean(NOTIFY_SERVICE_STATUS);
		case SENSOR_ONLY:
			return new Boolean(SENSOR_ONLY);
		case WIFI_THRESHOLD:
			return new Integer(WIFI_THRESHOLD);
		case USE_GPS_POSITION:
			return new Boolean(USE_GPS_POSITION);
		case ALLOW_TRACE_CONNECTIONS:
			return new Boolean(ALLOW_TRACE_CONNECTIONS);
		case ALLOW_UPLOAD_TRACES:
			return new Boolean(ALLOW_UPLOAD_TRACES);
		case CONNECTIVITY_CHECK_FREQUENCY:
			return new Integer(CONNECTIVITY_CHECK_FREQUENCY);
		case PERFORM_CONNECTIVITY_CHECK:
			return new Boolean(PERFORM_CONNECTIVITY_CHECK);
		case IS_FIRST_LOOP:
			return new Boolean(IS_FIRST_LOOP);
		case REMOTE_UPLOAD_DIRECTORY:
			return new String(REMOTE_UPLOAD_DIRECTORY);
		case SERVER_IP:
			return new String(SERVER_IP);
		case DOWNLOAD_URL:
			return new String(DOWNLOAD_URL);
		case DOWNLOAD_PATH:
			return new String(DOWNLOAD_PATH);
		case UPLOAD_URL:
			return new String(UPLOAD_URL);
		case UPLOAD_SCRIPT:
			return new String(UPLOAD_SCRIPT);
		case CONNECTION_CHECK_URL:
			return new String(CONNECTION_CHECK_URL);
		case WI2ME_DIRECTORY:
			return new String(WI2ME_DIRECTORY);
		case IPC_RECONNECTION_NEEDED:
			return new Boolean(IPC_RECONNECTION_NEEDED);
		case LOCK_NETWORK:
			return new Boolean(LOCK_NETWORK);
		case MONITORED_INTERFACES:
			return new String(MONITORED_INTERFACES);
		default:
			return null;
		}
	}

}
