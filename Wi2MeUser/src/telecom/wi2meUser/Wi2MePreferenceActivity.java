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


import java.io.FileInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meUser.controller.ApplicationService;
import telecom.wi2meUser.controller.ApplicationService.ServiceBinder;




import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * This is the Preferences screen of Wi2MeUser.
 * 
 * @author Xin CHEN
 */

public class Wi2MePreferenceActivity extends PreferenceActivity{
	
	private static final String CONFIG_FILE = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.CONFIG_FILE;
	private int batterylevelsetted;
	private int frequencysetted;
	
	Preference battery;
	CheckBoxPreference openNetwork;
	CheckBoxPreference dataCollection;
	CheckBoxPreference notification;
	CheckBoxPreference location;
	CheckBoxPreference connectionNotification;
	CheckBoxPreference connectivityCheck;
	ListPreference threshold;
	EditTextPreference scanInterval;
	Preference connectivityCheckFrequency;
	Activity current;
	
	ServiceBinder binder;
	ServiceConnection serviceConnection;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		current = this;
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preference); 
		
		battery=(Preference) findPreference("BatteryLevel");
		openNetwork=(CheckBoxPreference) findPreference("OpenNetwork");
		dataCollection=(CheckBoxPreference) findPreference("DataCollection");
		notification=(CheckBoxPreference) findPreference("Notification");
		location = (CheckBoxPreference) findPreference("GPSLocation");
		connectionNotification = (CheckBoxPreference) findPreference("NotifyConnection");
		threshold = (ListPreference) findPreference("Threshold");
		scanInterval = (EditTextPreference) findPreference("ScanInterval");
		connectivityCheck = (CheckBoxPreference) findPreference ("ConnectivityCheck");
		connectivityCheckFrequency = (Preference) findPreference("ConnectivityFrequency");
			
	}
	public void onResume(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onResume");
		super.onResume();
		
		serviceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(getClass().getSimpleName(), "?? " + "Bind connection");
				
				File confFile = new File(Environment.getExternalStorageDirectory() + CONFIG_FILE);
				binder = (ServiceBinder) service;	
				if (binder.loadingError){
					finish();
				}else{
					//Verify the USB storage
					if(!confFile.exists()){
						Toast.makeText(current, "ERROR LOADING CONFIGURATION FILE. Please, check ensure USB storage is off.", Toast.LENGTH_LONG).show();
						finish();
					}
					else{
						loadConfig();
					}
				}
			}
			public void onServiceDisconnected(ComponentName name) {}
		};
		
		bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

	}
	
	public void onStop(){
		Toast.makeText(this, "The modifications will ", Toast.LENGTH_LONG);
		super.onStop();		
	}
	
	/**
	 * function to load all the configurations saved
	 */
	public void loadConfig(){
		
		/**Set parameter AllowDataCollection */
		try {
			dataCollection.setChecked(Boolean.parseBoolean(readProperties("ALLOW_TRACE_CONNECTIONS")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		dataCollection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.ALLOW_TRACE_CONNECTIONS);
					dataCollection.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return false;
			}

		});		

		/** Set parameter ConnectionToOpenNetwork */
		try {
			openNetwork.setChecked(Boolean.parseBoolean(readProperties("CONNECT_TO_OPEN_NETWORKS")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		openNetwork.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.CONNECT_TO_OPEN_NETWORKS);
					openNetwork.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return false;
			}

		});
		
		/** Set parameter batteryLevel */
		battery.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference p) {
				LayoutInflater factory = LayoutInflater.from(current); 
				View view = factory.inflate(R.layout.batterylevelpopup, null);
				
				final SeekBar batterySeekBar = (SeekBar)view.findViewById(R.id.batteryseekBar);
				final TextView batteryLevel=(TextView) view.findViewById (R.id.batteryLevel);
				
				try {
					batteryLevel.setText(readProperties("MIN_BATTERY_LEVEL")+"%");
					batterySeekBar.setProgress(Integer.parseInt(readProperties("MIN_BATTERY_LEVEL")));
					
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
					
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(current);
				builder.setTitle("Battery Level");
				builder.setView(view);
				
				builder.setPositiveButton("Save", new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
												
						try {
							changeParameter(batterylevelsetted, Parameter.MIN_BATTERY_LEVEL);
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
						}
						
						}
				});
				
				builder.setNegativeButton("Cancel", new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						}
				});
				
				
				
				
				builder.show();
				
				batterySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						if(fromUser){
						
								batteryLevel.setText(Integer.toString(progress)+"%");
								batterylevelsetted = progress;
							
						}
						
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
						
					}
				});
				
				return false;
			}
		});
		
		/** Set parameter Notification */
		try {
			notification.setChecked(Boolean.parseBoolean(readProperties("NOTIFY_SERVICE_STATUS")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		
		notification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.NOTIFY_SERVICE_STATUS);
					notification.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return false;
			}

		});
		

		
		/** set GPS Location parameter */
		try {
			location.setChecked(Boolean.parseBoolean(readProperties("USE_GPS_POSITION")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		
		location.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.USE_GPS_POSITION);
					location.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return false;
			}

		});
		
		/** set notify_when_wifi_connected parameter */
		try {
			connectionNotification.setChecked(Boolean.parseBoolean(readProperties("NOTIFY_WHEN_WIFI_CONNECTED")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		
		connectionNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.NOTIFY_WHEN_WIFI_CONNECTED);
					connectionNotification.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return false;
			}

		});
		
		/**Set parameter ConnectivityCheck */
		try {
			connectivityCheck.setChecked(Boolean.parseBoolean(readProperties("PERFORM_CONNECTIVITY_CHECK")));
			if(Boolean.parseBoolean(readProperties("PERFORM_CONNECTIVITY_CHECK"))){
				connectivityCheckFrequency.setShouldDisableView(false);
				connectivityCheckFrequency.setEnabled(true);
			}
			else{
				connectivityCheckFrequency.setShouldDisableView(true);
				connectivityCheckFrequency.setEnabled(false);
			}
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		connectivityCheck.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				
				try {
					changeParameter((Boolean)arg1, Parameter.PERFORM_CONNECTIVITY_CHECK);
					connectivityCheck.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				if((Boolean)arg1){
					connectivityCheckFrequency.setShouldDisableView(false);
					connectivityCheckFrequency.setEnabled(true);
				}
				else{
					connectivityCheckFrequency.setShouldDisableView(true);
					connectivityCheckFrequency.setEnabled(false);
				}
				return false;
			}

		});		
		
		/** set threshold parameter*/
		
		threshold.setEntries(getResources().getStringArray(R.array.thresholdDisplayWord));
		threshold.setEntryValues(getResources().getStringArray(R.array.thresholdReturnValue));
		threshold.setNegativeButtonText("Cancel");
		String threshold_read;
		try {
			threshold_read = readProperties("WIFI_THRESHOLD");
			String[] threshold_array;
			threshold_array = getResources().getStringArray(R.array.thresholdReturnValue);
			ArrayList<String> list = new ArrayList<String>(Arrays.asList(threshold_array));
			int index = list.indexOf(threshold_read);
			if(index!=-1){threshold.setValueIndex(index);}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				try {
					changeParameter(Integer.parseInt((String) newValue), Parameter.WIFI_THRESHOLD);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}});
		
		/** set scanInterVal parameter */
		try {
			int scanInterval_read = Integer.parseInt(readProperties("WIFI_SCAN_INTERVAL"));
			scanInterval.setText(String.valueOf(scanInterval_read/1000));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scanInterval.setNegativeButtonText("Cancel");
		scanInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				try {
					if (((String)newValue).matches("[0-9]+")){
						changeParameter(Integer.parseInt((String)newValue)*1000, Parameter.WIFI_SCAN_INTERVAL);
					}
					else {
						//TODO
						Toast.makeText(current, "Please enter only numbers", Toast.LENGTH_LONG);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}});
		
		/** Set parameter ConnectiviyCheckFrequency */
		connectivityCheckFrequency.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference p) {
				LayoutInflater factory = LayoutInflater.from(current); 
				View view = factory.inflate(R.layout.connectivitycheckfrequencypopup, null);
				
				final SeekBar frequencySeekBar = (SeekBar)view.findViewById(R.id.connectivityCheckFrequencySeekBar);
				final TextView connectivityFrequency=(TextView) view.findViewById (R.id.connectivityCheckFrequency);
				
				try {
					connectivityFrequency.setText(String.valueOf(Integer.parseInt(readProperties("CONNECTIVITY_CHECK_FREQUENCY"))/60000)+"minutes");
					frequencySeekBar.setProgress((Integer.parseInt(readProperties("CONNECTIVITY_CHECK_FREQUENCY"))/60000)-1);
					
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
					
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(current);
				builder.setTitle("Frequency of the connectivity verification");
				builder.setView(view);
				
				builder.setPositiveButton("Save", new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
												
						try {
							changeParameter(frequencysetted, Parameter.CONNECTIVITY_CHECK_FREQUENCY);
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
						}
						
						}
				});
				
				builder.setNegativeButton("Cancel", new OnClickListener(){
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						}
				});
				
				
				
				
				builder.show();
				
				frequencySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						if(fromUser){
						
								connectivityFrequency.setText(Integer.toString(progress+1)+"minutes");
								frequencysetted = (progress+1)*60000;
							
						}
						
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
						
					}
				});
				
				return false;
			}
		});
		
	}
	
	/** Function used while a parameter is changed 
	 * @param property : the parameter to changer
	 * @param param : the new value of the parameter*/
	public void changeParameter(Object param, Parameter property)throws IOException{
		FileInputStream configFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
		Properties props = new Properties();
        props.load(configFileIn);
        configFileIn.close();
        
        switch(property){
        	case MIN_BATTERY_LEVEL:
        		props.setProperty("MIN_BATTERY_LEVEL", param.toString());
        		binder.parameters.setParameter(Parameter.MIN_BATTERY_LEVEL, param);
        		break;
        	case CONNECT_TO_OPEN_NETWORKS:
        		props.setProperty("CONNECT_TO_OPEN_NETWORKS", param.toString());
        		binder.parameters.setParameter(Parameter.CONNECT_TO_OPEN_NETWORKS, param);
        		break;
        	case NOTIFY_SERVICE_STATUS:
        		props.setProperty("NOTIFY_SERVICE_STATUS", param.toString());
        		binder.parameters.setParameter(Parameter.NOTIFY_SERVICE_STATUS, param);
        		break;
        	case USE_GPS_POSITION:
        		props.setProperty("USE_GPS_POSITION", param.toString());
        		binder.parameters.setParameter(Parameter.USE_GPS_POSITION, param);
        		break;
        	case WIFI_THRESHOLD:
        		props.setProperty("WIFI_THRESHOLD",param.toString());
        		binder.parameters.setParameter(Parameter.WIFI_THRESHOLD, param);
        		break;
        	case WIFI_SCAN_INTERVAL:
        		props.setProperty("WIFI_SCAN_INTERVAL",param.toString());
        		binder.parameters.setParameter(Parameter.WIFI_SCAN_INTERVAL, param);
                break;
        	case ALLOW_TRACE_CONNECTIONS:
        		props.setProperty("ALLOW_TRACE_CONNECTIONS",param.toString());
        		binder.parameters.setParameter(Parameter.ALLOW_TRACE_CONNECTIONS, param);
                break;
        	case NOTIFY_WHEN_WIFI_CONNECTED:
        		props.setProperty("NOTIFY_WHEN_WIFI_CONNECTED",param.toString());
        		binder.parameters.setParameter(Parameter.NOTIFY_WHEN_WIFI_CONNECTED, param);
                break;
        	case PERFORM_CONNECTIVITY_CHECK:
        		props.setProperty("PERFORM_CONNECTIVITY_CHECK",param.toString());
        		binder.parameters.setParameter(Parameter.PERFORM_CONNECTIVITY_CHECK, param);
                break;
        	case CONNECTIVITY_CHECK_FREQUENCY:
        		props.setProperty("CONNECTIVITY_CHECK_FREQUENCY",param.toString());
        		binder.parameters.setParameter(Parameter.CONNECTIVITY_CHECK_FREQUENCY, param);
                break;
        		
        }
        /** refresh the list showed when the parameter changed*/
        loadConfig();
        
        /** modify the value of the parameter changed in the configuration file */
        FileOutputStream configFileOut = new FileOutputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
        props.store(configFileOut, null);
        configFileOut.close();
	}
	
	/** function to get the value of a parameter in the configuration file
	 * @param type : the searched parameter */
	public String readProperties(String type) throws IOException{
		FileInputStream configFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
		Properties props = new Properties();
        props.load(configFileIn);
        configFileIn.close();
        return props.getProperty(type);
              
	}
	
	@Override
	public void onPause(){
    	Log.d(getClass().getSimpleName(), "?? " + "Running onPause");
    	super.onPause();
    	if (serviceConnection != null){
    		unbindService(serviceConnection);
    		serviceConnection = null;
    	}	

	}
}
 
