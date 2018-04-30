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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import telecom.wi2meRecherche.R;
import telecom.wi2meRecherche.Wi2MeRecherche;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.AssetServices;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.IControllerServices;
import telecom.wi2meCore.controller.services.NotificationServices;
import telecom.wi2meCore.controller.services.TimeService;
import telecom.wi2meCore.controller.services.ble.BLEService;
import telecom.wi2meCore.controller.services.cell.CellService;
import telecom.wi2meCore.controller.services.communityNetworks.CommunityNetworkService;
import telecom.wi2meCore.controller.services.move.MoveService;
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
import telecom.wi2meCore.model.entities.ExternalEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.WirelessNetworkCommand;

import telecom.wi2meRecherche.model.parameters.ParameterFactory;

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
	int startId;
	String configuration;
	Context context;
	ServiceBinder binder;

	private NotificationManager mNM;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = 2;

	@Override
	public void onCreate()
	{
    		super.onCreate();
    		context = this;
    		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    		parameters = ParameterFactory.getNewParameterManager();
    		binder = new ServiceBinder(parameters);


	        ControllerServices.initializeServices(new TimeService(),
							new CellService(context),
							new MoveService(context, new TimeService()),
							new WebService(context),
							new WifiService(context),
							new BatteryService(context),
							new LocationService(context),
							new AssetServices(context),
							new NotificationServices(context),
							new CommunityNetworkService(context),
							new BLEService(context)
			);

			TextTraceHelper.initialize(context);

    		try {
    			ConfigurationManager.loadParameters(context, parameters);
    			configuration = ConfigurationManager.getConfiguration();
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				//If we have problems loading the parameters file, we should tell and finish
				Toast.makeText(context, "ERROR LOADING CONFIGURATION FILE: "+e.getMessage()+". Please, check ensure USB storage is off. Otherwise, replace configuration file and try again.", Toast.LENGTH_LONG).show();
				binder.loadingError = true;
				return;
			}
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

		ControllerServices.getInstance().getLocation().monitorLocation();

		for (String looperKey:WirelessLoopers.keySet())
		{
			IWirelessNetworkCommandLooper looper = WirelessLoopers.get(looperKey);
			WirelessThreads.put(looperKey, new WirelessLooperThread(looper));
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

       	if (binder.isBateryLow)
		{
    			if (binder.isRunning)
			{
    				binder.stop();
	    			ControllerServices.getInstance().getNotification().playNotificationSound();
    			}

	    	}

    		ControllerServices.finalizeServices();

    		super.onDestroy();
	}

	private void stopThreads()
	{
		ControllerServices.getInstance().getLocation().unMonitorLocation();

		for (String looperKey:WirelessLoopers.keySet())
		{
			WirelessLoopers.get(looperKey).breakLoop();
			if (WirelessThreads.containsKey(looperKey))
			{
				WirelessThreads.get(looperKey).interrupt();//Same keys anyway
				try
				{
					WirelessThreads.get(looperKey).join(10000);
				}
				catch (InterruptedException e)
				{
					Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
				}
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
     */

    private void showNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context
                .NOTIFICATION_SERVICE);

    	String CHANNEL_ID = "telecom.wi2meRecherche.channel";
        Notification.Builder notificationBuilder = new Notification.Builder(this);

		// SDK 26 introduced a dramatic change in the way notifications are handled,
		// by introducing the NotificationChannel object. The code below implements these
		// channel modifications, and does so by using reflection. This way, this can also be built for targets under 26
		if (android.os.Build.VERSION.SDK_INT >= 26)
		{
			try
			{
				Class<?> clazz = Class.forName("android.app.NotificationChannel");
				Constructor<?> ctor = clazz.getConstructor(String.class, String.class, String.class);
				Object channel = ctor.newInstance(CHANNEL_ID, "notif_test_temp", NotificationManager.IMPORTANCE_DEFAULT);


				Method set_channel_method = notificationBuilder.getClass().getMethod("setChannelId");
				set_channel_method.invoke(notificationBuilder, CHANNEL_ID);
			}
			catch (ClassNotFoundException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (InstantiationException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (IllegalAccessException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (InvocationTargetException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (SecurityException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
			catch (NoSuchMethodException e)
			{
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			}
		}

		/*NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "notif_test_temp",
        	NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);
        channel.setSound(null, null);
		notificationManager.createNotificationChannel(channel);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
	            .setChannelId(CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Wi2Me Research")
                .setContentText("The wi2me service is running");*/

                notificationBuilder.setSmallIcon(R.drawable.icon);
                notificationBuilder.setContentTitle("Wi2Me Research");
                notificationBuilder.setContentText("The wi2me service is running");

        Notification notification = notificationBuilder.build();

        // Send the notification.
 		mNM.notify(NOTIFICATION, notification);

		startForeground(NOTIFICATION, notification);
    }


	public class ServiceBinder extends Binder
	{
    	private boolean firstRun;
   		private boolean bound;
    	public boolean loadingError;
		public Date startedDate;
		private boolean isRunning;
		public boolean isBateryLow;
		public IParameterManager parameters;

		public synchronized boolean isRunning() {
			return isRunning;
		}

		protected synchronized void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}

		public synchronized HashMap<String, HashMap<String,String>> getLooperData() {
    		HashMap<String, HashMap<String, String>> looperData = new HashMap<String, HashMap<String, String>>();

			for (String looperKey:WirelessLoopers.keySet())
			{
				looperData.put(looperKey, WirelessLoopers.get(looperKey).getStates());
			}
			return looperData;
		}

		public ServiceBinder(IParameterManager parameters){
			this.parameters = parameters;
			isBateryLow = false;
			firstRun = true;
			startedDate = new Date(Calendar.getInstance().getTimeInMillis());
	    		loadingError = false;
	    	}

		public void start()
		{
    		try {
    			ConfigurationManager.loadParameters(context, parameters);
    			configuration = ConfigurationManager.getConfiguration();
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				//If we have problems loading the parameters file, we should tell and finish
				Toast.makeText(context, "ERROR LOADING CONFIGURATION FILE: "+e.getMessage()+". Please, check ensure USB storage is off. Otherwise, replace configuration file and try again.", Toast.LENGTH_LONG).show();
				binder.loadingError = true;
				return;
			}

			WirelessLoopers = ConfigurationManager.getWirelessLoopers();
			for (IWirelessNetworkCommandLooper looper:WirelessLoopers.values())
			{
				looper.initializeCommands(parameters);
			}


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

		    mNM.cancel(NOTIFICATION);
			Logger.getInstance().flush();
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
