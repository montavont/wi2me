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

package telecom.wi2meRecherche.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import telecom.wi2meRecherche.R;
import telecom.wi2meRecherche.Wi2MeRecherche;
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
import telecom.wi2meCore.controller.services.persistance.TextTraceHelper;
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
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.wifiCommands.CommunityNetworkConnector;
import telecom.wi2meCore.model.wifiCommands.Pinger;
import telecom.wi2meCore.model.wifiCommands.WifiCleanerCommand;

import telecom.wi2meRecherche.model.parameters.ParameterFactory;
import telecom.wi2meRecherche.model.wifiCommands.WifiConnector;
import telecom.wi2meRecherche.model.wifiCommands.WifiBestRssiSelector;
import telecom.wi2meRecherche.model.wifiCommands.WifiCommandPoppingStayConnected;
import telecom.wi2meCore.model.wifiCommands.WifiDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiSPDYDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiWebPageDownloader;
import telecom.wi2meCore.model.wifiCommands.WifiScanner;
import telecom.wi2meCore.model.wifiCommands.WifiSensor;
import telecom.wi2meCore.model.wifiCommands.WifiTransferrerContainer;
import telecom.wi2meCore.model.wifiCommands.WifiUploader;
import telecom.wi2meCore.model.ShellPoppingCommand;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;








public class ApplicationService extends Service {
	
	private static String ASSETS_FILES_DIRECTORY = "files/";
	
