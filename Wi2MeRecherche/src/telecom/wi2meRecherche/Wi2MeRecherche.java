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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.GZIPOutputStream;

import org.jibble.simpleftp.SimpleFTP;


import telecom.wi2meRecherche.controller.ApplicationService;
import telecom.wi2meRecherche.controller.ApplicationService.ServiceBinder;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.cell.CellInfo;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.persistance.TextTraceHelper;
import telecom.wi2meCore.controller.services.web.WebService;
import telecom.wi2meCore.model.Logger.TraceString;
import telecom.wi2meCore.model.entities.CellularConnectionData;
import telecom.wi2meCore.model.entities.CellularConnectionEvent;
import telecom.wi2meCore.model.entities.CellularScanResult;
import telecom.wi2meCore.model.entities.CustomEvent;
import telecom.wi2meCore.model.entities.WifiConnectionData;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.controller.services.StatusService;

//import pl.pawelkleczkowski.customgauge.CustomGauge;

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
import android.graphics.Color;
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
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;


public class Wi2MeRecherche extends Activity
{


	private static final int MENU_START = 0;
	private static final int MENU_STOP = 1;
	private static final int MENU_EXPORT_RESULTS = 2;
	private static final int MENU_EXPORT_LOGCAT = 3;
	private static final int MENU_ACCOUNT_MANAGEMENT = 4;
	private static final int MENU_PREFERENCES = 5;


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

	long cell_start_time = 0;
   	long wifi_start_time = 0;
	boolean wifi_start=false;
   	boolean cell_start=false;
	int lastBytesTransferred = 0;

	private Handler refreshHandler = new Handler();
	private Runnable refreshTask;
	private static final long UI_REFRESH_PERIOD = 500;
	private boolean refreshUI = true;

	private boolean buttonLoaded = false;


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


	        /*CustomGauge gauge1 = findViewById(R.id.gauge1);
			gauge1.setValue(-140);
	        CustomGauge gauge2 = findViewById(R.id.gauge2);
			gauge2.setValue(0);

			TextView gauge1Value = findViewById(R.id.textView1);
			gauge1Value.setText("-140 dBm");
			TextView gauge2Value = findViewById(R.id.textView2);
			gauge2Value.setText("0 Mbits/s");*/


        	startService();

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

