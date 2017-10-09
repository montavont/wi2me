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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.wifi.IWifiConnectionEventReceiver;
import telecom.wi2meCore.controller.services.wifi.PingInfo;
import telecom.wi2meCore.model.entities.Trace;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;


public class WifiService implements IWifiService{

	private static final String INTERFACE_ENABLING_TIMEOUT_MESSAGE = "The timeout for enabling the wifi interface elapsed";
	private static final String INTERFACE_DISABLING_TIMEOUT_MESSAGE = "The timeout for disabling the wifi interface elapsed";
	private static final String SCANNING_TIMEOUT_MESSAGE = "The timeout for scanning elapsed";
	private static final String CONNECTING_TIMEOUT_MESSAGE = "The timeout for connecting elapsed";
	private static final String DISCONNECTING_TIMEOUT_MESSAGE = "The timeout for disconnecting elapsed";

	private static final String CONNECTION_START_EVENT = "CONNECTION_START";	
	private static final String CANNOT_DISCONNECT_MESSAGE = "FATAL ERROR: Wifi network cannot be disconnected. Unable to continue";

	private Context context;
	private WifiManager wifi;
	private ConnectivityManager connectivityManager;
	private InterfaceEnablerThread enablerThread;
	private InterfaceDisablerThread disablerThread;
	private SupplicantConnectedReceiver supplicantConnectedReceiver;	
	private ScanningThread scanningThread;
	private ScanResultReceiver scanResultReceiver;
	private ConnectingThread connectingThread;
	private EventLoggingReceiver eventLoggingReceiver;
	private ConnectionStatusReceiver statusReceiver;
	private DisconnectingThread disconnectingThread;

	private int wifiSleepPolicy = -1;
	private WifiManager.WifiLock wifiLock;
	private PowerManager.WakeLock wl;
	//This variable is used to remove any created entry in the known Networks list
	private int createdNetId = -1;

	private ScanResult targetAP = null;
	private long scanTimestamp = 0;