        HashMap<String, IWirelessNetworkCommandLooper> WirelessLoopers = new HashMap<String, IWirelessNetworkCommandLooper>();
        HashMap<String, Thread> WirelessThreads = new HashMap<String, Thread>();
	

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

	
	@Override
	public void onCreate()
	{
    		super.onCreate(); 
    		
    		cellThreadContainer = new CellThreadContainer();
    		context = this;
    		
    		Log.d(getClass().getSimpleName(), "++ " + "Running onCreate");    	
    		
    		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    		parameters = ParameterFactory.getNewParameterManager();
    		
    		binder = new ServiceBinder(parameters);

    		//binder also keeps the last info of the log
    		Logger.getInstance().addObserver(binder);
    		try {
		        
		        ControllerServices.initializeServices(new TimeService(),
								new CellService(this),
								new MoveService(this, new TimeService()),
								new WebService(this),
								new WifiService(this),
								new BatteryService(this),
								new LocationService(this),
					        		new ThreadSynchronizingService(cellThreadContainer),
								new AssetServices(this),
								new NotificationServices(this),
								new CommunityNetworkService(this)
								);
		        
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
		if (version <= 8)
		{
			//TKER TODO LOook into
			//8 is FROYO (2.2), higher versions change the Telephony API so we should not use the cellular methods
			wasCellularConnected = ControllerServices.getInstance().getCell().isDataNetworkConnected();
		}
		else
		{
			wasCellularConnected = false;
		}
			
        	/*// Set the icon, scrolling text and timestamp
	        Notification notification = new Notification(R.drawable.icon, "Wi2Me service running",
                System.currentTimeMillis());

	        // The PendingIntent to launch our activity if the user selects this notification
        	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Wi2MeRecherche.class), 0);

        	// Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this, "Wi2Me Xplorer", "Select to open application", contentIntent);
        
	        notification.flags = Notification.FLAG_NO_CLEAR;
        
		mNM.notify(NOTIFICATION, notification);

		startForeground(NOTIFICATION, notification);
	        
	        Log.d(getClass().getSimpleName(), "++ " + "Finished onCreate");*/
	}

	private class WirelessLooperThread extends Thread
	{
		private IWirelessNetworkCommandLooper looper;

		public WirelessLooperThread(IWirelessNetworkCommandLooper looper)
		{
			this.looper = looper;
		}
        	
		@Override		
		public void run()
		{
        		looper.loop(parameters);
        	}

	}

	private void startThreads()
	{
		for (String looperKey:WirelessLoopers.keySet())
		{
			IWirelessNetworkCommandLooper looper = WirelessLoopers.get(looperKey);
			WirelessThreads.put(looperKey, new WirelessLooperThread(looper));			
			if (looperKey.equals("cellCommands"))
			{
				cellThreadContainer.cellThread = WirelessThreads.get(looperKey);
			}
		}
       


		try
		{
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String version = pInfo.versionName;
			Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), "APPLICATION.VERSION:" + version));
	
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);

			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			String appBuildDate = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), "APPLICATION.BUILDDATE:" + appBuildDate));


		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		}
		catch (NameNotFoundException e)
		{
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		}

 
		Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), "STARTING.CONFIGURATION:" + configuration));
        
		for (Thread loopThr:WirelessThreads.values())
		{	
			loopThr.start();
		}
		
		(new Thread()
		{
			public void run(){
				ControllerServices.getInstance().getBattery().registerLevelReceiver(
					new BatteryLevelSupervisor((Integer)parameters.getParameter(Parameter.MIN_BATTERY_LEVEL)));
			}
		}).start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
	    	this.startId = startId;
	        // We want this service to continue running until it is explicitly
        	// stopped, so return sticky.
        	return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy()
	{
    	
	    	Log.d(getClass().getSimpleName(), "++ " + "Running onDestroy");
        	if (binder.isBateryLow)
		{
    			if (binder.isRunning)
			{
    				binder.stop();
	    			ControllerServices.getInstance().getNotification().playNotificationSound();
    			}
    			
	    	}
    	
    		if (wasCellularConnected){
    			//disable wifi if connected
    			ControllerServices.getInstance().getWifi().disableAsync();
    			ControllerServices.getInstance().getCell().connectAsync();
    		}
    		    	    	
    		ControllerServices.finalizeServices();    	
    		
    		super.onDestroy();
	}

	private void stopThreads()
	{
		//Stop 
	    	//we also interrupt what is being done, as it may be sleeping to scan, or downloading something
		    	
		for (String looperKey:WirelessLoopers.keySet())
		{
			WirelessLoopers.get(looperKey).breakLoop();
			WirelessThreads.get(looperKey).interrupt();//Same keys anyway
			try
			{
				WirelessThreads.get(looperKey).join(10000);
			}
			catch (InterruptedException e)
			{
				//Should not happen
				Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
			}
		}

		cellWorkingFlag.setActive(false);
		wifiWorkingFlag.setActive(false);

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
     */

    private void showNotification() {

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, "Wi2Me service running",
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Wi2MeRecherche.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Wi2Me Xplorer",
        		"Select to open application", contentIntent);
        
        notification.flags = Notification.FLAG_NO_CLEAR;

        // Send the notification.
 	mNM.notify(NOTIFICATION, notification);
        
	startForeground(NOTIFICATION, notification);
    }

    
	public class ServiceBinder extends Binder implements Observer
	{
	    	private boolean firstRun;
    		private boolean bound;
	    	public boolean loadingError;
		public Date startedDate;
		private Observable observable;
		private Object wifiData;
		private Object cellData;
		private boolean isRunning;
		public boolean isBateryLow;
		public IParameterManager parameters;
    	
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
			isBateryLow = false;
			firstRun = true;
			startedDate = new Date(Calendar.getInstance().getTimeInMillis());    		
	    		loadingError = false;
    			observable = null;
    			wifiData = null;
	    	}
		
		public void start()
		{
			DatabaseHelper.initialize(context);
			TextTraceHelper.initialize(context);
		        wifiWorkingFlag = new Flag((Boolean)parameters.getParameter(Parameter.RUN_WIFI));
	        	cellWorkingFlag = new Flag((Boolean)parameters.getParameter(Parameter.RUN_CELLULAR));

			if (wifiWorkingFlag.isActive())
			{
				//WirelessLoopers.put("wifiCommands", new WirelessNetworkCommandLooper(new WifiCleanerCommand())); //TKE
				WirelessLoopers.put("wifiCommands", new WirelessNetworkCommandLooper());
			}
			if (cellWorkingFlag.isActive())
			{
				//WirelessLoopers.put("cellCommands", new WirelessNetworkCommandLooper(new CellCleanerCommand())); //TKE
				WirelessLoopers.put("cellCommands", new WirelessNetworkCommandLooper());
			}

			try
			{
				JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream((String)parameters.getParameter(Parameter.COMMAND_FILE))));
				reader.beginObject();
				while (reader.hasNext())
				{
					String topKey = reader.nextName();
					if (topKey.equals("command"))
					{
						reader.beginObject();
						String commandType = "";
						String commandModule = "wi2meCore";
						String commandFamily = "wifiCommands";
						HashMap<String, String> commandParams = new HashMap<String, String>();
						while (reader.hasNext())
						{
							String key = reader.nextName();
							if (key.equals("name"))
							{
         							commandType = reader.nextString();
							}
							else if (key.equals("family"))
							{
								commandFamily = reader.nextString();
							}
							else if (key.equals("module"))
							{
         							commandModule = reader.nextString();
							}
       							else if (key.equals("params"))
							{
								reader.beginObject();
								while (reader.hasNext())
								{
									commandParams.put(reader.nextName(), reader.nextString());
								}
								reader.endObject();	
							}
							else
							{
								reader.skipValue();
							}
						}
						reader.endObject();
						try
						{
							if (commandType.length() > 0)
							{
								String className = "telecom." + commandModule + ".model." + commandFamily + "." + commandType;
								Class<?> clazz = Class.forName(className);
								Constructor<?> ctor = clazz.getConstructor(HashMap.class);

								if (WirelessLoopers.containsKey(commandFamily))
								{
									WirelessLoopers.get(commandFamily).addCommand((WirelessNetworkCommand) ctor.newInstance(new Object[] {commandParams}));
								}
							}
						}
						catch (ClassNotFoundException e)
						{
    							Log.e(getClass().getSimpleName(), "++ " + "ClassNotFoundException parsing json configuration file: "+e.getMessage());
						}
						catch (NoSuchMethodException e)
						{
    							Log.e(getClass().getSimpleName(), "++ " + "NoSuchMethodException parsing json configuration file: "+e.getMessage());
						}
						catch (InstantiationException e)
						{
    							Log.e(getClass().getSimpleName(), "++ " + "InstantiationException parsing json configuration file: "+e.getMessage());
						}
						catch (IllegalAccessException e)
						{
    							Log.e(getClass().getSimpleName(), "++ " + "IllegalAccessException parsing json configuration file: "+e.getMessage());
						}
						catch (java.lang.reflect.InvocationTargetException e)
						{
    							Log.e(getClass().getSimpleName(), "++ " + "InvocationTargetException parsing json configuration file: "+e.getMessage());
							e.printStackTrace();
						}
					}
				}
				reader.endObject();
			}
			catch (java.io.FileNotFoundException e )
			{
    				Log.e(getClass().getSimpleName(), "++ " + "FileNotFoundException trying to access command file: " +e.getMessage());
				return;
			}
			catch (IOException e )
			{
    				Log.e(getClass().getSimpleName(), "++ " + "IOException trying to access command file file: " +e.getMessage());
				return;
			}


			for (IWirelessNetworkCommandLooper looper:WirelessLoopers.values())
			{
				looper.initializeCommands(parameters);
			}
		        
		        parameters.setParameter(Parameter.WIFI_WORKING_FLAG, wifiWorkingFlag);
		        parameters.setParameter(Parameter.CELL_WORKING_FLAG, cellWorkingFlag);

		        if ((Boolean)parameters.getParameter(Parameter.LOCK_NETWORK))
		       	{ 
				LockNetwork();
			}

			ControllerServices.getInstance().getMove().resetLastMovementTimestamp();
			TraceManager.initializeManager(parameters);
			showNotification();
			startThreads();
			setRunning(true);
		}
		
		public void stop()
		{
	    		stopThreads();
	    
		        if ((Boolean)parameters.getParameter(Parameter.LOCK_NETWORK))
			{
				UnlockNetwork();
			}
			
			for (IWirelessNetworkCommandLooper looper:WirelessLoopers.values())
			{
				looper.finalizeCommands(parameters);
			}
			
			TraceManager.finalizeManager();
			
		        // Cancel the persistent notification.
		        mNM.cancel(NOTIFICATION);
	        
	        
	    		if (DatabaseHelper.isInitialized())
			{
				Logger.getInstance().flush();
	    			DatabaseHelper.getDatabaseHelper().closeDatabase();
			}
		    	setRunning(false);
		}
		
    		public IControllerServices getServices()
		{
	    		return ControllerServices.getInstance();
    		}
    	
	    	public ILogger getLogger()
		{
    			return Logger.getInstance();
	    	}
    	
    		public boolean isFirstRun()
		{
    			boolean ret = firstRun;
    			firstRun = false;
	    		return ret;
	   	}

		public synchronized void setBound(boolean b)
		{
			this.bound = b;
		}
		
		public synchronized boolean isBound()
		{
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
    	
	}
    
	private class BatteryLevelSupervisor implements IBatteryLevelReceiver
	{
    	
		private int minLevel;
    	
    		public BatteryLevelSupervisor(int minLevel)
		{
    			this.minLevel = minLevel;
	    	}

    		@Override
	    	public void receiveBatteryLevel(int batteryLevel)
		{
    			if (batteryLevel <= minLevel)
			{
    				Log.d(getClass().getSimpleName(), "++ " + "Stopping Service");
    				binder.isBateryLow = true;
    				stopSelf(startId);
    				if (binder.isBound())
				{
    					Toast.makeText(context, "Minimum battery level reached (" + batteryLevel + "%)." +
    							" Meassuring will stop when application closes", Toast.LENGTH_LONG).show();
    				}
				else
				{
    					Toast.makeText(context, "Minimum battery level reached (" + minLevel + "%)." +
    							" Meassuring stopped", Toast.LENGTH_LONG).show();
    				}
    			}
    		}
    	

    	}
   

	private ArrayList<String> popIPTablesCommand(String command)
	{	
		ArrayList<String> retval = new ArrayList<String>();	
		OutputStreamWriter osw = null;
		try
		{
			Process process = Runtime.getRuntime().exec("su");
			osw = new OutputStreamWriter(process.getOutputStream());
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;	

			osw.write(command);
			osw.flush();
			osw.close();
 
			while ((line = bufferedReader.readLine()) != null)
			{	
		    		retval.add(line);	
			}
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Error popping iptables command : " + command + " "+ e.getMessage(), e);
		}
		return retval;
	}



	private void LockNetwork()
	{

		
		int UID_OFFSET = 10000;
		int uid = 0;
		ArrayList iptablesRetVal = new ArrayList<String>();	


		//Create Chain : 
	      	iptablesRetVal = popIPTablesCommand("iptables -N wi2me");
		if (iptablesRetVal.size() > 0)
		{
			Log.e(getClass().getSimpleName(), "Error creating wi2me chain");
		}		
		else
		{
			//redirect output to wi2me chain 
		      	iptablesRetVal = popIPTablesCommand("iptables -A OUTPUT -j wi2me -p all");
			if (iptablesRetVal.size() > 0)
			{
				Log.e(getClass().getSimpleName(), "Error appending jump to wi2me rule to OUTPUT");
			}
		
			else
			{		
				//Allow traffic for wi2Me : 
				uid = context.getApplicationInfo().uid - UID_OFFSET;
		      		iptablesRetVal = popIPTablesCommand("iptables -A wi2me -m owner --uid-owner u0_a" + uid + " -j RETURN ");
				if (iptablesRetVal.size() > 0)
				{
					Log.e(getClass().getSimpleName(), "Error adding rule to let wi2me traffic through");
				}
				else
				{
				     	//Allow DNS
		      			iptablesRetVal = popIPTablesCommand("iptables -A wi2me -p udp --dport 53 -j RETURN");  
					if (iptablesRetVal.size() > 0)
					{
						Log.e(getClass().getSimpleName(), "Error appending rule to let DNS through");
					}
					else
					{
						//Block all the rest
		      				iptablesRetVal = popIPTablesCommand("iptables -A wi2me -p all -j REJECT");
						if (iptablesRetVal.size() > 0)
						{
							Log.e(getClass().getSimpleName(), "Error appending non wi2me traffic blocking rule");
						}
					}
				}
			}
		}
	}

	private void UnlockNetwork()
	{
		ArrayList OutputRules = new ArrayList<String>();
		ArrayList iptablesRetVal = new ArrayList<String>();
		String WI2ME_RULE_NAME = "wi2me";
		boolean deleted = true;

		while (deleted)
		{
			deleted = false;
			OutputRules = popIPTablesCommand("iptables -L OUTPUT");
		  	for (int i = 2; i < OutputRules.size(); i++)
			{
				if (((String) OutputRules.get(i)).startsWith(WI2ME_RULE_NAME))
				{

					iptablesRetVal = popIPTablesCommand("iptables -D OUTPUT " + (i - 1));
					if (iptablesRetVal.size() > 0)
					{
						Log.e(getClass().getSimpleName(), "Error deleting jump from OUTPUT to wi2me");
						deleted = false;
					}
					else
					{
						deleted = true;
					}
					break;
				}
			}
		}

		//Clear wi2me rules
		int ruleNum  =popIPTablesCommand("iptables -L wi2me").size() - 2;
		for (int i=0; i < ruleNum; i++)
		{
			popIPTablesCommand("iptables -D wi2me 1");
		}
		
		
		iptablesRetVal = popIPTablesCommand("iptables -X wi2me ");
		if (iptablesRetVal.size() > 0)
		{
			Log.e(getClass().getSimpleName(), "Error deleting wi2me Chain");
		}
	}

	// Returns false if network is unlocked, true otherwise	
	private boolean isNetworkLocked()
	{		
		boolean retval = true;
		ArrayList iptablesRetVal = new ArrayList<String>();

	      	iptablesRetVal = popIPTablesCommand("iptables -L wi2me");
		//Not stdout return value : we got an error meaning no rules were found, so we are ok, otherwise, it we are still locked
		retval = (iptablesRetVal.size() > 0);
		return retval;

	}


}
