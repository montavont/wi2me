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

package telecom.wi2meUser.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


import telecom.wi2meUser.R;
import telecom.wi2meUser.Wi2MeUserActivity;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.AssetServices;
import telecom.wi2meCore.controller.services.CellThreadContainer;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.IControllerServices;
import telecom.wi2meCore.controller.services.NotificationServices;
import telecom.wi2meCore.controller.services.ThreadSynchronizingService;
import telecom.wi2meCore.controller.services.TimeService;
import telecom.wi2meCore.controller.services.cell.CellService;
import telecom.wi2meCore.controller.services.communityNetworks.CommunityNetworkService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.move.MoveService;
import telecom.wi2meCore.controller.services.persistance.DatabaseHelper;
import telecom.wi2meCore.controller.services.trace.BatteryService;
import telecom.wi2meCore.controller.services.trace.IBatteryLevelReceiver;
import telecom.wi2meCore.controller.services.trace.LocationService;
import telecom.wi2meCore.controller.services.web.WebService;
import telecom.wi2meCore.controller.services.wifi.WifiService;
import telecom.wi2meCore.model.Flag;
import telecom.wi2meCore.model.ILogger;
import telecom.wi2meCore.model.IWirelessNetworkCommandLooper;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommandLooper;
import telecom.wi2meCore.model.Logger.TraceString;
import telecom.wi2meCore.model.cellCommands.CellCleanerCommand;
import telecom.wi2meCore.model.cellCommands.CellConnector;
import telecom.wi2meCore.model.cellCommands.CellDownloader;
import telecom.wi2meCore.model.cellCommands.CellScanner;
import telecom.wi2meCore.model.cellCommands.CellTransferrerContainer;
import telecom.wi2meCore.model.cellCommands.CellUploader;
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meUser.model.parameters.ParameterFactory;
import telecom.wi2meCore.model.wifiCommands.CommunityNetworkConnector;
import telecom.wi2meCore.model.wifiCommands.Pinger;
import telecom.wi2meCore.model.wifiCommands.WifiCleanerCommand;
import telecom.wi2meUser.model.wifiCommands.WifiConnector;
import telecom.wi2meUser.model.wifiCommands.WifiStayConnected;
import telecom.wi2meCore.model.wifiCommands.WifiDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiScanner;
import telecom.wi2meCore.model.wifiCommands.WifiSensor;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.WifiUploader;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ApplicationService  extends Service {

	private static String UPLOAD_FILE_DIRECTORY = "files/";

	Thread wifiThread;
	Thread cellThread;
	IWirelessNetworkCommandLooper wifiCommandLooper;
	IWirelessNetworkCommandLooper cellCommandLooper;
	IParameterManager parameters;
	Flag wifiWorkingFlag;
	Flag cellWorkingFlag;
	int startId;
	CellThreadContainer cellThreadContainer;
	String configuration;
	Context context;
	boolean wasCellularConnected;

	ServiceBinder binder;

	private NotificationManager mNM;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = 0;
	private boolean notification_show = false;

	// Lost connection
	private LostConnectionReceiver lostConnectionReceiver = new LostConnectionReceiver(this);

	@Override
	public void onCreate() {

		super.onCreate();

		cellThreadContainer = new CellThreadContainer();
		context = this;

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		parameters = ParameterFactory.getNewParameterManager();

		binder = new ServiceBinder(parameters);


		ControllerServices.initializeServices(new TimeService(),
							new CellService(this),
							new MoveService(this,
							new TimeService()),
							new WebService(this),
							new WifiService(this),
							new BatteryService(this),
							new LocationService(this),
							new ThreadSynchronizingService(cellThreadContainer),
							new AssetServices(this),
							new NotificationServices(this),
							new CommunityNetworkService(this)
);

		//binder also keeps the last info of the log
		Logger.getInstance().addObserver(binder);
		try {
			ConfigurationManager.loadParameters(context, parameters);
			configuration = ConfigurationManager.getConfiguration();
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			//If we have problems loading the parameters file, we should tell and finish
			Toast.makeText(this, "ERROR LOADING CONFIGURATION FILE: "+e.getMessage()+". Please, check ensure USB storage is off. Otherwise, replace configuration file and try again.", Toast.LENGTH_LONG).show();
			binder.loadingError = true;
			return;
		}


		// We get if the 3G connection is available to enable it again when closing
		int version = Build.VERSION.SDK_INT;
		if (version <= 8){//8 is FROYO (2.2), higher versions change the Telephony API so we should not use the cellular methods
			wasCellularConnected = ControllerServices.getInstance().getCell().isDataNetworkConnected();
		}else{
			wasCellularConnected = false;
		}

		// Lost connection
		lostConnectionReceiver.register();
	}

	private void startThreads() {
		wifiThread = new Thread(){
			public void run(){
				wifiCommandLooper.loop(parameters);
			}
		};

		cellThread = new Thread(){
			public void run(){
				cellCommandLooper.loop(parameters);
			}
		};
		cellThreadContainer.cellThread = cellThread;

		Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), "STARTING.CONFIGURATION:" + configuration));

		if (cellWorkingFlag.isActive())
		{
			cellThread.start();
		}
		if (wifiWorkingFlag.isActive())
		{
			wifiThread.start();
		}

		(new Thread(){
			public void run(){
				ControllerServices.getInstance().getBattery().registerLevelReceiver(
						new BatteryLevelSupervisor((Integer)parameters.getParameter(Parameter.MIN_BATTERY_LEVEL)));
			}
		}).start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.startId = startId;
		Log.d(getClass().getSimpleName(), "++ " + "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {

		// Lost connection
		lostConnectionReceiver.unregister();

		if (binder.isBatteryLow){
			if (binder.isRunning){
				binder.stop();
				ControllerServices.getInstance().getNotification().playNotificationSound();
			}

		}

		if (wasCellularConnected){
			//disable wifi if connected
			ControllerServices.getInstance().getWifi().disableAsync();
			/*
	    	try {
				ControllerServices.getInstance().getCell().connect();
			} catch (TimeoutException e) {
				// If it fails to connect, we are unlucky
				Log.e(getClass().getSimpleName(), e.getMessage(), e);
				Toast.makeText(context, "Could not re-enable cellular network connection", Toast.LENGTH_LONG);
			} catch (InterruptedException e) {
				// Should never be interrupted
				Log.e(getClass().getSimpleName(), e.getMessage(), e);
			}    	*/
			//enable cellular connection
			ControllerServices.getInstance().getCell().connectAsync();
		}

		ControllerServices.finalizeServices();
		/*
    	if (DatabaseHelper.isInitialized())
    		DatabaseHelper.getDatabaseHelper().closeDatabase();
		 */


		super.onDestroy();
	}

	private void stopThreads() {
		//Stop
		//we also interrupt what is being done, as it may be sleeping to scan, or downloading something



		if (cellWorkingFlag != null){
			cellWorkingFlag.setActive(false);
			cellCommandLooper.breakLoop();
			cellThread.interrupt();
			try {
				cellThread.join(10000);
			} catch (InterruptedException e) {
				//Should not happen
				Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
			}
		}

		if (wifiWorkingFlag != null){
			wifiWorkingFlag.setActive(false);
			wifiCommandLooper.breakLoop();
			wifiThread.interrupt();
			try {
				wifiThread.join(10000);
			} catch (InterruptedException e) {
				//Should not happen
				Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
			}
		}

		Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), "STOPPING"));

	}

	@Override
	public IBinder onBind(Intent intent) {
		binder.setBound(true);
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		binder.setBound(false);
		return super.onUnbind(intent);
	}

	/**
	 * Show a notification while this service is running.
	 * @param the text to display.
	 */
	private void showNotification(String text) {

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Wi2MeUserActivity.class), 0);

		notification.flags = Notification.FLAG_NO_CLEAR;
		notification_show = true;
		// Send the notification.
		mNM.notify(NOTIFICATION, notification);

	}


	public class ServiceBinder extends Binder implements Observer{

		private boolean firstRun;
		private boolean bound;
		public boolean loadingError;
		public Date startedDate;
		private Observable observable;
		private Object wifiData;
		private Object cellData;
		private boolean isRunning;
		public boolean isBatteryLow;
		public IParameterManager parameters;

		public boolean reconnectionNeeded = false;


		public synchronized boolean isRunning() {
			return isRunning;
		}

		protected synchronized void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}

		public synchronized Object getCellData() {
			return cellData;
		}

		protected synchronized void setCellData(Object cellData) {
			this.cellData = cellData;
		}

		public synchronized Observable getObservable() {
			return observable;
		}

		protected synchronized void setObservable(Observable observable) {
			this.observable = observable;
		}

		public synchronized Object getWifiData() {
			return wifiData;
		}

		protected synchronized void setWifiData(Object data) {
			this.wifiData = data;
		}

		public ServiceBinder(IParameterManager parameters){
			this.parameters = parameters;
			isBatteryLow = false;
			firstRun = true;
			startedDate = new Date(Calendar.getInstance().getTimeInMillis());
			loadingError = false;
			observable = null;
			wifiData = null;
		}

		public void start()
		{

			wifiCommandLooper = new WirelessNetworkCommandLooper();
			wifiCommandLooper.addCommand(new WifiScanner());
			wifiCommandLooper.addCommand(new WifiConnector());
			wifiCommandLooper.addCommand(new CommunityNetworkConnector());
			wifiCommandLooper.addCommand(new WifiStayConnected());
			wifiCommandLooper.addCommand(new WifiCleanerCommand());

			if ((Boolean)parameters.getParameter(Parameter.SENSOR_ONLY))
				wifiCommandLooper.addCommand(new WifiSensor());

			wifiCommandLooper.initializeCommands(parameters);

			cellCommandLooper = new WirelessNetworkCommandLooper();
			cellCommandLooper.addCommand(new CellScanner());
			cellCommandLooper.addCommand(new CellConnector());
			cellCommandLooper.addCommand(new CellCleanerCommand());

			cellCommandLooper.initializeCommands(parameters);

			DatabaseHelper.initialize(context);
			wifiWorkingFlag = new Flag((Boolean)parameters.getParameter(Parameter.RUN_WIFI));
			cellWorkingFlag = new Flag((Boolean)parameters.getParameter(Parameter.RUN_CELLULAR));

			parameters.setParameter(Parameter.WIFI_WORKING_FLAG, wifiWorkingFlag);
			parameters.setParameter(Parameter.CELL_WORKING_FLAG, cellWorkingFlag);

			ControllerServices.getInstance().getMove().resetLastMovementTimestamp();
			TraceManager.initializeManager(parameters);
			if((Boolean)parameters.getParameter(Parameter.NOTIFY_SERVICE_STATUS)){
				showNotification("Wi2Me service running");
			}

			startForeground(NOTIFICATION, null);

			parameters.setParameter(Parameter.IS_FIRST_LOOP, true);
			startThreads();
			setRunning(true);
		}

		public void stop(){
			stopThreads();

			if (cellCommandLooper != null)
				cellCommandLooper.finalizeCommands(parameters);

			if (wifiCommandLooper != null)
				wifiCommandLooper.finalizeCommands(parameters);

			TraceManager.finalizeManager();

			// Cancel the persistent notification.
			if(notification_show){
				mNM.cancel(NOTIFICATION);
				notification_show = false;
			}

			stopForeground(true);

			if (DatabaseHelper.isInitialized())
				DatabaseHelper.getDatabaseHelper().closeDatabase();
			setRunning(false);
		}



		private PendingIntent getDialogPendingIntent(String dialogText,
	            String intentname) {
       			 return PendingIntent.getActivity(
		                context,
                		dialogText.hashCode(),
		                new Intent()
                	        .putExtra(Intent.EXTRA_TEXT, dialogText)
                        	.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	                        .setAction(intentname), 0);
    		}

		public void showNotificationBar(String text){
			if((Boolean)parameters.getParameter(Parameter.NOTIFY_SERVICE_STATUS)){
				showNotification(text);
			}
		}

		public IControllerServices getServices(){
			return ControllerServices.getInstance();
		}

		public ILogger getLogger(){
			return Logger.getInstance();
		}

		public boolean isFirstRun(){
			boolean ret = firstRun;
			firstRun = false;
			return ret;
		}

		public synchronized void setBound(boolean b) {
			this.bound = b;
		}

		public synchronized boolean isBound(){
			return bound;
		}

		@Override
		public void update(Observable observable, Object data) {
			setObservable(observable);
			TraceString logTrace = (TraceString) data;
			switch(logTrace.type){
			case CELL:
				setCellData(data);
				break;
			case WIFI:
				setWifiData(data);
				break;
			}
		}

		public void forceReconnection()
		{
			Log.d("MOBIDAC_wi2me", "Call to Wi2me forceReconnection()" );
			parameters.setParameter(Parameter.IPC_RECONNECTION_NEEDED, true);
		}

	}

	private class BatteryLevelSupervisor implements IBatteryLevelReceiver{

		private int minLevel;

		public BatteryLevelSupervisor(int minLevel){
			this.minLevel = minLevel;
		}

		@Override
		public void receiveBatteryLevel(int batteryLevel) {
			if (batteryLevel <= minLevel){
				Log.d(getClass().getSimpleName(), "++ " + "Stopping Service");
				binder.isBatteryLow = true;
				stopSelf(startId);
				if (binder.isBound()){
					Toast.makeText(context, "Minimum battery level reached (" + batteryLevel + "%)." +
							" Meassuring will stop when application closes", Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(context, "Minimum battery level reached (" + minLevel + "%)." +
							" Meassuring stopped", Toast.LENGTH_LONG).show();
				}

				//Toast.makeText(this, "Minimum battery level reached (" + batteryLevel + "%)", Toast.LENGTH_LONG).show();
			}
		}


	}




	/*
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	 */

}