	public WifiService(Context context){
		wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.context = context;

		//Instantiate the scan result receiver
		scanResultReceiver = new ScanResultReceiver();

		//We register the supplicant connected receiver to know when the interface is enabled as soon as possible
		supplicantConnectedReceiver = new SupplicantConnectedReceiver();
		context.registerReceiver(supplicantConnectedReceiver,  new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
		context.registerReceiver(supplicantConnectedReceiver,  new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));


		//Register receiver to log events to database
		eventLoggingReceiver = new EventLoggingReceiver();
		context.registerReceiver(eventLoggingReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		context.registerReceiver(eventLoggingReceiver,  new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		context.registerReceiver(eventLoggingReceiver,  new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
		context.registerReceiver(eventLoggingReceiver,  new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		//Register receiver to retrieve the finish status of connection attempts
		statusReceiver = new ConnectionStatusReceiver();
		context.registerReceiver(statusReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		context.registerReceiver(statusReceiver,  new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		context.registerReceiver(statusReceiver,  new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
		context.registerReceiver(statusReceiver,  new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));

		try {
			wifiSleepPolicy = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY);
		} catch (SettingNotFoundException e) {
			Log.e("WifiService", "Getting Wifi Sleep policy "+e.getMessage(), e);
		}
		Settings.System.putInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);

		//acquire wakeLock
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wl.acquire();

		//USE WIFI LOCK TO PREVENT IT FROM TURNING OFF
		wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");
		wifiLock.acquire(); 
	}

	@Override
	public void finalizeService() {
		context.unregisterReceiver(supplicantConnectedReceiver);
		context.unregisterReceiver(eventLoggingReceiver);
		context.unregisterReceiver(statusReceiver);

		//return previous wifi sleep policy
		if (wifiSleepPolicy != -1)
			Settings.System.putInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, wifiSleepPolicy);
		//release the wifiLock
		wifiLock.release();
		//release wakeLock
		wl.release();

		//We disable it to let the user enable again and recover known networks
		//wifi.setWifiEnabled(false);
	}

	@Override
	public void enableWiFi(){
		wifi.setWifiEnabled(true);
	}

	@Override
	public void enableWithoutNetworks() throws TimeoutException{
		if (this.isInterfaceEnabled()){//If it is already enabled, just remove the networks
			disableKnownNetworks();
			return;
		}

		if (enablerThread != null){
			if (enablerThread.isAlive())
				//If it is still running, we need to wait it to finish
				return;			
		}
		enablerThread = new InterfaceEnablerThread();
		enablerThread.start();
		try {
			wifi.setWifiEnabled(true);
			enablerThread.join();
			if (!enablerThread.enablingSuccessful)
				throw new TimeoutException(INTERFACE_ENABLING_TIMEOUT_MESSAGE);
		} catch (InterruptedException e) {
			//If this happens, let it finish
		}finally{
			enablerThread = null;
			//remove configured networks in memory so the interface does not connect automatically
			disableKnownNetworks();
		}
	}

	@Override
	public void cleanNetworks() 
	{
		int max_sleep = 10;
		int slept = 0;

		if(createdNetId!=-1){
			Log.d(getClass().getSimpleName(), "++ " + "Removing Network !!!");
			wifi.removeNetwork(createdNetId);
			createdNetId=-1;
		}
		if(isConnectedToAP()){
			wifi.disconnect();
		}
		try {
			while(isConnectedToAP()){
				slept+=1;
				if (slept > max_sleep)
					break;
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		disableKnownNetworks();
	}

	/**
	 * Disable all known networks.
	 * Used to make sure the phone doesn't connect itself, allows the application to have control over the wifi.
	 * Preferred to removeKnownNetworks, as the networks are still accessible.
	 */
	private void disableKnownNetworks() {
		List<WifiConfiguration> ConfiguredNetworks = wifi.getConfiguredNetworks();
		if (ConfiguredNetworks != null)
		{
			for (WifiConfiguration network : ConfiguredNetworks){	
				wifi.disableNetwork(network.networkId);
			}
		}
		else
		{
			Log.i(getClass().getSimpleName(), "wifi.getConfiguredNetworks returned null");
		}
	}

	@Override
	public void enableKnownNetworks() {
		List<WifiConfiguration> ConfiguredNetworks = wifi.getConfiguredNetworks();
		if (ConfiguredNetworks != null)
		{
			for (WifiConfiguration network : wifi.getConfiguredNetworks()){	
				wifi.enableNetwork(network.networkId,false);
			}
		}
		else
		{
			Log.i(getClass().getSimpleName(), "wifi.getConfiguredNetworks returned null");
		}
	}

	@Override
	public void removeKnownNetworks() {
		List<WifiConfiguration> ConfiguredNetworks = wifi.getConfiguredNetworks();
		if (ConfiguredNetworks != null)
		{
			for (WifiConfiguration network : wifi.getConfiguredNetworks()){	
				wifi.removeNetwork(network.networkId);
			}
		}
		else
		{
			Log.i(getClass().getSimpleName(), "wifi.getConfiguredNetworks returned null");
		}
	}

	@Override
	public void enableKnownNetwork(WifiConfiguration network) {
		wifi.enableNetwork(network.networkId,true);
	}

	@Override
	public List<WifiConfiguration> getKnownNetworks(){
		return wifi.getConfiguredNetworks();
	}

	@Override
	public void disable() throws TimeoutException {
		if (!this.isInterfaceEnabled())
			return;

		if (disablerThread != null){
			if (disablerThread.isAlive())
				//If it is still running, we need to wait it to finish
				return;			
		}
		disablerThread = new InterfaceDisablerThread();
		disablerThread.start();
		try
		{
			wifi.setWifiEnabled(false);
			disablerThread.join();
			if (!disablerThread.disablingSuccessful)
				throw new TimeoutException(INTERFACE_DISABLING_TIMEOUT_MESSAGE);
		} catch (InterruptedException e) {
			//If this happens, let it finish
		}finally{
			disablerThread = null;
			/*
			//remove configured networks in memory so the interface does not connect automatically
			removeKnownNetworks();
			 */
		}
	}

	@Override
	public List<ScanResult> scanSynchronously() throws TimeoutException, InterruptedException{
		if (!this.isInterfaceEnabled()){
			Log.e(getClass().getSimpleName(), "++ "+"Unable to scan with disabled interface.");
			return null;
		}

		if (scanningThread != null){
			if (scanningThread.isAlive())
				//If it is still running, we need to wait it to finish
				return null;			
		}
		scanningThread = new ScanningThread();

		context.registerReceiver(scanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		scanningThread.start();
		try {
			wifi.startScan();
			scanningThread.join();
			if (!scanningThread.scanningSuccessful){
				throw new TimeoutException(SCANNING_TIMEOUT_MESSAGE);
			}
			else{ //If the scanning was successful, the thread has a reference of the results				
				return scanningThread.results;
			}
		} catch (InterruptedException e) {
			//If it is interrupted, we should let it interrupt the caller			
			throw e;
		} finally{
			context.unregisterReceiver(scanResultReceiver);
			scanningThread = null;
		}

	}

	@Override
	public List<ScanResult> getScanResults()
	{
		return	wifi.getScanResults();	
	}

	@Override
	public long getScanResultTimestamp()
	{
		return scanTimestamp;
	}


	@Override
	public boolean connect(WifiConfiguration netConfiguration, ScanResult target) throws TimeoutException, InterruptedException 
	{
			
		targetAP = target;

		if (!this.isInterfaceEnabled())
		{
			return false;
		}

		if (connectingThread != null){
			if (connectingThread.isAlive())
			{
				//If it is still running, we need to wait it to finish
				return false;			
			}
		}

		connectingThread = new ConnectingThread();
		connectingThread.start();
		try {
			WifiConnectionEvent connectionEvent = null;
			WifiAP connectionToWifiAP = null;
			connectionToWifiAP = WifiAP.getWifiAPFromScanResult(target);	
			connectionEvent = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), CONNECTION_START_EVENT, connectionToWifiAP);
			Logger.getInstance().log(connectionEvent);

			int netId = 0;
			//To avoid the creation of unnecessary networks in the AP list, we check if it already exists.
			List<WifiConfiguration> knownNetworks = getKnownNetworks();
			int knownNetworkPosition = -1;
			for(WifiConfiguration ap:knownNetworks)
			{

				if(ap.BSSID != null &&  ap.BSSID.equals(netConfiguration.BSSID))
				{
					knownNetworkPosition = knownNetworks.indexOf(ap);
					break;
				}
			}
			//If it doesn't exists, create it.
			if(knownNetworkPosition==-1)
			{
				netConfiguration.status=WifiConfiguration.Status.ENABLED;
				netId = wifi.addNetwork(netConfiguration);
				createdNetId=netId;
			}
			else
			{
				//If it exists, use the existing one and don't create a new one.
				netId = knownNetworks.get(knownNetworkPosition).networkId;				
			}
			wifi.enableNetwork(netId, true);
			connectingThread.join();
			if (connectingThread.timeoutElapsed)
			{
				throw new TimeoutException(CONNECTING_TIMEOUT_MESSAGE);
			}
			return connectingThread.connectingSuccessful;
		} catch (InterruptedException e) {
			//If it is interrupted, we should let it interrupt the caller			
			throw e;
		}finally{
			connectingThread = null;
		}
	}

	@Override
	public void disconnect() throws TimeoutException, InterruptedException {		
		if (!this.isInterfaceEnabled())
			return;
		if (!isConnectedToAP()){ 
			return;
		}
		if (disconnectingThread != null){
			if (disconnectingThread.isAlive())
				//If it is still running, we need to wait it to finish
				return;			
		}
		disconnectingThread = new DisconnectingThread();
		disconnectingThread.start();
		try {
			disableKnownNetworks();
			//wifi.disconnect();
			disconnectingThread.join();
			if (!disconnectingThread.disconnectingSuccessful)
				throw new TimeoutException(DISCONNECTING_TIMEOUT_MESSAGE);
		} catch (InterruptedException e) {
			//If it is interrupted, we should let it interrupt the caller			
			throw e;
		}finally{
			disconnectingThread = null;
		}	
	}

	@Override
	public boolean isInterfaceEnabled() {
		return wifi.isWifiEnabled();
	}

	@Override
	public boolean isConnectedToAP(){
		WifiInfo info = wifi.getConnectionInfo();

		if (info != null&&!info.getSupplicantState().equals(SupplicantState.UNINITIALIZED))
		/*SupplicantState state = info.getSupplicantState();
		if (info != null&&!state.equals(SupplicantState.UNINITIALIZED) && !state.equals(SupplicantState.DISCONNECTED))*/
			return info.getBSSID() != null;
		return false;
	}

	@Override
	public boolean isConnected() {
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		if (netInfo != null){
			return netInfo.isConnected() && (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
		}else{
			return false;
		}
	}

	@Override
	public WifiInfo getWifiConnectionInfo(){
		WifiInfo ret = wifi.getConnectionInfo();
		if (ret.getBSSID() != null && ret.getSSID() != null){
			return ret;
		}else{
			return null;
		}
	}

	@Override
	public PingInfo ping(String ip, float deadline, int packets, float interval) throws IOException, InterruptedException {
		PingInfo ret = new PingInfo(ip);
		Process process = null;
		process = Runtime.getRuntime().exec("ping -c " + packets + " -i " + interval + " -w " + deadline + " " + ip);
		process.waitFor();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = "";
		while ((line = bufferedReader.readLine()) != null){
			//Log.d(getClass().getSimpleName(), "++ " + line);
			if (line.contains("packet")){
				String sent = line.substring(0, line.indexOf(" packets"));
				//Log.d(getClass().getSimpleName(), "++ Sent:" + sent + "-");
				line = line.replace(sent + " packets transmitted, ", "");
				String rec = line.substring(0, line.indexOf(" received"));
				//Log.d(getClass().getSimpleName(), "++ Rec:" + rec + "-");
				ret.received = Integer.parseInt(rec);
				ret.sent = Integer.parseInt(sent);
			}else{
				if (line.startsWith("rtt")){
					line = line.replace("rtt min/avg/max/mdev = ", "");
					line = line.substring(0, line.indexOf(" ms"));
					String[] values = line.split("/");
					//Log.d(getClass().getSimpleName(), "++ Vals:" + values[0] + "-" + values[1] + "-" + values[2] + "-" + values[3] + "-");
					ret.rttMin = Float.parseFloat(values[0]);
					ret.rttAvg = Float.parseFloat(values[1]);
					ret.rttMax = Float.parseFloat(values[2]);
					ret.rttMdev = Float.parseFloat(values[3]);
				}
			}
		}
		return ret;
	}


	@Override
	public DhcpInfo getDhcpInfo() {
		return wifi.getDhcpInfo();
	}


	/** ----------------------------------------CLASSES AND	METHODS TO IMPLEMENT THE WIFI INTERFACE ENABLING METHOD ------------------------------- */
	/**
	 * This class performs the interface enabling and then waits to be interrupted when the supplicant is connected (interface is ready)
	 */
	private class InterfaceEnablerThread extends Thread{

		public boolean enablingSuccessful = false;

		public void run(){			
			try {
				Thread.sleep(TimeoutConstants.WIFI_INTERFACE_ENABLING_TIMEOUT);
				//If the timeout elapses, enabling was unsuccessful
				enablingSuccessful = false;
			} catch (InterruptedException e) {
				// If it is interrupted, it would have finished properly
				enablingSuccessful = true;
			}
		}
	}

	/**
	 * This method will allow the thread waiting for the supplicant to connect, to be interrupted and continue its duty (probably to remove the known networks and prevent the interface from connecting automatically)
	 */
	private void announceSupplicantConnected(){
		if (enablerThread != null){
			enablerThread.interrupt();
		}
	}

	/**
	 * This class is used to receive the broadcast telling that the wifi interface is enabled
	 * @author Alejandro
	 *
	 */
	private class SupplicantConnectedReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			//If the supplicant is already connected, announce it
			if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
				Log.d(this.getClass().getSimpleName(), "++ "+"CONNECTED!");
				announceSupplicantConnected();
			}else{
				Log.d(this.getClass().getSimpleName(), "++ "+"DISCONNECTED!");
				announceSupplicantDisconnected();
			}

		}

	}

	/** CLASSES AND	METHODS TO IMPLEMENT THE WIFI INTERFACE DISABLING METHOD ------------------------------- */
	/**
	 * This class performs the interface disabling and then waits to be interrupted when the supplicant is disconnected (interface disabled)
	 */
	private class InterfaceDisablerThread extends Thread{

		public boolean disablingSuccessful = false;

		public void run(){			
			try {
				Thread.sleep(TimeoutConstants.WIFI_INTERFACE_DISABLING_TIMEOUT);
				//If the timeout elapses, disabling was unsuccessful
				disablingSuccessful = false;
			} catch (InterruptedException e) {
				// If it is interrupted, it would have finished properly
				disablingSuccessful = true;
			}
		}
	}

	/**
	 * This method will allow the thread waiting for the supplicant to connect, to be interrupted and continue its duty (probably to remove the known networks and prevent the interface from connecting automatically)
	 */
	private void announceSupplicantDisconnected(){
		if (disablerThread != null){
			disablerThread.interrupt();
		}
	}

	/** CLASSES AND	METHODS TO IMPLEMENT THE WIFI SYNCHRONOUS SCANNING METHOD ------------------------------- */
	/**
	 * This class performs the scanning and then waits to be interrupted when results are ready
	 */
	private class ScanningThread extends Thread{

		public boolean scanningSuccessful = false;
		public List<ScanResult> results;

		public void run(){			
			try {
				Thread.sleep(TimeoutConstants.WIFI_SCANNING_TIMEOUT);
				//If the timeout elapses, scanning was unsuccessful
				scanningSuccessful = false;
			} catch (InterruptedException e) {
				// If it is interrupted, it would have finished properly
				scanningSuccessful = true;
			}
		}
	}

	/**
	 * This method will allow the thread waiting for the scan results, to be interrupted and continue its duty
	 */
	private void announceScanResultsReady(){
		if (scanningThread != null){
			scanningThread.results = wifi.getScanResults();	
			scanningThread.interrupt();
		}
	}

	/**
	 * This class is used to receive the broadcast with the scan results
	 * @author Alejandro
	 *
	 */
	private class ScanResultReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {			
			announceScanResultsReady();
		}

	}

	/** CLASSES AND	METHODS TO IMPLEMENT THE WIFI SYNCHRONOUS CONNECTION METHOD ------------------------------- */
	/**
	 * This class performs the connections and then waits to be interrupted when connection events are done
	 */
	private class DisconnectingThread extends Thread{

		public boolean disconnectingSuccessful = false;

		public void run(){			

			try {
				Thread.sleep(TimeoutConstants.WIFI_DISCONNECTING_TIMEOUT);
				//If the timeout elapses, disconnecting was unsuccessful
				disconnectingSuccessful = false;
			} catch (InterruptedException e) {
				// If it is interrupted, the timeout did not elapse and the disconnection took place
				disconnectingSuccessful = true;
			}
		}
	}

	/**
	 * This method will allow the thread for the connection to take place, to be interrupted and finish its duty
	 */
	private void announceDisonnectionFinished(){
		if (disconnectingThread != null){
			disconnectingThread.interrupt();
		}
	}

	/** CLASSES AND	METHODS TO IMPLEMENT THE WIFI SYNCHRONOUS CONNECTION METHOD ------------------------------- */
	/**
	 * This class performs the connections and then waits to be interrupted when connection events are done
	 */
	private class ConnectingThread extends Thread{

		public boolean timeoutElapsed = false;
		public boolean connectingSuccessful = false;

		public void run(){
			if (!wifi.reconnect())
			{
				timeoutElapsed = false;
				connectingSuccessful = false;
				return;
			}

			try {
				Thread.sleep(TimeoutConstants.WIFI_CONNECTING_TIMEOUT);
				//If the timeout elapses, connecting was unsuccessful
				timeoutElapsed = true;
				connectingSuccessful = false;
			}
			catch (InterruptedException e)
			{
				// If it is interrupted, the timeout did not elapse, but the receiver of the wifi state decides if the connection was successful or not
				timeoutElapsed = false;
			}
		}
	}

	/**
	 * This method will allow the thread for the connection to take place, to be interrupted and finish its duty
	 */
	private void announceConnectionFinished(){
		if (connectingThread != null){
			connectingThread.interrupt();
		}
	}

	/*
	 * Event receiver to log Wifi statuses to database
	 */
	private class EventLoggingReceiver extends BroadcastReceiver
	{

		//This indeed looks ugly, although, on some devices the CONNECTED and DISCONNECTED events may be fired more than once identically. This will allow us to drop the echoes 
		private boolean expecting_connected = true;
		private boolean expecting_disconnected = true;
		private final Semaphore mSema = new Semaphore(1, true);

		@Override
		public void onReceive(Context context, Intent intent) 
		{

			NetworkInfo.DetailedState netState = null;
			WifiConnectionEvent event = null;
			WifiAP connectionToWifiAP = null;

			String action = intent.getAction();

			Log.d(this.getClass().getSimpleName(), "++ ACTION " + action); //TKE 


			if (action == ConnectivityManager.CONNECTIVITY_ACTION)
			{

				
				NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				// This is a starting case, targetAP might be null, we do not really care anyway...
				if (mNetworkInfo != null && targetAP != null)
				{	
					try
					{
						if (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
						{
							netState = mNetworkInfo.getDetailedState();
							switch(netState)
							{
								case CONNECTED:
									WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
									if (info != null && expecting_connected)
									{
										connectionToWifiAP = WifiAP.getNewWifiAP(info.getBSSID(), info.getSSID(), info.getRssi(), WifiAP.frequencyToChannel(targetAP.frequency), targetAP.capabilities, info.getLinkSpeed());
										expecting_connected = false;
										expecting_disconnected = true;
									}
									break;
								case DISCONNECTED:
									mSema.acquire();
									if (expecting_disconnected)
									{
										connectionToWifiAP = WifiAP.getWifiAPFromScanResult(targetAP);
										expecting_connected = true;
										expecting_disconnected = false;
									}
									mSema.release();
									break;
							}
							event = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), netState.name(), connectionToWifiAP);
						}
					}

					catch (InterruptedException e)
					{
						Log.d(this.getClass().getSimpleName(), "++ Connectivity change action failed to complete because of semaphore locking");
						e.printStackTrace();
					}
				}


			}
			else if ( action == WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
			{
				SupplicantState supState = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				switch (supState)
				{
					case ASSOCIATING:
					case ASSOCIATED:
						WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
						if (info != null)
						{
							connectionToWifiAP = WifiAP.getWifiAPFromScanResult(targetAP);
						}
						event = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), supState.name(), connectionToWifiAP);
						break;
				}
			}
			else if ( action == WifiManager.NETWORK_STATE_CHANGED_ACTION)
			{
				NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (mNetworkInfo != null)
				{
					netState = mNetworkInfo.getDetailedState();
					switch(netState)
					{
						case FAILED:
						case AUTHENTICATING:
						case OBTAINING_IPADDR:
							WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
							if (info != null)
							{
								connectionToWifiAP = WifiAP.getWifiAPFromScanResult(targetAP);								}
							event = WifiConnectionEvent.getNewWifiConnectionEvent(TraceManager.getTrace(), netState.name(), connectionToWifiAP);
							break;
					}
				}	
			}
			else if ( action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
			{
				scanTimestamp = System.currentTimeMillis();
			}

			if (event != null)
			{
				Logger.getInstance().log(event);
			}	
		}
	}


