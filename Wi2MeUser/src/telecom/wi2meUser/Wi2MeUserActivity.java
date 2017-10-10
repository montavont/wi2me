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

package telecom.wi2meUser;


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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jibble.simpleftp.SimpleFTP;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.persistance.DatabaseHelper;
import telecom.wi2meCore.controller.services.wifi.APStatusService;
import telecom.wi2meCore.model.Logger.TraceString;
import telecom.wi2meCore.model.entities.CommunityNetworkConnectionEvent;
import telecom.wi2meCore.model.entities.WifiSnifferData;
import telecom.wi2meCore.model.entities.WifiConnectionEvent;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.wifiCommands.WifiBytesTransferedReceiver;
import telecom.wi2meUser.controller.ApplicationService;
import telecom.wi2meUser.controller.ApplicationService.ServiceBinder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Wi2MeUserActivity is the main screen of Wi2MeUser..
 * 
 * @author xxx + Xin CHEN
 */

public class Wi2MeUserActivity extends Activity {

	private static final String STOPPING_SERVICE_MESSAGE = "Stopping service...";
	private static final String NOTHING_TO_UPLOAD_MESSAGE = "Nothing to upload.";
	private static final String SENDING_DATABASE_MESSAGE = "Sending database...";
	private static final String DATABASE_UPLOAD_SUCCESSFUL_MESSAGE = "Database successfully sent!!";
	private static final String ERROR_UPLOADING_DATABASE = "An error ocurred. Check the device's storage is not mounted and that you have enough disk space, then relaunch.";
	private static final String MAIL_TO_SEND_DATABASE = "alejandro.lampropulos@telecom-bretagne.eu";
	private static final String ERROR_RUNNING_DROIDWALL = "ERROR: Drodiwall could not be launched. Is it installed on your device?";
	private static final String STOP_SERVICE_TO_UPLOAD_RESULTS_MESSAGE = "Service must be stopped first";
	private static final String DISABLE_DROIDWALL_UPLOAD_MESSAGE = "Droidwall must be disabled to send the results. Select the option to disable the Firewall, and press BACK to return and try to Upload Results again.";
	private static final String ENABLE_DROIDWALL_START_MESSAGE = "Droidwall must be enabled to start the service. Select the option to enable the Firewall, and press BACK to return and try to Start again.";
	private static final String TRIAL_EXPIRED = "Trial Version Expired";
	
	////TRIAL VARIABLES
	private Boolean trial  = ConfigurationManager.TRIAL;
	private Date trialExpire= new Date(112,8,11);

	MenuItem menuItemUpload;
	MenuItem menuItemAccountManagement;
	MenuItem menuItemPreference;
	MenuItem menuItemExit;
	StatusObserver statusObserver;
	LogObserver logObserver;

	ProgressDialog stoppingProcessDialog;
	ProgressDialog exportingProcessDialog;

	ServiceBinder binder;

	ServiceConnection serviceConnection;

	String packageName;

	Context context;

	String deviceId;

	WifiManager wifi;

	TextView connectionStatus;
	TextView network;
	TextView signalLevel;
	TextView ipadr;
	TextView channel;
	TextView bssid;
	TextView speed;

	Button btnstart;
	Button btnstop;

	Activity currentActivity;

	/** Called when the activity is first created. */
	@Override
	@SuppressLint("HardwareIds") // We actually want to log the hardware id
	public void onCreate(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "?? " + "Running onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		deviceId = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		packageName = this.getPackageName();
		context = this;

		connectionStatus = (TextView) findViewById(R.id.connectionStatus); 
		network = (TextView) findViewById(R.id.network); 
		signalLevel = (TextView) findViewById(R.id.signalLevel);  
		ipadr = (TextView) findViewById(R.id.ipadr); 
		channel = (TextView) findViewById(R.id.channel);
		bssid = (TextView) findViewById(R.id.macAdr); 
		speed = (TextView) findViewById(R.id.speed); 
		btnstart = (Button) findViewById(R.id.btnstart);
		btnstop = (Button) findViewById(R.id.btnstop);
		btnstart.setVisibility(View.GONE);

		statusObserver = new StatusObserver();
		logObserver = new LogObserver();

		currentActivity = this;


		startService();
		//if (!databaseExport)

	}

	/*public View onCreateView (String name, Context context, AttributeSet attrs){
    	Log.d(getClass().getSimpleName(), "?? " + "Running onCreateView");	
    	return btnstart;
    }*/

	/** Called when the menu key is pressed.*/

