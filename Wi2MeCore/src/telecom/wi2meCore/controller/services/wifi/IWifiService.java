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

package telecom.wi2meCore.controller.services.wifi;

import java.io.IOException;
import java.util.List;

import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.wifi.IWifiConnectionEventReceiver;
import telecom.wi2meCore.controller.services.wifi.PingInfo;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

public interface IWifiService {
	
	/**
	 * Turns on the wifi. Used to get the list of preferred networks if Wifi is not enabled.
	 */
	void enableWiFi();
	
	/**
	 * Turns on the wifi interface, forcing it to remove the known networks in memory (that is to say, known networks will still be remembered when restarting the interface)
	 * This option prevents the device from auto-connecting to a network if found.
	 * If the interface is already enabled, it only removes the networks.
	 */
	void enableWithoutNetworks() throws TimeoutException;
	
	/**
	 * Enables the given known network, keeps the other know networks disabled.
	 * @param network WifiConfiguration
	 */
	void enableKnownNetwork(WifiConfiguration network);
	
	/**
	 * Enables all the known networks.
	 * Used when leaving the application (to let the phone take back its control over the wifi).
	 */
	void enableKnownNetworks();
	
	/**
	 * Removes all known networks.
	 * Do NOT use unless extreme necessity. Prefer disableKnownNetworks (private).
	 */
	void removeKnownNetworks();
	
	/**
	 * Returns the list of known (preferred) networks of the phone.
	 * @return The list of known networks
	 */	
	List<WifiConfiguration> getKnownNetworks();
	
	/**
	 * Turns off the wifi interface.
	 * If it is disabled, returns immediately.
	 */
	void disable() throws TimeoutException;
	
	/**
	 * Launches method to disable interface and returns immediately
	 */
	void disableAsync();
	
	/**
	 * Turns on the wifi interface (see the turnOnWithoutNetworks method), and as soon as this is done, performs a synchronous scanning (see the scanSynchronously method)
	 * @return The list of ScanResult obtained from scanning
	 * @throws ScanningTimeoutException This RuntimeException is thrown when the scanning results are not ready before the scanning timeout period of the service.
	 */
	/*
	List<ScanResult> turnOnWithoutNetworksAndScanSynchronously() throws ScanningTimeoutException;
	*/
	/**
	 * Performs a scanning and returning the scan results as soon as they are available.
	 * If the interface is disabled, returns null.
	 * @return The list of ScanResult obtained from scanning
	 * @throws TimeoutException This RuntimeException is thrown when the scanning results are not ready before the scanning timeout period of the service.
	 * @throws InterruptedException This is thrown when the thread where this is running is interrupted
	 */
	List<ScanResult> scanSynchronously() throws TimeoutException, InterruptedException;
	

	/*
	 * 	
	 * 	Retrieves the last list of scan results from the wifi manager, without guaranties on their age
 	*/
	List<ScanResult> getScanResults();

	/*
	 * 	
	 * 	Retrieves the timestamp of the last scan results
 	*/
	long getScanResultTimestamp();

	/**
	 * 
	 * @param netInfo Information 
	 */
	/**
	 * Establishes a layer 2 wifi connection with the network indicated, calling the receiver to broadcast the events. Finishes as soon as the connection is established or failed.
	 * If the interface is disabled, returns false.
	 * @param netConfiguration Configuration of the network you want to connect to
	 * @param receiver The receiver of the connection events 
	 * @return True if the connection was successful, false if it failed.
	 * @throws InterruptedException This is thrown when the thread where this is running is interrupted
	 */
	boolean connect(WifiConfiguration netConfiguration, ScanResult target) throws TimeoutException, InterruptedException;
	
	/**
	 * Disconnects from the network previously connected (if any). Also forgets this network to prevent automatic connections.
	 * If the interface is disabled, it does nothing.
	 * @throws InterruptedException This is thrown when the thread where this is running is interrupted
	 */
	void disconnect() throws TimeoutException, InterruptedException;
	
	/**
	 * Removes all known networks and possibly disconnects to the current AP. Runs asynchronously (this means, returns immediately).
	 */
	void cleanNetworks();
	
	/**
	 * Finalize the service closing connections (is any) and unregistering receivers
	 */
	void finalizeService();
	
	/**
	 * Informs if the wifi interface is currently enabled
	 * @return True if the interface is enabled, false in other case
	 */
	boolean isInterfaceEnabled();

	/**
	 * Informs is the wifi interface is connected to an AP or not. Not necessarily if will have assigned an IP address yet.
	 * @return True if wifi is connected (associated) with an AP, false in other case
	 */
	boolean isConnectedToAP();
	
	/**
	 * If it is connected it is because it already has an IP address and it is completely connected.
	 * @return True if wifi is connected and has an IP address, false in other case
	 */
	boolean isConnected();

	/**
	 * Returns the information about the current wifi connection (if any). If the connection info is not available (BSSID, SSID, etc) returns null
	 * @return The WifiInfo object with connection information, or null if the information is not available
	 */
	WifiInfo getWifiConnectionInfo();
	
	/**
	 * Sends ping requests to the ip address passed
	 * @param ip IP Address to ping
	 * @param deadline Time in seconds to complete the run, regardless of how many packets have been sent or received
	 * @param packets Amount of packets that will be sent (only before deadline elapses)
	 * @param interval Time in seconds between packets send (over 0.2 only)
	 * @return Information obtained from the ping request
	 * @throws IOException When there are problems calling the ping command 
	 * @throws InterruptedException When ping is interrupted while running
	 */
	PingInfo ping(String ip, float deadline, int packets, float interval) throws IOException, InterruptedException;
	
	/**
	 * Retrives the results of a DHCP request
	 * @return Results of the requests.
	 */
	DhcpInfo getDhcpInfo();

	/**
	 * Disconnects the wifi network connection to the current AP just like the disconnect() method.
	 * If a timeout is reached, or it failed at disconnecting, throws a RuntimeException with a FATAL ERROR message.
	 * If this is interrupted, simply logs the error and finishes execution.
	 */
	void disconnectOrDie();

}