	/*
	 * Event receiver to retrieve the connection operation status (success or failure)
	 */
	private class ConnectionStatusReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent) 
		{

			NetworkInfo.DetailedState netState = null;
			WifiConnectionEvent event = null;
			WifiAP connectionToWifiAP = null;

			String action = intent.getAction();
			if (action == ConnectivityManager.CONNECTIVITY_ACTION)
			{
				NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				// This is a starting case, targetAP might be null, we do not really care anyway...
				if (mNetworkInfo != null)
				{	
						if (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)
						{
							netState = mNetworkInfo.getDetailedState();
							switch(netState)
							{
								case CONNECTED:
									if (connectingThread != null)
									{
										connectingThread.connectingSuccessful = true;
										announceConnectionFinished();
									}				
									break;
								case DISCONNECTED:
									announceDisonnectionFinished();
									break;
							}
						}
				}


			}
			else if ( action == WifiManager.NETWORK_STATE_CHANGED_ACTION)
			{

				NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (mNetworkInfo != null)
				{
					netState = mNetworkInfo.getDetailedState();
					switch(netState)
					{
						case FAILED:
							if (connectingThread != null)
							{
								connectingThread.connectingSuccessful = false;
								announceConnectionFinished();
							}
							break;
					}
				}	
			}
			else if ( action == WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
			{
				SupplicantState supState = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				switch (supState)
				{
					case DISCONNECTED:
					case INACTIVE:
						announceSupplicantDisconnected();
						break;
				}
			}

		}
	}

	@Override
	public void disconnectOrDie() {
		//Make sure previous connections are closed
		try {
			disconnect();
		} catch (TimeoutException e) {
			// fatal error
			Log.e(getClass().getSimpleName(), "++ "+ "Could not disconnect wifi network", e);
			throw new RuntimeException(CANNOT_DISCONNECT_MESSAGE);
		} catch (InterruptedException e) {
			// if we are interrupted, we finish
			Log.d(getClass().getSimpleName(), "++ "+"Interrupted while disconnecting", e);
		}
	}

	@Override
	public void disableAsync() {
		wifi.setWifiEnabled(false);
	}

}