	public boolean onCreateOptionsMenu(Menu menu) { 
		Log.d(getClass().getSimpleName(), "?? " + "Running onCreateMenu");	

		menu.add(0, 0, 0, R.string.network_management).setIcon(getResources().getDrawable(R.drawable.network_management));

		menu.add(0, 1, 0, R.string.ic_menu_preferences).setIcon(getResources().getDrawable(R.drawable.ic_menu_preferences));	

		menu.add(0, 2, 0, R.string.log).setIcon(getResources().getDrawable(R.drawable.log));

		menu.add(0, 3, 0, R.string.quit).setIcon(getResources().getDrawable(R.drawable.supprimer));

		return super.onCreateOptionsMenu(menu);
	}

	/**     *  Item Network Management is connected to the Screen Wi2MeNetworkManagerActivity.java
	 *  Item Preferences is connected to the screen Wi2MePreferenceActivity.java
	 *  Item Upload is for compress the traces and upload
	 *  Item Exit is for exit the application*/
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		// TRIAL VERSION MANAGEMENT
		Date dateFormat = new Date();
		Log.d(getClass().getSimpleName(), "++ DATE LOCAL " + dateFormat);	
			
		if ((dateFormat.after(trialExpire))&&(trial)) //trial expired
		{
			Log.d(getClass().getSimpleName(), "++ TRIAL EXPIRED");
			Toast.makeText(context, TRIAL_EXPIRED, Toast.LENGTH_LONG).show();
			stopAndQuit();		
		} else 
		{

			switch(item.getItemId()){
			case 0: // Manage the networks
	
				startActivity(new Intent(this, Wi2MeNetworkManagerActivity.class));
				break;
	
	
	
			case 1:
	
				startActivity(new Intent(this, Wi2MePreferenceActivity.class));				
				break;
	
			case 2: 
	
				//service must be stopped
				if (!binder.isRunning()){
					//Export database to a text file
					exportDatabase();
	
				}else{
					Toast.makeText(this, STOP_SERVICE_TO_UPLOAD_RESULTS_MESSAGE, Toast.LENGTH_LONG).show();
				}
				break;
	
			case 3: 
				exit();
				break;
	
			} 
		}
	
