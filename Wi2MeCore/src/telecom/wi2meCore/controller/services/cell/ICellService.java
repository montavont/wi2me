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

package telecom.wi2meCore.controller.services.cell;

import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import android.net.NetworkInfo;


public interface ICellService {

	void finalizeService();

	/**
	 * Gets the information about the current connected cell and its neighbors. This function will return the first cell found in the first run. The next runs will return only when a change in the results is available.
	 * This call can be interrupted.
	 * To get the last scan result and return immediately, call the getLastScannedCell method.
	 * @return The cell information (and its neighbors) of the current connected cell, different from the previous call.
	 * @throws InterruptedException Throws this exception if the thread where it is running is interrupted.
	 */
	CellInfo scan() throws InterruptedException;

	CellInfo getLastScannedCell();
	int getLastRsrp();
	
	/**
	 * Retrieves the information about the current mobile data network 
	 * @return The network info of the mobile data connection, or null if it is not connected
	 */
	NetworkInfo getCellularDataNetworkInfo();

	boolean isDataNetworkConnected();

	boolean isPhoneNetworkConnected();

	boolean isDataTransferringEnabled();
	
	/**
	 * This method enables the cellular connection and returns immediately (does not wait to connection to take place)
	 */
	void connectAsync();

	/**
	 * Establishes a data network connection to the current cell and returns as soon as the connection is established.
	 * If the connection is already established, returns true.
	 * If a connection is already taking place, returns false.
	 * If data transferring is disabled, returns false.
	 * @return True if the connection succeeded, false in other case.
	 * @throws TimeoutException Exception thrown if the connection takes more than a connection timeout
	 * @throws InterruptedException Throws this exception if the thread where it is running is interrupted.
	 */
	boolean connect() throws TimeoutException, InterruptedException;

	/**
	 * Disconnects the data network connection to the current cell and returns as soon as the disconnection is established.
	 * If the network is disconnected, returns true.
	 * If a disconnection is already taking place, returns false.
	 * If data transferring is disabled, returns false.
	 * @return True if the disconnection succeeded, false in other case.
	 * @throws TimeoutException Exception thrown if the disconnection takes more than a connection timeout
	 * @throws InterruptedException Throws this exception if the thread where it is running is interrupted.
	 */
	boolean disconnect() throws TimeoutException, InterruptedException;
	
	/**
	 * Disconnects the data network connection to the current cell just like the disconnect() method.
	 * If a timeout is reached, or it failed at disconnecting, throws a RuntimeException with a FATAL ERROR message.
	 * If this is interrupted, simply logs the error and finishes execution.
	 */
	void disconnectOrDie();
	
	/**
	 * Registers a receiver of the cellular disconnection event, to be called whenever this happens. Disconnection can be triggered by the user, or by the system when being unable to keep the connection.
	 * It is not mandatory that you unregister the receiver when finished.
	 * @param receiver The receiver of the disconnection events 
	 */
	void registerDisconnectionReceiver(ICellularConnectionEventReceiver receiver);
	
	/**
	 * Unregisters a receiver of the cellular disconnection event, to be called whenever this happens. Disconnection can be triggered by the user, or by the system when being unable to keep the connection.
	 * @param receiver The receiver of the disconnection events 
	 */
	void unregisterDisconnectionReceiver(ICellularConnectionEventReceiver receiver);

}
