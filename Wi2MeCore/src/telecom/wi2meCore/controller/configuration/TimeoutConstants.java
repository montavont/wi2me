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

package telecom.wi2meCore.controller.configuration;

/**Contains all the static timeouts (interface enabling, scanning, connecting, disconnecting, download, upload) 
 * @author XXX
 */
public final class TimeoutConstants {
	//in milliseconds
	public static final int CELL_CONNECTION_CHANGE_TIMEOUT = 40000;
	public static final int COMMUNITY_NETWORK_CONNECTION_TIMEOUT = 20000;
	public static final int WIFI_INTERFACE_ENABLING_TIMEOUT = 10000;
	public static final int WIFI_INTERFACE_DISABLING_TIMEOUT = 10000;
	public static final int WIFI_SCANNING_TIMEOUT = 10000; 
	public static final int WIFI_CONNECTING_TIMEOUT = 15000; 
	public static final int WIFI_DISCONNECTING_TIMEOUT = 30000;
	
	public static final int WIFI_DOWNLOAD_SOCKET_TIMEOUT = 10000;
	public static final int WIFI_DOWNLOAD_CONNECT_TIMEOUT = 10000; 
	public static final int WIFI_UPLOAD_SOCKET_TIMEOUT = 10000;
	public static final int WIFI_UPLOAD_CONNECT_TIMEOUT = 10000;
	public static final int CELL_DOWNLOAD_SOCKET_TIMEOUT = 10000;
	public static final int CELL_DOWNLOAD_CONNECT_TIMEOUT = 10000;	
	public static final int CELL_UPLOAD_SOCKET_TIMEOUT = 10000;
	public static final int CELL_UPLOAD_CONNECT_TIMEOUT = 10000;
}