			return super.onMenuItemSelected(featureId, item); 
	}
	
	
		private void exit(){
			if((binder != null ) && (binder.isRunning())){
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setTitle("Exit");
				builder.setMessage("Wi2MeUser is running. Are you sure you want to exit?");
	
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
	

	/** This function is used to stop the binder with the service and update the screen */
	private void stop() { 
		stoppingProcessDialog = new ProgressDialog(this);
		stoppingProcessDialog.setMessage(STOPPING_SERVICE_MESSAGE);
		stoppingProcessDialog.setCancelable(false);
		stoppingProcessDialog.show();
		(new Thread(){
			public void run(){/*
				stopService();
				unbind();*/
				binder.stop();
				runOnUiThread(new Runnable() {				
					@Override
					public void run() {
						APStatusService.getInstance().reset();
						StatusService.getInstance().changeStatus("Ready");
						btnstop.setVisibility(View.GONE);
						btnstart.setVisibility(View.VISIBLE);
						connectionStatus.setText("Ready");
						network.setText("");
						signalLevel.setText("");
						channel.setText("");
						bssid.setText("");
						speed.setText("");
						ipadr.setText("");
						if (stoppingProcessDialog.isShowing()) {
							stoppingProcessDialog.dismiss();
						}		
					}

				});

			}
		}).start();
	}

	/** Function called when MenuItem Upload is chosen*/
	private void exportDatabase() { 
		if (!DatabaseHelper.databaseFileExists(packageName)){//if we do not have traces there is nothing to upload
			Toast.makeText(context, NOTHING_TO_UPLOAD_MESSAGE, Toast.LENGTH_LONG).show();
			return;			
		}
		/*Log.d(getClass().getSimpleName(), "++ " + "Droidwall option run");
		if (isDroidwallEnabled()){// must be disabled
			enableOrDisableDroidwall(false);
			return;
		}*/
		/*if (!checkWifiConnection()){
			//wifi.setWifiEnabled(false); //let the user turn it on with wifi settings
			openWifiSettings();
			return;
		}*/
		runExport();		
	}

	/** Function used to compress and upload the traces 
	 * User the function "private boolean compressFile(String pathFileInput, String pathFileOutput)"
	 * and "private boolean sendFileFTP(String pathFileInput, String pathFileOutput, String server, String user, String pass)"*/
	private void runExport(){

		exportingProcessDialog = new ProgressDialog(this);
		exportingProcessDialog.setMessage(SENDING_DATABASE_MESSAGE);
		exportingProcessDialog.show();
		(new Thread(){
			String msg;
			public void run(){
				/*msg = Logger.exportDatabaseToTextFile();*/
				String compressedFileName = DatabaseHelper.DATABASE_NAME +"_"+ deviceId +"_"+ Calendar.getInstance().getTimeInMillis() + ".db.gz";

				String directory = Environment.getExternalStorageDirectory() +  ConfigurationManager.WI2ME_DIRECTORY+"traces";


				File file = new File(directory);
				if (file.exists() == false){
					file.mkdir();
				}

				String compressedFilePath = directory + "/" + compressedFileName;

				boolean successful = compressFile(Environment.getDataDirectory() + 
						"/data/" + packageName + "/databases/" + 
						DatabaseHelper.DATABASE_NAME, compressedFilePath);
				if (successful){
					msg = "Database compression successful!";
					if (sendFileFTP(compressedFilePath, ConfigurationManager.REMOTE_UPLOAD_DIRECTORY + compressedFileName, 
						ConfigurationManager.SERVER_IP, "anonymous", "")){
						msg = DATABASE_UPLOAD_SUCCESSFUL_MESSAGE;

						//erase the temporary compressed file

						File fileUpload = new File(compressedFilePath);
						/*
						if (fileUpload.exists()){
							fileUpload.delete();
						}
						 */


						if (!DatabaseHelper.isInitialized()){
							DatabaseHelper.initialize(context);
						}
						Log.d(getClass().getSimpleName(), "++ " + "DATABASE About to reset tables");
						DatabaseHelper.getDatabaseHelper().resetTables();
						Log.d(getClass().getSimpleName(), "++ " + "DATABASE New tables ready");
						DatabaseHelper.getDatabaseHelper().closeDatabase();
						Log.d(getClass().getSimpleName(), "++ " + "DATABASE closed");


					}else{
						msg = "Error. The file could not be sent. Please make sure you have internet connection. Otherwise, close the application, enable USB storage and send the '" +
								compressedFileName + "' file manually to " + MAIL_TO_SEND_DATABASE + ".";
					}

					Log.d(getClass().getSimpleName(), "++ " + "DATABASE About to be reset");
					//erase the content of this already sent (or not) database
					DatabaseHelper.resetDatabase(packageName);
					Log.d(getClass().getSimpleName(), "++ " + "DATABASE Reset OK");

				}else{
					msg = ERROR_UPLOADING_DATABASE;
				}

				runOnUiThread(new Runnable() {				
					@Override
					public void run() {
						if (exportingProcessDialog.isShowing()) {
							exportingProcessDialog.dismiss();
						}		
						Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
					}

				});

				//wifi.setWifiEnabled(false);
				//databaseExport = false;

			}
		}).start();
	}

	private void openWifiSettings() {	
		AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
		deleteAlert.setTitle("Wifi connection");
		deleteAlert.setMessage("You are not connected to a Wifi Access Point with internet connection to upload the data. Please TURN ON the interface, CHOSE an access point, connect to it, WAIT until connection is finished, press BACK and try to upload again.");
		deleteAlert.setButton("OK", new AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {    
				//databaseExport = true;				
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
					return ControllerServices.getInstance().getWeb().isOnline();
				}
			}
		}
		return false;
	}

	/** Function to establish the binder connection between this activity and the service */
	private void bind(){
		serviceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(getClass().getSimpleName(), "?? " + "Bind connection");

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

				if(!binder.isRunning()){
					btnstart.setVisibility(View.VISIBLE);
					btnstop.setVisibility(View.GONE);
				}

			}
			public void onServiceDisconnected(ComponentName name) {
			}
		};

		bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

		/*
        if (!bound){
			Log.e(getClass().getSimpleName(), "++ " + "Not bound to service");
			//If we have problems loading the parameters file, we should tell and finish
			Toast.makeText(this, "ERROR BINDING TO SERVICE", Toast.LENGTH_LONG).show();
			finish();
        }*/
	}

	/** Function to remove the connection between this activity and the service*/
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
			//binder = null;
		}		
		StatusService.getInstance().deleteObserver(statusObserver);
	}

	private void startService(){
		startService(new Intent(this, ApplicationService.class));

	}


	private void stopService(){
		stopService(new Intent(this, ApplicationService.class));
		removeObservers(); 	
	}

	private void stopAndQuit() {
		try{
			ControllerServices.getInstance().getWifi().enableKnownNetworks();
		}catch(Exception e){
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		}
		stopService();		
		finish();
	}

	@Override
	public void onBackPressed(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onBackPressed");
		exit();
		//super.onBackPressed();
	}

	public void onStart(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onStart");
		super.onStart();

	}

	public void onStop(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onStop");
		super.onStop();

	}

	public void onRestart(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onRestart");
		super.onRestart();
	}

	public void onPause(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onPause");
		super.onPause();
		unbind();
	}

	public void onResume(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onResume");
		super.onResume();
		bind();
		StatusService.getInstance().addObserver(statusObserver);
		connectionStatus.setText(StatusService.getInstance().getStatus());
		if(binder!=null){
			if(binder.isRunning()){
				btnstart.setVisibility(View.GONE);
				btnstop.setVisibility(View.VISIBLE); 
			}
			else{
				btnstart.setVisibility(View.VISIBLE);
				btnstop.setVisibility(View.GONE);
			}
		}

	}

	public void onDestroy(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onDestroy");
		super.onDestroy();
		if (!binder.isRunning()){
			stopService();
		}
	}




	/** Called when the button Start is pressed*/    
	public void onStartClick(View v) {

		// TRIAL VERSION MANAGEMENT
		Date dateFormat = new Date();
		Log.d(getClass().getSimpleName(), "++ DATE LOCAL " + dateFormat);	
			
		if ((dateFormat.after(trialExpire))&&(trial)) //trial expired
		{
			Log.d(getClass().getSimpleName(), "++ TRIAL EXPIRED");
			Toast.makeText(context, TRIAL_EXPIRED, Toast.LENGTH_LONG).show();
			stopAndQuit();		
		} else 
		{
			binder.start();
			btnstart.setVisibility(View.GONE);
			btnstop.setVisibility(View.VISIBLE);  
		}
	}


	/** Called when the button Stop is pressed*/   
	public void onStopClick(View v) {
		stop();	
	}

	/**This class observes and updates the status of the application on the screen.
	 * Observed object : StatusService (package telecom.wi2meCore.controller.services) 
	 * @author Gilles Vidal
	 */
	private class StatusObserver implements Observer{
		private String status;
		@Override
		public void update(Observable observable, Object data) {
			status=(String) data;
			runOnUiThread(new Runnable(){
				public void run(){
					connectionStatus.setText(status);
					if(!status.equals("Ready")){
						binder.showNotificationBar("Wi2Me service "+status);
					}
					if(status.contains(/*s*/"can")){
						network.setText("");
						signalLevel.setText("");
						channel.setText("");
						bssid.setText("");
						speed.setText("");
						ipadr.setText("");
					}
				}
			});
		}
	}

	/**This class observes and updates the informations of the connection on the screen.*/
	private class LogObserver implements Observer{

		private TraceString logTrace;

		@Override
		public void update(Observable observable, Object data) {			
			logTrace = (TraceString) data;
			runOnUiThread(new Runnable() {				
				@Override
				public void run() {					
					switch(logTrace.type){
					case CELL:
						// TODO case cell
						break;
					case WIFI:
						switch (logTrace.content.getStoringType()){
						case WIFI_SCAN_RESULT:
							network.setText("");
							signalLevel.setText("");
							channel.setText("");
							bssid.setText("");
							speed.setText("");
							ipadr.setText("");
							break;

						case WIFI_CONNECTION_EVENT:
							WifiConnectionEvent wificonnectionevent = (WifiConnectionEvent) logTrace.content;
							network.setText(wificonnectionevent.getConnectionTo().getSsid());
							signalLevel.setText(String.valueOf(wificonnectionevent.getConnectionTo().getLevel())+"dBm");
							channel.setText(String.valueOf(wificonnectionevent.getConnectionTo().getChannel()));
							bssid.setText(wificonnectionevent.getConnectionTo().getBSSID());
							speed.setText(String.valueOf(wificonnectionevent.getConnectionTo().getLinkSpeed())+"Mbps");
							break;


							//case WIFI_PING:

						case WIFI_SNIFFER_DATA:
							WifiSnifferData wifiSnifferdata = (WifiSnifferData) logTrace.content;
							network.setText(wifiSnifferdata.getConnectedTo().getSsid());
							signalLevel.setText(String.valueOf(wifiSnifferdata.getConnectedTo().getLevel())+"dBm");
							channel.setText(String.valueOf(wifiSnifferdata.getConnectedTo().getChannel()));
							bssid.setText(wifiSnifferdata.getConnectedTo().getBSSID());
							speed.setText(String.valueOf(wifiSnifferdata.getConnectedTo().getLinkSpeed())+"Mbps");
							ipadr.setText(wifiSnifferdata.getSnifferData().getIp());
							break;


						case COMMUNITY_NETWORK_CONNECTION_EVENT:
							CommunityNetworkConnectionEvent cnConnectionEvent = (CommunityNetworkConnectionEvent) logTrace.content;
							network.setText(cnConnectionEvent.getConnectedTo().getSsid());
							signalLevel.setText(String.valueOf(cnConnectionEvent.getConnectedTo().getLevel())+"dBm");
							channel.setText(String.valueOf(cnConnectionEvent.getConnectedTo().getChannel()));
							bssid.setText(cnConnectionEvent.getConnectedTo().getBSSID());
							speed.setText(String.valueOf(cnConnectionEvent.getConnectedTo().getLinkSpeed())+"Mbps");

							break;
						}

					}

				}
			});
		}

	}

	private String createScript(String fileName, String scriptContent) throws Exception{
		File file = new File(this.getCacheDir(), fileName);
		file.createNewFile();
		String abspath = file.getAbsolutePath();
		// make sure we have execution permission on the script file
		Runtime.getRuntime().exec("chmod 777 "+abspath).waitFor();
		// Write the script to be executed
		final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
		if (new File("/system/bin/sh").exists()) {
			out.write("#!/system/bin/sh\n");
		}
		final String script = scriptContent;
		out.write(script);
		if (!script.endsWith("\n")) out.write("\n");
		out.write("exit\n");
		out.flush();
		out.close();

		return abspath;
	}


	/** Function used to compress the traces 
	 * @param pathFileInput
	 * @param pathFileOutput*/
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
			/*
			 * type ==> a=ASCII mode, i=image (binary) mode, d= file directory
			 * listing
			 */
			//sb.append( ";type=i" );

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
					Log.e(getClass().getSimpleName(), "++ "+ioe.getMessage(), ioe);
				}
				if (bos != null)
					try
				{
						bos.close();
				}
				catch (IOException ioe)
				{
					Log.e(getClass().getSimpleName(), "++ "+ioe.getMessage(), ioe);
				}
			}
		}
		else
		{
			Log.e(getClass().getSimpleName(), "++ " + "Input not available");
		}
		return false;
			}

	/** Function used to upload the compressed traces 
	 * @param pathFileInput
	 * @param pathFileOutput
	 * @param server
	 * @param user
	 * @param pass*/
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
		/*byte[] buffer = new byte[10000];

		FTPClient client = new FTPClient();
		client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
		try {					
			int sent = 0;			
            File file = new File(pathFileInput);
            long total = file.length();
            Log.d(getClass().getSimpleName(), "++ " + "FTP About to start. File: " + pathFileInput + " - Size: " + total);
			InputStream fileStream = new FileInputStream(file);
			Log.d(getClass().getSimpleName(), "++ " + "FTP About to connect");
			client.connect(server);
			Log.d(getClass().getSimpleName(), "++ " + "FTP Connected to server");
			if (!client.login(user, pass)){
				Log.d(getClass().getSimpleName(), "++ " + "FTP Login failure");
				return false;
			}			
			Log.d(getClass().getSimpleName(), "++ " + "FTP Login OK");
			int allo = client.allo((int)total);
			Log.d(getClass().getSimpleName(), "++ " + "FTP Allocating bytes. Result: " + allo);			
			Log.d(getClass().getSimpleName(), "++ " + "FTP About to run storing command");

			OutputStream out =  client.storeFileStream(pathFileOutput);
			Log.d(getClass().getSimpleName(), "++ " + "FTP About to start sending");
            int i;                     
            while ((i = fileStream.read(buffer, 0, buffer.length)) != -1)
            {
               out.write( buffer, 0, i );
               sent += i;
               Log.d(getClass().getSimpleName(), "++ " + "FTP Uploaded Bytes: "+ sent + " of " + total);
            }           
            if (total == sent)
            	return true;            	
            return false;

			//return client.storeFile(pathFileOutput, fileStream);

		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "++ " + "FTP " + e.getMessage(), e);
			return false;
		}finally{
			try {
				Log.d(getClass().getSimpleName(), "++ " + "FTP About to disconnect");
				client.disconnect();
				Log.d(getClass().getSimpleName(), "++ " + "FTP Disconnected");
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "++ " + "FTP " + e.getMessage(), e);
			}
		}

		try {
			return upload(server, user, pass, pathFileOutput, pathFileInput);
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
			return false;
		}
		 */
	}



}