			switch(item.getItemId()){
				case MENU_START:
					/*if (!binder.getServices().getLocation().isGPSEnabled() || (!binder.getServices().getLocation().isNetworkLocationEnabled() && binder.getServices().getCell().isDataTransferringEnabled()))
					{
						Log.e(getClass().getSimpleName(), "++ " + "LOCATION SATAN " + binder.getServices().getLocation().isGPSEnabled() + " " +  binder.getServices().getLocation().isNetworkLocationEnabled()  + " " + binder.getServices().getCell().isDataTransferringEnabled());
						openLocationSettings();
						break;
					}*/
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

					GridView button_grid=(GridView)findViewById(R.id.mainscreen_buttongrid);

					HashMap<String, String> custom_buttons = ConfigurationManager.getEventButtons();
				    final List<String> button_events = new ArrayList<String>(custom_buttons.keySet());
				    List<String> button_labels = new ArrayList<String>(custom_buttons.values());

    			    // Create a new ArrayAdapter
		        	final ArrayAdapter<String> gridViewArrayAdapter = new ArrayAdapter<String>
        	    	    (this,android.R.layout.simple_list_item_1, button_labels);

	        		// Data bind GridView with ArrayAdapter (String Array elements)
		    	    button_grid.setAdapter(gridViewArrayAdapter);
        		    gridViewArrayAdapter.notifyDataSetChanged();

					button_grid.setOnItemClickListener(new OnItemClickListener() {
				        @Override
        				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
						{
							binder.getLogger().log(CustomEvent.getNewCustomEvent(TraceManager.getTrace(),button_events.get(position)));
		        		}
    				});
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
				}
			}
			public void onServiceDisconnected(ComponentName name) {
			}
        	};


		bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbind(){
    	if (serviceConnection != null){
    		unbindService(serviceConnection);
    		serviceConnection = null;
			binder = null;
    	}
	}

	private void startService(){
		startService(new Intent(this, ApplicationService.class));

	}


	private void stopService(){
    		stopService(new Intent(this, ApplicationService.class));
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
    	if (binder != null && !binder.isRunning()){
    		stopService();
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

	private void addTableRow(String key, String value)
	{
		addTableRow(key, value, Color.rgb(100, 100, 100));
	}

	private void addTableRow(String key, String value, int color)
	{
		TableLayout table_layout=(TableLayout)findViewById(R.id.mainscreen_tablelayout);
		TableRow table_row = new TableRow(this);
		TextView key_text_view = new TextView(this);
		TextView value_text_view = new TextView(this);

		value_text_view.setGravity(5);

		table_row.setLayoutParams(new LayoutParams( LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
		key_text_view.setText(key);
		table_row.addView(key_text_view);
		value_text_view.setText(value);
		table_row.addView(value_text_view);
		table_row.setBackgroundColor(color);
		table_layout.addView(table_row, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}

	private void updateInfo()
	{


		TableLayout table_layout=(TableLayout)findViewById(R.id.mainscreen_tablelayout);
		//Reset table view
		while (table_layout.getChildCount() > 0)
		{
             table_layout.removeView(table_layout.getChildAt(0));
		}

		addTableRow(getResources().getString(R.string.mainscreen_localization_header), "", Color.rgb(30, 30, 30));

		if (binder != null)
		{

			int level = ControllerServices.getInstance().getCell().getLastRsrp();
			float thr = ControllerServices.getInstance().getWeb().getAverageThroughput();
	        /*CustomGauge gauge1 = findViewById(R.id.gauge1);
			gauge1.setValue(level);
			TextView gauge1Value = findViewById(R.id.textView1);
			gauge1Value.setText(level + " dBm");
	        CustomGauge gauge2 = findViewById(R.id.gauge2);
			gauge2.setValue((int)(thr * 8 / 1000));
			TextView gauge2Value = findViewById(R.id.textView2);
			gauge2Value.setText(String.format("%.3f Mbits/s", thr * 8 / 1000000));

			CellInfo cell = ControllerServices.getInstance().getCell().getLastScannedCell();
			if (cell != null)
			{
				TextView cellTextValue = findViewById(R.id.textView3);
				cellTextValue.setText(String.format("Current Cell : %x", cell.cid));
			}*/

			Location location = ControllerServices.getInstance().getLocation().getLocation();
			if (location != null)
			{
				addTableRow(getResources().getString(R.string.mainscreen_localization_provider), location.getProvider()+"/"+String.valueOf(location.getAccuracy()));
				addTableRow(getResources().getString(R.string.mainscreen_localization_latitude), String.valueOf(location.getLatitude()));
				addTableRow(getResources().getString(R.string.mainscreen_localization_longitude), String.valueOf(location.getLongitude()));
				addTableRow(getResources().getString(R.string.mainscreen_localization_speed), String.valueOf(location.getSpeed()));
			}

			//addTableRow(getResources().getString(R.string.mainscreen_localization_battery), String.valueOf(logTrace.content.getBatteryLevel())+"%");

			LinkedHashMap<String, LinkedHashMap<String, String>> looperData = binder.getLooperData();
			for (String looperKey : looperData.keySet())
			{
				addTableRow(looperKey, "", Color.rgb(30, 30, 30));
				HashMap<String, String> commandStates = looperData.get(looperKey);
				for (String commandKey : commandStates.keySet())
				{
					addTableRow(commandKey, commandStates.get(commandKey));
				}
			}
		}
		else
		{
			addTableRow("Service stopped", "", Color.rgb(30, 30, 30));
		}


		if (refreshUI)
		{
      			refreshHandler.postDelayed(refreshTask, UI_REFRESH_PERIOD);
		}
	}

}

