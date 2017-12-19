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

package telecom.wi2meRecherche.model.parameters;

import android.os.Environment; 

import java.util.ArrayList;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.model.cellCommands.CellDownloader;
import telecom.wi2meCore.model.cellCommands.CellTransferrerContainer;
import telecom.wi2meCore.model.cellCommands.CellUploader;
import telecom.wi2meCore.model.wifiCommands.Pinger;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.WifiDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiUploader;
import telecom.wi2meCore.model.parameters.Parameter;



public class ParameterDefaultValues {
	
	public static final int WIFI_SCAN_INTERVAL = 3000; //in milliseconds
	public static final int NOT_MOVING_TIME = 120000; //in milliseconds
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
	public static final int MIN_BATTERY_LEVEL = 5; //percentage
	public static final int PING_PACKETS = 10; //amount
	public static final float PING_DEADLINE = 3f;//in seconds
	public static final float PING_INTERVAL = 0.2f;//in seconds
	public static final boolean NOTIFY_WHEN_WIFI_CONNECTED = true;
	public static final boolean CONNECT_OPEN_NETWORKS = false;
	public static final boolean NOTIFY_SERVICE_STATUS = true;
	public static final boolean SENSOR_ONLY=false;
	public static final int WIFI_THRESHOLD=-85;
	public static final boolean USE_GPS_POSITION = true;
	public static final boolean ALLOW_TRACE_CONNECTIONS = true;
	public static final boolean ALLOW_UPLOAD_TRACES = true;
	public static final int CONNECTIVITY_CHECK_FREQUENCY = 60000;//1 minute
	public static final boolean PERFORM_CONNECTIVITY_CHECK = true;
	public static final boolean IS_FIRST_LOOP = false;
	public static final boolean LOCK_NETWORK = true;
	public static final String REMOTE_UPLOAD_DIRECTORY = "wi2me/";
	public static final String SERVER_IP = "192.108.119.26";
	//public static final String CONNECTION_CHECK_URL = "http://192.108.119.26/mnt/vg1/volume1/wi2me/page.html";
	public static final String CONNECTION_CHECK_URL = "https://slashdot.org";
	public static final String WI2ME_DIRECTORY = "/Wi2MeRecherche/";
	public static final String MONITORED_INTERFACES = "wlan0";
	public static final int STORAGE_TYPE = 1;
	
	public static Object getDefaultValue(Parameter type)	
	{
		switch (type){
		case MIN_BATTERY_LEVEL:
			return Integer.valueOf(MIN_BATTERY_LEVEL);
		case RUN_WIFI:
			return Boolean.valueOf(RUN_WIFI);
		case RUN_CELLULAR:
			return Boolean.valueOf(RUN_CELLULAR);
		case CONNECT_CELLULAR:
			return Boolean.valueOf(CONNECT_CELLULAR);
		case FIRST_FIX_WAITING:
			return Boolean.valueOf(FIRST_FIX_WAITING);
		case WIFI_SCAN_INTERVAL:
			return Integer.valueOf(WIFI_SCAN_INTERVAL);
		case NOT_MOVING_TIME:
			return Integer.valueOf(NOT_MOVING_TIME);
		case WIFI_CONNECTION_ATTEMPT:
			return Boolean.valueOf(WIFI_CONNECTION_ATTEMPT);
		case WIFI_CONNECTED:
			return Boolean.valueOf(WIFI_CONNECTED);
		case PING_PACKETS:
			return Integer.valueOf(PING_PACKETS);
		case PING_DEADLINE:
			return Float.valueOf(PING_DEADLINE);
		case PING_INTERVAL:
			return Float.valueOf(PING_INTERVAL);
		case COMMUNITY_NETWORK_CONNECTED:
			return Boolean.valueOf(INTERNET_CONNECTED);
		case CELL_CONNECTION_DELAY:
			return Integer.valueOf(CELL_CONNECTION_DELAY);
		case CELL_SCANNING:
			return Boolean.valueOf(CELL_SCANNING);
		case CELL_CONNECTION_ATTEMPT:
			return Boolean.valueOf(CELL_CONNECTION_ATTEMPT);
		case CELL_CONNECTED:
			return Boolean.valueOf(CELL_CONNECTED);
		case CELL_CONNECTING:
			return Boolean.valueOf(CELL_CONNECTING);
		case CELL_TRANSFERRING:
			return Boolean.valueOf(CELL_TRANSFERRING);
		case CELL_CONTINUE_TRANSFERRING:
			return Boolean.valueOf(CELL_CONTINUE_TRANSFERRING);
		case CELL_TRANSFER_COMMANDS:
			//this default value is a container with empty lists of uploaders and downloaders
			return new CellTransferrerContainer(new ArrayList<CellUploader>(), new ArrayList<CellDownloader>());
		case WIFI_TRANSFER_COMMANDS:
			//this default value is a container with empty lists of uploaders and downloaders
			return new WifiTransferrerContainer(new ArrayList<WifiUploader>(), new ArrayList<WifiDownloader>(), new ArrayList<Pinger>());
		case NOTIFY_WHEN_WIFI_CONNECTED:
			return Boolean.valueOf(NOTIFY_WHEN_WIFI_CONNECTED);
		case CONNECT_TO_OPEN_NETWORKS:
			return Boolean.valueOf(CONNECT_OPEN_NETWORKS);
		case NOTIFY_SERVICE_STATUS:
			return Boolean.valueOf(NOTIFY_SERVICE_STATUS);
		case SENSOR_ONLY:
			return Boolean.valueOf(SENSOR_ONLY);
		case WIFI_THRESHOLD:
			return Integer.valueOf(WIFI_THRESHOLD);
		case USE_GPS_POSITION:
			return Boolean.valueOf(USE_GPS_POSITION);
		case ALLOW_TRACE_CONNECTIONS:
			return Boolean.valueOf(ALLOW_TRACE_CONNECTIONS);
		case ALLOW_UPLOAD_TRACES:
			return Boolean.valueOf(ALLOW_UPLOAD_TRACES);
		case CONNECTIVITY_CHECK_FREQUENCY:
			return Integer.valueOf(CONNECTIVITY_CHECK_FREQUENCY);
		case PERFORM_CONNECTIVITY_CHECK:
			return Boolean.valueOf(PERFORM_CONNECTIVITY_CHECK);
		case IS_FIRST_LOOP:
			return Boolean.valueOf(IS_FIRST_LOOP);
		case REMOTE_UPLOAD_DIRECTORY:
			return String.valueOf(REMOTE_UPLOAD_DIRECTORY);
		case SERVER_IP:
			return String.valueOf(SERVER_IP);
		case CONNECTION_CHECK_URL:
			return String.valueOf(CONNECTION_CHECK_URL);
		case WI2ME_DIRECTORY:
			return String.valueOf(WI2ME_DIRECTORY);
		case MONITORED_INTERFACES:
			return String.valueOf(MONITORED_INTERFACES);
		case LOCK_NETWORK:
			return Boolean.valueOf(LOCK_NETWORK);
		case COMMAND_FILE:
			return Environment.getExternalStorageDirectory() +  ConfigurationManager.WI2ME_DIRECTORY + ConfigurationManager.JSON_COMMAND_DIRECTORY + "defconf.json";
		case STORAGE_TYPE:
			return Integer.valueOf(STORAGE_TYPE);

		default:
			return null;
		}
	}

}
