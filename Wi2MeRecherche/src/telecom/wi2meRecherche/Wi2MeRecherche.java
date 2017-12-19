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

package telecom.wi2meRecherche;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.GZIPOutputStream;

import org.jibble.simpleftp.SimpleFTP;


import telecom.wi2meRecherche.controller.ApplicationService;
import telecom.wi2meRecherche.controller.ApplicationService.ServiceBinder;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.persistance.TextTraceHelper;
import telecom.wi2meCore.controller.services.web.WebService;
import telecom.wi2meCore.model.Logger.TraceString;
import telecom.wi2meCore.model.entities.CellularConnectionData;
import telecom.wi2meCore.model.entities.CellularConnectionEvent;
import telecom.wi2meCore.model.entities.CellularScanResult;
import telecom.wi2meCore.model.entities.WifiConnectionData;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.controller.services.StatusService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Wi2MeRecherche extends Activity
{


	private static final int MENU_START = 0;
	private static final int MENU_STOP = 1;
	private static final int MENU_EXPORT_RESULTS = 2;
	private static final int MENU_EXPORT_LOGCAT = 3;
	private static final int MENU_ACCOUNT_MANAGEMENT = 4;
	private static final int MENU_PREFERENCES = 5;



	////TRIAL VARIABLES
	private Boolean trial  = ConfigurationManager.TRIAL;
	private Date trialExpire= new Date(112,8,11);


	LogObserver logObserver;

	ProgressDialog exportingProcessDialog;
	ProgressDialog stoppingProcessDialog;

	ServiceBinder binder;

	ServiceConnection serviceConnection;
	Activity currentActivity;

	Context context;

	String packageName;

	String deviceId;

	WifiManager wifi;



	ListView localizationView;
	ListView wifiView;
	ListView cellView;
	TextView traceView;

	MenuItem menuItemStart;
	MenuItem menuItemStop;

   	public TraceString logTrace = null;
	long cell_start_time = 0;
   	long wifi_start_time = 0;
	boolean wifi_start=false;
   	boolean cell_start=false;
	int lastBytesTransferred = 0;

	public CellularConnectionEvent mCellConnectionEvent = null;
	public CellularConnectionData mCellConnectionData = null;
	public WifiConnectionEvent mWifiConnectionEvent = null;
	public WifiConnectionData mWifiConnectionData = null;

	private Handler refreshHandler = new Handler();
	private Runnable refreshTask;
	private static final long UI_REFRESH_PERIOD = 500;
	private boolean refreshUI = true;

	/** Called when the activity is first created./ */
	@Override
	@SuppressLint("HardwareIds") // We actually want to store the hardware ID
	public void onCreate(Bundle savedInstanceState)
	{
	        super.onCreate(savedInstanceState);
        	setContentView(R.layout.mainscreen);
	        currentActivity = this;

        	wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	        deviceId = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        	packageName = this.getPackageName();
	        context = this;

        	logObserver = new LogObserver();

        	startService();
	        clearInfo();

		refreshTask = new Runnable()
		{
			@Override
			public void run()
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						updateInfo();
					}
				});
			}
		};



	}

    public boolean onCreateOptionsMenu(Menu menu) {

	    	menuItemStart=menu.add(0, MENU_START , 0, "Start");
		menuItemStart.setVisible(false);

		menuItemStop=menu.add(0, MENU_STOP , 0, "Stop");

		while (binder == null){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//Should never happen!
				Log.e(getClass().getSimpleName(), "++ " + "Waiting for bind interrupted");
			}
		}
		if (!binder.isRunning()){
			menuItemStart.setVisible(true);
			menuItemStop.setVisible(false);
		}

		menu.add(0, MENU_EXPORT_RESULTS , 0, "Export Results");

		menu.add(0, MENU_EXPORT_LOGCAT , 0, "Export Logcat");

		menu.add(0, MENU_ACCOUNT_MANAGEMENT , 0, "Account Management");

		menu.add(0, MENU_PREFERENCES , 0, "Preference");

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		// TRIAL VERSION MANAGEMENT
		Date dateFormat = new Date();
		Log.d(getClass().getSimpleName(), "++ DATE LOCAL " + dateFormat);

		if ((dateFormat.after(trialExpire))&&(trial)) //trial expired
		{
			Log.d(getClass().getSimpleName(), "++ TRIAL EXPIRED");
			Toast.makeText(context, getResources().getString(R.string.TRIAL_EXPIRED), Toast.LENGTH_LONG).show();
			stopAndQuit();
		} else
		{

			switch(item.getItemId()){

				case MENU_START:
					if ((Boolean)binder.parameters.getParameter(Parameter.USE_GPS_POSITION))
					{
						if (!binder.getServices().getLocation().isGPSEnabled() || (!binder.getServices().getLocation().isNetworkLocationEnabled() && binder.getServices().getCell().isDataTransferringEnabled()))
						{
							openLocationSettings();
							break;
						}
					}
					if ((Boolean)binder.parameters.getParameter(Parameter.CONNECT_CELLULAR)){//If user chose to connect to cellular networks, this must be enabled
						if (!binder.getServices().getCell().isDataTransferringEnabled())
						{
							openMobileNetworkSettings();
							break;
						}
					}

					binder.start();
					refreshUI = true;
      					refreshHandler.postDelayed(refreshTask, UI_REFRESH_PERIOD);
					menuItemStart.setVisible(false);
					menuItemStop.setVisible(true);
					break;
				case MENU_STOP:
					stop();
					refreshUI = false;
					menuItemStart.setVisible(true);
					menuItemStop.setVisible(false);
					break;

				case MENU_EXPORT_RESULTS:
						if (!TextTraceHelper.dataAvailable()){//if we do not have traces there is nothing to upload
							Toast.makeText(context, getResources().getString(R.string.NOTHING_TO_UPLOAD_MESSAGE), Toast.LENGTH_LONG).show();
							break;
						}

						//service must be stopped
						if (!binder.isRunning())
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(this);
							builder.setTitle(getResources().getString(R.string.EXPORT_POPUP_TITLE));
							builder.setPositiveButton(getResources().getString(R.string.EXPORT_POPUP_LOCAL),  new OnClickListener()
							{

								@Override
								public void onClick(DialogInterface arg0, int arg1)
								{
									runExport(false);
								}});
							/*builder.setNegativeButton(getResources().getString(R.string.EXPORT_POPUP_SERVER),  new OnClickListener()
							{

								@Override
								public void onClick(DialogInterface arg0, int arg1)
								{
									runExport(true);
								}})*/
							builder.show();



						}else{
							Toast.makeText(this, getResources().getString(R.string.STOP_SERVICE_TO_UPLOAD_RESULTS_MESSAGE), Toast.LENGTH_LONG).show();
						}
						break;
				case MENU_EXPORT_LOGCAT:
						exportLogcat();
						break;

				case MENU_ACCOUNT_MANAGEMENT:
						startActivity(new Intent(this, Wi2MeAccountManagerActivity.class));
						break;
				case MENU_PREFERENCES:
						startActivity(new Intent(this, Wi2MePreferenceActivity.class));
						break;
				}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void stop() {
    	stoppingProcessDialog = new ProgressDialog(this);
    	stoppingProcessDialog.setMessage(getResources().getString(R.string.STOPPING_SERVICE_MESSAGE));
       	stoppingProcessDialog.show();
		(new Thread(){
			public void run(){
				binder.stop();
		       	runOnUiThread(new Runnable() {
					@Override
					public void run() {
				       	if (stoppingProcessDialog.isShowing()) {
				            stoppingProcessDialog.dismiss();
							clearInfo();
				         }
					}

				});

			}
		}).start();
	}

	private void exportLogcat()
	{

	    try {
	      Process process = Runtime.getRuntime().exec("logcat -d -v time");

	      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

	      FileWriter outFile = new FileWriter(Environment.getExternalStorageDirectory() + "/Logcat_"+ deviceId +"_"+ Calendar.getInstance().getTimeInMillis() + ".txt");
	      PrintWriter out = new PrintWriter(outFile);

	      String line;
	      while ((line = bufferedReader.readLine()) != null) {
	    	  out.println(line);
	      }
	      out.flush();
	      out.close();

	      Toast.makeText(context, getResources().getString(R.string.LOGCAT_EXPORT_SUCCESSFUL_MESSAGE), Toast.LENGTH_LONG).show();

	    } catch (Exception e) {
	    	Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
	    	Toast.makeText(context, getResources().getString(R.string.LOGCAT_EXPORT_ERROR_MESSAGE), Toast.LENGTH_LONG).show();
	    }
	}

	private String FTPDatabaseUpload(String Path, String Name)
	{
		String msg = "";
		if (sendFileFTP(Path,
				ConfigurationManager.REMOTE_UPLOAD_DIRECTORY + Name,
				ConfigurationManager.SERVER_IP,
				"anonymous",
				""))
		{
			msg = getResources().getString(R.string.DATABASE_UPLOAD_SUCCESSFUL_MESSAGE);
		}
		else
		{
			msg = "Error. The file could not be sent. Please make sure you have internet connection. Otherwise, close the application, enable USB storage and send the file manually to " + getResources().getString(R.string.MAIL_TO_SEND_DATABASE) + ".</string>";
		}

		return msg;

	}


	private void runExport(boolean uploadDB)
	{
		final boolean upload = uploadDB;

		final String compressedFileName = TextTraceHelper.TRACEFILE_NAME +"_"+ deviceId +"_"+ Calendar.getInstance().getTimeInMillis() + ".csv.gz";
		final String compressedFilePath = Environment.getExternalStorageDirectory() + "/" + compressedFileName;

		//Only check if we intend to upload
		if (upload)
		{
			if (!checkWifiConnection()){
				wifi.setWifiEnabled(false); //let the user turn it on with wifi settings
				openWifiSettings();
				return;
			}
		}

	    	exportingProcessDialog = new ProgressDialog(this);
    		exportingProcessDialog.setMessage(getResources().getString(R.string.SENDING_DATABASE_MESSAGE));
	       	exportingProcessDialog.show();
		(new Thread(){
			String msg = "";
			@Override
			public void run()
			{
				if (compressFile(Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + TextTraceHelper.TRACEFILE_NAME, compressedFilePath))
				{
					if (upload)
						msg = FTPDatabaseUpload(compressedFilePath, compressedFileName);
					else
						msg = getResources().getString(R.string.EXPORT_OK_MESSAGE);
				}
				else
				{
					msg = getResources().getString(R.string.ERROR_COMPRESSING_DATABASE);
				}

				//erase the content of this already sent (or not) database
				TextTraceHelper.getTextTraceHelper().resetTables();

	       		runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
				       	if (exportingProcessDialog.isShowing()) {
				            exportingProcessDialog.dismiss();
				         }
				       	Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
					}

				});

			}
		}).start();
	}

	private void openMobileNetworkSettings() {
		AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
		deleteAlert.setTitle("Mobile data enabling");
		deleteAlert.setMessage("Mobile data network is disabled. Please CHECK the option to enable it, press BACK and try to Start again.");
		deleteAlert.setButton("OK", new AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
				ComponentName cName = new ComponentName("com.android.phone","com.android.phone.Settings");
				intent.setComponent(cName);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		deleteAlert.show();
	}

	private void openLocationSettings() {
		AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
		deleteAlert.setTitle("Location enabling");
		deleteAlert.setMessage("Either GPS or Network location option is disabled. Please make sure both of them are enabled, press BACK and try to Start again.");
		deleteAlert.setButton("OK", new AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		deleteAlert.show();
	}

	private void openWifiSettings() {
		AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
		deleteAlert.setTitle("Wifi connection");
		deleteAlert.setMessage("You are not connected to a Wifi Access Point with internet connection to upload the data. Please TURN ON the interface, CHOSE an access point, connect to it, WAIT until connection is finished, press BACK and try to upload again.");
		deleteAlert.setButton("OK", new AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		deleteAlert.show();
	}

	private boolean checkWifiConnection() {

		if (wifi.isWifiEnabled()){
			WifiInfo info = wifi.getConnectionInfo();
			if (info != null){
				if (info.getBSSID() != null && info.getSSID() != null){
					//check if it is also connected to the internet
					return ControllerServices.getInstance().getWeb().isOnline(WebService.Route.WIFI);
				}
			}
		}
		return false;
	}

	private void bind()
	{
        	serviceConnection = new ServiceConnection()
		{
			public void onServiceConnected(ComponentName name, IBinder service) {
				binder = (ServiceBinder) service;
				if (binder.loadingError){
					stopAndQuit();
				}else{
					binder.getLogger().addObserver(logObserver);
					if (binder.isRunning()){
						//if it was running the binder has something we can already show
						Observable observable = binder.getObservable();
						Object data = binder.getWifiData(); //first the wifi data
						if (observable != null && data != null){
							logObserver.update(observable, data);
						}
						data = binder.getCellData(); //now cell data
						if (observable != null && data != null){
							logObserver.update(observable, data);
						}
					}
				}


			}
			public void onServiceDisconnected(ComponentName name) {
			}
        	};


		bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbind(){
    	if (serviceConnection != null){
    		removeObservers();
    		unbindService(serviceConnection);
    		serviceConnection = null;
    	}
	}

	private void removeObservers(){
    		if (binder != null){
    			binder.getLogger().deleteObserver(logObserver);
    		}
	}

	private void startService(){
		startService(new Intent(this, ApplicationService.class));

	}


	private void stopService(){
    		stopService(new Intent(this, ApplicationService.class));
    		removeObservers();
	}

    private void stopAndQuit() {
    	stopService();
		finish();
	}

	private void exit(){
		if(binder.isRunning()){
			AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
			builder.setTitle("Exit");
			builder.setMessage("Wi2MeRecherche is running. Are you sure you want to exit?");

			builder.setNegativeButton("Cancel", new OnClickListener(){


				public void onClick(DialogInterface arg0, int arg1) {

				}
			});
			builder.setPositiveButton("Yes", new OnClickListener(){

				@Override
				public void onClick(DialogInterface arg0, int arg1) {

					if (binder.isRunning()){
						stop();
					}
					while (binder.isRunning()){
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							//Should never happen!
							Log.e(getClass().getSimpleName(), "++ " + "Waiting for bind interrupted");
						}
					}

					unbind();
					stopAndQuit();
				}
			});
			builder.show();
		}
		else{
			stopAndQuit();
		}
	}

    public void onBackPressed(){
    	exit();
    }

    public void onStart(){
    	super.onStart();
    }

    public void onStop(){
    	super.onStop();
    }

    public void onRestart(){
    	super.onRestart();
    }

    public void onPause(){
    	super.onPause();
    	unbind();
    }

    public void onResume(){

    	super.onResume();
	bind();
	updateInfo(); //TKE IHM empty bug ?

    }

    public void onDestroy(){
    	super.onDestroy();
    	if (!binder.isRunning()){
    		stopService();
    	}
    }


	private class LogObserver implements Observer
	{

		@Override
		public void update(Observable observable, Object data)
		{
			logTrace = (TraceString) data;
			switch(logTrace.type)
			{
				case CELL:

					switch (logTrace.content.getStoringType())
					{
						case CELL_CONNECTION_EVENT:
							mCellConnectionEvent = (CellularConnectionEvent) logTrace.content;
							break;

						case CELL_CONNECTION_DATA:
							mCellConnectionData = (CellularConnectionData) logTrace.content;
							break;
					}
					break;

				case WIFI:
					switch (logTrace.content.getStoringType())
					{
						case WIFI_CONNECTION_EVENT:

							mWifiConnectionEvent = (WifiConnectionEvent) logTrace.content;
							break;

						case WIFI_CONNECTION_DATA:
							mWifiConnectionData = (WifiConnectionData) logTrace.content;
							break;

					}
					break;
			}
		}

	}

	private boolean compressFile(String pathFileInput, String pathFileOutput){
		byte[] buffer = new byte[100000];
		InputStream file = null;
		try{
			file = new FileInputStream(pathFileInput);
            BufferedOutputStream out = new BufferedOutputStream(
                    new GZIPOutputStream(new FileOutputStream(pathFileOutput)));
              int c;
              while ((c = file.read(buffer)) != -1)
            	  out.write(buffer, 0, c);
              file.close();
              out.close();

              return true;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "++ "+e.getMessage(), e);
			return false;
		}
	}

	   public boolean upload( String ftpServer, String user, String password,
		         String fileName, String source ) throws MalformedURLException,
		         IOException
		   {
		  	 byte[] buffer = new byte[100000];
		      if (ftpServer != null && fileName != null && source != null)
		      {
		         StringBuffer sb = new StringBuffer( "ftp://" );
		         // check for authentication else assume its anonymous access.
		         if (user != null && password != null)
		         {
		            sb.append( user );
		            sb.append( ':' );
		            sb.append( password );
		            sb.append( '@' );
		         }
		         sb.append( ftpServer );
		         sb.append( '/' );
		         sb.append( fileName );

		         BufferedInputStream bis = null;
		         BufferedOutputStream bos = null;
		         File file = null;
		         try
		         {
		        	int sent = 0;
		        	long total = 0;
		            URL url = new URL( sb.toString() );
		            URLConnection urlc = url.openConnection();

		            bos = new BufferedOutputStream( urlc.getOutputStream() );
		            file = new File(source);
		            total = file.length();
		            bis = new BufferedInputStream( new FileInputStream( file ) );

		            int i;

		            while ((i = bis.read(buffer)) != -1)
		            {
		               bos.write( buffer, 0, i );
		               sent += i;
		               Log.d(getClass().getSimpleName(), "++ " + "FTP Uploaded Bytes: "+ sent + " of " + total);
		            }

		            if (total == sent)
		            	return true;
		         }
		         finally
		         {
		            if (bis != null)
		               try
		               {
		                  bis.close();
		               }
		               catch (IOException ioe)
		               {
		                  Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
		               }
		            if (bos != null)
		               try
		               {
		                  bos.close();
		               }
		               catch (IOException ioe)
		               {
		            	   Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
		               }
		         }
		      }
		      else
		      {
		    	  Log.e(getClass().getSimpleName(), "++ " + "Input not available");
		      }
		      return false;
		   }

	private boolean sendFileFTP(String pathFileInput, String pathFileOutput, String server, String user, String pass){
		SimpleFTP ftp = new SimpleFTP();
		try {
			Log.d(getClass().getSimpleName(), "++ " + "FTP About to connect");
		    // Connect to an FTP server on port 21.
		    ftp.connect(server, 21, user, pass);
		    Log.d(getClass().getSimpleName(), "++ " + "FTP Connected and logged to server");
		    // Set binary mode.
		    ftp.bin();
		    /*
		    // Change to a new working directory on the FTP server.
		    ftp.cwd("web");
		    */
		    Log.d(getClass().getSimpleName(), "++ " + "FTP About to run storing command");

		    // You can also upload from an InputStream, e.g.
		    return ftp.stor(new FileInputStream(new File(pathFileInput)), pathFileOutput);

		}
		catch (IOException e) {
			Log.e(getClass().getSimpleName(), "++ " + "FTP " + e.getMessage(), e);
			return false;
		} finally{
			try {
				Log.d(getClass().getSimpleName(), "++ " + "FTP About to disconnect");
				ftp.disconnect();
				Log.d(getClass().getSimpleName(), "++ " + "FTP Disconnected");
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "++ " + "FTP " + e.getMessage(), e);
			}
		}
	}
	/*
	private class LogcatPrintWriter extends PrintWriter{

	}*/
	/** Customer ListView to show the information in traces*/

	private class myListAdapter extends BaseAdapter
	{
		private ArrayList<HashMap<String, String>> infoTraceList;
		private LayoutInflater mInflater;
		public myListAdapter(ArrayList<HashMap<String, String>> list, Context context){
			infoTraceList = list;
			mInflater = LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			return infoTraceList.size();
		}
		@Override
		public Object getItem(int position) {
			return infoTraceList.get(position);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.griditem, null);
				holder = new ViewHolder();
				holder.traceItem = (TextView) convertView.findViewById(R.id.traceItem);
				holder.traceValue = (TextView)convertView.findViewById(R.id.traceValue);

				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.traceItem.setText((String) infoTraceList.get(position).get("traceItem"));
			holder.traceValue.setText((String) infoTraceList.get(position).get("traceValue"));
			return convertView;
		}

		public void setData(String key, String value)
		{
			boolean inserted = false;
			HashMap<String, String> newVal;
			for (HashMap<String, String> d : infoTraceList)
			{
				if (d.get("traceItem") == key)
				{
					d.put("traceValue", value);
					inserted = true;
				}
			}

			if (!inserted)
			{
				newVal = new HashMap<String, String>();
				newVal.put("traceItem", key);
				newVal.put("traceValue",value);
				infoTraceList.add(newVal);
			}
		}

		class ViewHolder {
			TextView traceItem;
			TextView traceValue;
		}


	}

	private void updateInfo()
	{

		localizationView = (ListView) findViewById(R.id.gridViewLocalization);
		RelativeLayout looperLogContainer = (RelativeLayout) findViewById(R.id.LooperLogContainer);
		myListAdapter localizationAdapter = (myListAdapter) localizationView.getAdapter();

		if (logTrace != null)
		{
			Location location = ControllerServices.getInstance().getLocation().getLocation();
			if (location != null)
			{
				localizationAdapter.setData("Provider/Accuracy (m)", location.getProvider()+"/"+String.valueOf(location.getAccuracy()));
				localizationAdapter.setData("Latitude", String.valueOf(location.getLatitude()));
				localizationAdapter.setData("Longitude", String.valueOf(location.getLongitude()));
				//localizationAdapter.setData("Timestamp (ms)", String.valueOf(location.getTimestamp()));
				localizationAdapter.setData("Speed (m/s)", String.valueOf(location.getSpeed()));
			}
			localizationAdapter.setData("Battery Level", String.valueOf(logTrace.content.getBatteryLevel())+"%");

			localizationAdapter.notifyDataSetChanged();

			traceView = (TextView) findViewById(R.id.trace);
			traceView.setText(logTrace.content.toString());
			TextView cellStatus=(TextView) findViewById(R.id.CellStatus);
			ListView dataView = (ListView) findViewById(R.id.gridViewConnection);
			TextView wifiStatus=(TextView) findViewById(R.id.wifiStatus);
			if (refreshUI)
			{
      				refreshHandler.postDelayed(refreshTask, UI_REFRESH_PERIOD);
			}

		}
	}

	private void clearInfo()
	{

		localizationView = (ListView) findViewById(R.id.gridViewLocalization);

		ArrayList<HashMap<String, String>> traceValueList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Provider/Accuracy (m)");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);
		traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Latitude");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);
		traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Longitude");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);
		traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Timestamp (ms)");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);
		traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Speed (m/s)");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);
		traceValueItem = new HashMap<String, String>();
		traceValueItem.put("traceItem", "Battery Level");
		traceValueItem.put("traceValue","");
		traceValueList.add(traceValueItem);

		myListAdapter adapter = new myListAdapter(traceValueList,currentActivity);
		localizationView.setAdapter(adapter);

		traceView = (TextView) findViewById(R.id.trace);
		traceView.setText("");


		wifiView = (ListView) findViewById(R.id.gridViewWifi);
		ArrayList<HashMap<String, String>> wifiTraceList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> wifiTraceItem = new HashMap<String, String>();
		wifiTraceItem.put("traceItem", "SSID");
		wifiTraceItem.put("traceValue", "");
		wifiTraceList.add(wifiTraceItem);
		wifiTraceItem = new HashMap<String, String>();
		wifiTraceItem.put("traceItem", "Signal strength (dBm)");
		wifiTraceItem.put("traceValue", "");
		wifiTraceList.add(wifiTraceItem);
		wifiTraceItem = new HashMap<String, String>();
		wifiTraceItem.put("traceItem", "Channel");
		wifiTraceItem.put("traceValue", "");
		wifiTraceList.add(wifiTraceItem);
		wifiTraceItem = new HashMap<String, String>();
		wifiTraceItem.put("traceItem", "BSSID");
		wifiTraceItem.put("traceValue", "");
		wifiTraceList.add(wifiTraceItem);
		wifiTraceItem = new HashMap<String, String>();
		wifiTraceItem.put("traceItem", "Rate (Mbps)");
		wifiTraceItem.put("traceValue", "");
		wifiTraceList.add(wifiTraceItem);

		myListAdapter wifiAdapter = new myListAdapter(wifiTraceList,currentActivity);
		wifiView.setAdapter(wifiAdapter);

		TextView wifiStatus=(TextView) findViewById(R.id.wifiStatus);
		wifiStatus.setText("");

		cellView = (ListView) findViewById(R.id.gridViewCell);
		ArrayList<HashMap<String, String>> cellTraceList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> cellTraceItem = new HashMap<String, String>();
		cellTraceItem.put("traceItem", "Operator Name");
		cellTraceItem.put("traceValue", "");
		cellTraceList.add(cellTraceItem);
		cellTraceItem = new HashMap<String, String>();
		cellTraceItem.put("traceItem", "Signal strength (dBm)");
		cellTraceItem.put("traceValue", "");
		cellTraceList.add(cellTraceItem);
		cellTraceItem = new HashMap<String, String>();
		cellTraceItem.put("traceItem", "Network type");
		cellTraceItem.put("traceValue", "");
		cellTraceList.add(cellTraceItem);
		cellTraceItem = new HashMap<String, String>();
		cellTraceItem.put("traceItem", "CID/LAC");
		cellTraceItem.put("traceValue", "");
		cellTraceList.add(cellTraceItem);

		myListAdapter adapterCell = new myListAdapter(cellTraceList,currentActivity);
		cellView.setAdapter(adapterCell);

		TextView cellStatus=(TextView) findViewById(R.id.CellStatus);
		cellStatus.setText("");

		ListView wifiDataView = (ListView) findViewById(R.id.gridViewConnection);
		ArrayList<HashMap<String, String>> connectionTraceList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "IP address");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);
		connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "Elapsed time (ms)");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);
		connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "Retries");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);
		connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "Tx/Rx packets");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);
		connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "Throughput (KB/s)");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);
		connectionTraceItem = new HashMap<String, String>();
		connectionTraceItem.put("traceItem", "Transfer progress");
		connectionTraceItem.put("traceValue", "");
		connectionTraceList.add(connectionTraceItem);


		myListAdapter adapterConnection = new myListAdapter(connectionTraceList,currentActivity);
		wifiDataView.setAdapter(adapterConnection);

		TextView connectionStatus=(TextView) findViewById(R.id.connectionStatus);
		connectionStatus.setText("");
	}
}

