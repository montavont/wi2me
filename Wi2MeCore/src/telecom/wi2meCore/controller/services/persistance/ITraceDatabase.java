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

package telecom.wi2meCore.controller.services.persistance;

import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.model.entities.*;

/**
 * Interface to implement for a database storing traces
 * @author Alejandro
 *
 */
public interface ITraceDatabase {
		
	/**
	 * Drops and recreates the database tables
	 */
	void resetTables();
	/**
	 * Closes the database. This method must be run before finishing the application's execution.
	 */
	void closeDatabase();

	/**
	 * Stores the Wifi scan results in the database
	 * @param result Wifi Scan results to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveWifiScanResult(WifiScanResult result);
	/**
	 * Stores the Connection events to a Wifi network in the database
	 * @param wifiConnectionEvent event captured to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveWifiConnectionEvent(WifiConnectionEvent wifiConnectionEvent);
	/**
	 * Stores the Connection event to a community network, using a certain username
	 * @param communityNetworkConnectionEvent The event to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveCommunityNetworkConnectionEvent(CommunityNetworkConnectionEvent communityNetworkConnectionEvent);
	/**
	 * Stores the information about data transferred within a Wifi Internet Connection
	 * @param wifiConnectionData The information of bytes uploaded and downloaded, and ip address to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveWifiConnectionData(WifiConnectionData wifiConnectionData);
	/**
	 * Stores the event regarding a cellular Internet connection taking place
	 * @param cellularConnectionEvent The event to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveWifiSnifferData(WifiSnifferData wifiSnifferData);
	/**
	 * Stores level 2 traffic information within a Wifi Internet Connection
	 * @param WifiSnifferData The level 2 traffic information to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveWifiConnectionInfo(WifiConnectionInfo wifiConnectionInfo);
	/**
	 * Stores level 3 traffic information within a Wifi Internet Connection
	 * @param WifiConnectionInfo The level 3 traffic information to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	
	//long saveBytesperUid(BytesperUid bytesperUid);
	/**
	 * Stores application traffic information within a Wifi Internet Connection
	 * @param WifiConnectionInfo The application traffic information to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	
	//long saveCellularConnectionEvent(CellularConnectionEvent cellularConnectionEvent);
	/**
	 * Stores the information about data transferred within a Cellular Internet Connection
	 * @param cellularConnectionData The information about the transfer to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveCellularConnectionData(CellularConnectionData cellularConnectionData);
	/**
	 * Stores the information about the current cellular network and its neighbors
	 * @param cellularScanResult The current cell and neighbors to be stored
	 * @return Returns the id of the inserted item in the database
	 */
	//long saveCellularScanResult(CellularScanResult cellularScanResult);
	
	//long saveWifiPing(WifiPing wifiPing);
	
	void saveAllTraces(List<Trace> traces);
	
	//long saveWifiExternalEvent(ExternalEvent externalEvent);

}
