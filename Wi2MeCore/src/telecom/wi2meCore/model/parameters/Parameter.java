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

package telecom.wi2meCore.model.parameters;

/**
 * Contains the list of parameters used between the classes.
 * Each entry has a boolean parameter. If true, this parameter should be in the configuration file traces.conf.txt.
 * This boolean is used when creating this parameter file, to know whether the current parameter has to be put in the file or not.
 * @author Milcea
 *
 */
public enum Parameter {
	//The boolean parameter indicated whether this parameter comes from the traces.conf.txt file. Used to generate this file if it doesn't exist
	GENERIC(false),
	WIFI_SCAN_INTERVAL(true),
	WIFI_SCAN_RESULT(false),
	WIFI_CONNECTED(false),
	COMMUNITY_NETWORK_CONNECTED(false),
	WIFI_CONNECTED_TO_AP(false),
	WIFI_THRESHOLD(true), 
	COMMUNITY_NETWORKS(false), 
	LOG_OBSERVER(false),
	NOT_MOVING_TIME(true), 
	COMMUNITY_NETWORK_USERS(false),
	WIFI_USERNAME(false), 
	WIFI_PASSWORD(false), 
	WIFI_PLUGIN_SCRIPT(false), 
	CELL_SCANNING(false),	
	CELL_CONNECTION_DELAY(true), 
	CELL_CONNECTED(false), 
	CELL_CONNECTING(false),
	CELL_TRANSFERRING(false), 
	CELL_CONNECTION_ATTEMPT(false), 
	WIFI_WORKING_FLAG(false), 
	CELL_WORKING_FLAG(false),
	WIFI_CONNECTION_ATTEMPT(false), 
	CELL_TRANSFER_COMMANDS(false),
	FIRST_FIX_WAITING(true), 
	RUN_WIFI(true), 
	RUN_CELLULAR(true), 
	MIN_BATTERY_LEVEL(true),
	PING_PACKETS(true),
	PING_DEADLINE(true),
	PING_INTERVAL(true),
	CONNECT_CELLULAR(true), 
	WIFI_TRANSFER_COMMANDS(false), 
	PING_SERVER_IP(false), 
	CELL_CONTINUE_TRANSFERRING(false), 
	NOTIFY_WHEN_WIFI_CONNECTED(true), 
	CONNECT_TO_OPEN_NETWORKS(true),
	SENSOR_ONLY(true),
	NOTIFY_SERVICE_STATUS(true),
	USE_GPS_POSITION(true),
	AP_GRADE_MAP(false),
	ALLOW_TRACE_CONNECTIONS(true),
	ALLOW_UPLOAD_TRACES(true),
	CONNECTIVITY_CHECK_FREQUENCY(true),
	PERFORM_CONNECTIVITY_CHECK(true),
	IS_FIRST_LOOP(false),
	REMOTE_UPLOAD_DIRECTORY(true),
	SERVER_IP(true),
	DOWNLOAD_URL(true),
	DOWNLOAD_PATH(true),
	UPLOAD_URL(true),
	UPLOAD_SCRIPT(true),
	CONNECTION_CHECK_URL(true),
	MONITORED_INTERFACES(true),
	WI2ME_DIRECTORY(false),
	LOCK_NETWORK(true),
	COMMAND_FILE(true),
	IPC_RECONNECTION_NEEDED(false),
	STORAGE_TYPE(true);

	private final boolean value;

	private Parameter(boolean value) {
		this.value = value;
	}

	public boolean isInConfFile() {
		return this.value;
	}
}
