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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.parameters.LooperCommand;
import telecom.wi2meRecherche.controller.ApplicationService;
import telecom.wi2meRecherche.controller.ApplicationService.ServiceBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class Wi2MePreferenceActivity extends PreferenceActivity
{

	private static final String CONFIG_FILE = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.CONFIG_FILE;
	private int batterylevelsetted;

	private static final int PICK_COMMAND_FILE = 0;

	CheckBoxPreference runWIFI;
	CheckBoxPreference runCellular;
	CheckBoxPreference lockNetwork;
	CheckBoxPreference useGPS;
	CheckBoxPreference connectCellular;
	CheckBoxPreference openNetwork;
	Preference battery;
	EditTextPreference threshold;
	EditTextPreference scanInterval;
	MultiSelectListPreference MonitoredInterfaces;
	Preference advanced;
	PreferenceCategory commandLoopCategory;
	ListPreference storageType;

	Preference about;

	Preference commandFile;

	Activity current;

	ServiceBinder binder;
	ServiceConnection serviceConnection;

	private List<LooperCommand> LooperCommands = new ArrayList<LooperCommand>();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		current = this;
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preference);

		runWIFI = (CheckBoxPreference) findPreference("RUN_WIFI");
		runCellular=(CheckBoxPreference) findPreference("RUN_CELLULAR");
		connectCellular =(CheckBoxPreference) findPreference("CONNECT_CELLULAR");
		openNetwork=(CheckBoxPreference) findPreference("CONNECT_TO_OPEN_NETWORKS");
		battery=(Preference) findPreference("MIN_BATTERY_LEVEL");
		threshold = (EditTextPreference) findPreference("WIFI_THRESHOLD");
		scanInterval = (EditTextPreference) findPreference("WIFI_SCAN_INTERVAL");
		MonitoredInterfaces = (MultiSelectListPreference) findPreference("MONITORED_INTERFACES");
		commandFile = (Preference) findPreference("COMMAND_FILE");
		commandLoopCategory = (PreferenceCategory) findPreference("COMMAND_LOOP_CATEGORY");
		lockNetwork=(CheckBoxPreference) findPreference("LOCK_NETWORK");
		useGPS=(CheckBoxPreference) findPreference("USE_GPS");

		storageType =(ListPreference) findPreference("STORAGE_TYPE");

		advanced =(Preference) findPreference("Advanced");
		about =(Preference) findPreference("About");

	}


	public void onResume()
	{
		super.onResume();

		serviceConnection = new ServiceConnection()
		{
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
					else
					{
						loadConfig();
					}
				}
			}
			public void onServiceDisconnected(ComponentName name) {}
		};

		bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public void onStop()
	{
		super.onStop();
	}

	public void loadConfig()
	{
		// Set parameter RUN_WIFI

		try {
			runWIFI.setChecked(Boolean.parseBoolean(readProperties("RUN_WIFI")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		runWIFI.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {

				try {
					changeParameter((Boolean)arg1, Parameter.RUN_WIFI);
					runWIFI.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return true;
			}

		});

		// Set parameter RUN_Cellular
		try {
			runCellular.setChecked(Boolean.parseBoolean(readProperties("RUN_CELLULAR")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		runCellular.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {

				try {
					changeParameter((Boolean)arg1, Parameter.RUN_CELLULAR);
					runCellular.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return true;
			}

		});

		// Set parameter Connect_Cellular

		try {
			connectCellular.setChecked(Boolean.parseBoolean(readProperties("CONNECT_CELLULAR")));
		} catch (IOException e1) {
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}
		connectCellular.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {

				try {
					changeParameter((Boolean)arg1, Parameter.CONNECT_CELLULAR);
					connectCellular.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return true;
			}

		});

		// Set parameter ConnectionToOpenNetwork
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
				return true;
			}

		});

		// Set parameter batteryLevel
		battery.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{

			@Override
			@SuppressLint("InflateParams")
			public boolean onPreferenceClick(Preference p)
			{
				LayoutInflater factory = LayoutInflater.from(current);
				View view = factory.inflate(R.layout.batterylevelpopup, null);

				final SeekBar batterySeekBar = (SeekBar)view.findViewById(R.id.batteryseekBar);
				final TextView batteryLevel=(TextView) view.findViewById (R.id.batteryLevel);

				try {
					batteryLevel.setText(String.format(getResources().getString(R.string.PERCENTAGE), readProperties("MIN_BATTERY_LEVEL")));
					batterySeekBar.setProgress(Integer.parseInt(readProperties("MIN_BATTERY_LEVEL")));

				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);

				}

				AlertDialog.Builder builder = new AlertDialog.Builder(current);
				builder.setTitle("Battery Level");
				builder.setView(view);

				builder.setNegativeButton("Cancel", new OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						}
				});

				builder.setPositiveButton("Save", new OnClickListener()
				{

					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						try {
							changeParameter(batterylevelsetted, Parameter.MIN_BATTERY_LEVEL);
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
						}

						}
				});





				builder.show();

				batterySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
				{

					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						if(fromUser){

								batteryLevel.setText(Integer.toString(progress)+"%");
							batteryLevel.setText(String.format(getResources().getString(R.string.PERCENTAGE), Integer.toString(progress)));
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

				return true;
			}
		});




		// set threshold parameter

		try {
			int threshold_read = Integer.parseInt(readProperties("WIFI_THRESHOLD"));
			threshold.setText(String.valueOf(threshold_read));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		threshold.setNegativeButtonText("Cancel");
		threshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {

						try {
							changeParameter(Integer.parseInt((String)newValue), Parameter.WIFI_THRESHOLD);
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

				return true;
			}
		});


		// set scanInterVal parameter
		try
		{
			int scanInterval_read = Integer.parseInt(readProperties("WIFI_SCAN_INTERVAL"));
			scanInterval.setText(String.valueOf(scanInterval_read/1000));
		}
		catch (IOException e)
		{
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
						Toast.makeText(current, "Please enter only numbers", Toast.LENGTH_LONG).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
		}});

		// Set parameter MONITORED_INTERFACES

		File dir = new File("/sys/class/net/"); //TKER TODO From config
		File[] files = dir.listFiles();
		List<String> ifaces = new ArrayList<String>();
		List<String> ifaceValues = new ArrayList<String>();

		for (File f : files)
		{
			if (f.isDirectory())
			{
				ifaces.add(f.getName());
				ifaceValues.add(f.getName());
			}

		}


		//Select monitored interfaces
		MonitoredInterfaces.setEntries(ifaces.toArray(new CharSequence[ifaces.size()]));
		MonitoredInterfaces.setEntryValues(ifaceValues.toArray(new CharSequence[ifaceValues.size()]));
		MonitoredInterfaces.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				boolean retval = true;

				List<String> newVals = new ArrayList<String>((HashSet<String>) newValue);

				MultiSelectListPreference mpreference = (MultiSelectListPreference) preference;
				try
				{
					changeParameter(newVals, Parameter.MONITORED_INTERFACES);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					retval = false;
				}
				return retval;
			}

		});

		// Set parameter LOCK_NETWORK

		try
		{
			lockNetwork.setChecked(Boolean.parseBoolean(readProperties("LOCK_NETWORK")));
		}
		catch (IOException e1)
		{
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}

		lockNetwork.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1)
			{
			try {
					changeParameter((Boolean)arg1, Parameter.LOCK_NETWORK);
					lockNetwork.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return true;
			}

		});


		// Set parameter USE_GPS
		try
		{
			useGPS.setChecked(Boolean.parseBoolean(readProperties("USE_GPS")));
		}
		catch (IOException e1)
		{
			Log.e(getClass().getSimpleName(), "++ " + e1.getMessage(), e1);
		}

		useGPS.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1)
			{
			try {
					changeParameter((Boolean)arg1, Parameter.USE_GPS_POSITION);
					useGPS.setChecked((Boolean)arg1);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				return true;
			}

		});


		//storage type
		storageType.setEntries(ifaces.toArray(new CharSequence[ifaces.size()]));
		storageType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				boolean retval = true;

				List<String> newVals = new ArrayList<String>((HashSet<String>) newValue);

				ListPreference mpreference = (ListPreference) preference;
				try
				{
					changeParameter(newVals, Parameter.STORAGE_TYPE);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					retval = false;
				}
				return retval;
			}

		});

		//command file
		commandFile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference p)
			{
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath() +  ConfigurationManager.WI2ME_DIRECTORY + ConfigurationManager.JSON_COMMAND_DIRECTORY), "*/*");
				startActivityForResult(intent, PICK_COMMAND_FILE);
				return true;
			}

		});

		// advanced configuration - edit file

		advanced.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{

			@Override
			@SuppressLint("InflateParams")
			public boolean onPreferenceClick(Preference p) {

				LayoutInflater factory = LayoutInflater.from(current);
				View view = factory.inflate(R.layout.advancedconfigurationpopup, null);

				final EditText configurationEdit = (EditText)view.findViewById(R.id.configurationEdit);
				File confFile = new File(Environment.getExternalStorageDirectory() + CONFIG_FILE);
				StringBuilder text = new StringBuilder();
				try {
					if (confFile.exists())
					{

						BufferedReader br = new BufferedReader(new FileReader(confFile));
						String line;

						while ((line = br.readLine()) != null)
						{
								text.append(line);
								text.append('\n');
						}
					}
				}catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}

				configurationEdit.setText(text);

				AlertDialog.Builder builder = new AlertDialog.Builder(current);
				builder.setTitle("Advanced configuration");
				builder.setView(view);

				builder.setNegativeButton("Cancel", new OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						}
				});


				builder.setPositiveButton("Save", new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{

						try
						{
							String config = configurationEdit.getText().toString();
							FileWriter filewriter = new FileWriter(Environment.getExternalStorageDirectory() + CONFIG_FILE);
							BufferedWriter out = new BufferedWriter(filewriter);
							out.write(config);
							out.close();
							filewriter.close();
						} catch (IOException e) {
							Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
						}
							Toast.makeText(current, "CONFIGURATION FILE MODIFIED. Please restart the application to make these modifications applied.", Toast.LENGTH_LONG).show();
						}

				});





				builder.show();

				return true;

			}
		});


		about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference p)
			{
				String info = "";

				AlertDialog.Builder builder = new AlertDialog.Builder(current);

				try
				{
					PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					String version = pInfo.versionName;

					info += "Version : ";
					info += version;
					info += "\n";


					ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);

					ZipFile zf = new ZipFile(ai.sourceDir);
					ZipEntry ze = zf.getEntry("classes.dex");
					long time = ze.getTime();
					String appBuildDate = SimpleDateFormat.getInstance().format(new java.util.Date(time));

					info += "Build Date : ";
					info += appBuildDate;
					info += "\n";

				}
				catch (IOException e)
				{
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
				catch (NameNotFoundException e)
				{
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}

				builder.setMessage(info);
				builder.setTitle(getResources().getString(R.string.ABOUT_WI2ME));
				builder.show();

				return true;
			}

		});

		loadWirelessCommands();

	}

	private class CommandParamsAdapter extends BaseAdapter
	{
		private final ArrayList mData;
		private final int index;
		public CommandParamsAdapter(int commandIndex)
		{
			this.index = commandIndex;
				mData = new ArrayList();
				mData.addAll(LooperCommands.get(index).parameters.entrySet());
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Entry<String, String> getItem(int position) {
			return (Entry) mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final View result;

			if (convertView == null) {
				result = LayoutInflater.from(parent.getContext()).inflate(R.layout.commandparameter, parent, false);
			} else {
				result = convertView;
			}

			Entry<String, String> item = getItem(position);

			((TextView) result.findViewById(R.id.cpam_text1)).setText(item.getKey());
			((TextView) result.findViewById(R.id.cpam_text2)).setText(item.getValue());

			return result;
		}
	}

	public void loadWirelessCommands()
	{
		try
		{
			String commandFilePath = readProperties("COMMAND_FILE");
			LooperCommands = ConfigurationManager.readCommandFile(new FileInputStream(commandFilePath));
		}
		catch (java.io.IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ " + "IOException trying to access configuration file: " +e.getMessage());
		}
		refreshWirelessCommands();

	}

	public void refreshWirelessCommands()
	{

		PreferenceScreen screen = this.getPreferenceScreen();

		commandLoopCategory.removeAll();
		for (int i = 0; i < LooperCommands.size(); i++)
		{
			final int ii = i;

			final LooperCommand commandParam = LooperCommands.get(i);

			Preference commandPref = new Preference(screen.getContext());
			commandPref.setKey("command_" + i);
			commandPref.setTitle(commandParam.name);
			if (commandParam.parameters.size() > 0)
			{
				commandPref.setSummary(commandParam.parameters.toString());
			}

			commandPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
			{

				@Override
				public boolean onPreferenceClick(Preference p)
				{

					CommandParamsAdapter adapter = new CommandParamsAdapter(ii);
					final ListView paramView = new ListView(current);
					paramView.setAdapter(adapter);

					AlertDialog.Builder builder = new AlertDialog.Builder(current);
					builder.setTitle("Edit " + commandParam.name +" Parameters");
					builder.setView(paramView);

					builder.setNegativeButton("Cancel", new OnClickListener()
					{

						@Override
						public void onClick(DialogInterface arg0, int arg1)
						{

						}
					});

					builder.setPositiveButton("Save", new OnClickListener()
					{

						@Override
						public void onClick(DialogInterface arg0, int arg1)
						{
							int count = paramView.getAdapter().getCount();
							for (int j = 0; j < count; j++)
							{
								View lineView = paramView.getChildAt(j);
								String pKey = ((TextView)lineView.findViewById(R.id.cpam_text1)).getText().toString();
								String pVal = ((TextView)lineView.findViewById(R.id.cpam_text2)).getText().toString();
								LooperCommands.get(ii).parameters.put(pKey, pVal);
							}
							refreshWirelessCommands();
							try
							{
								String commandFilePath = readProperties("COMMAND_FILE");
								ConfigurationManager.saveCommandFile(LooperCommands, commandFilePath);
							}
							catch (java.io.IOException e)
							{
								Log.e(getClass().getSimpleName(), "++ " + "IOException trying to save command: " +e.getMessage());
							}

						}
					});


					AlertDialog dialog = builder.create();

					dialog.show();


					dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

					return true;

				}
			});
			commandLoopCategory.addPreference(commandPref);
		}
	}


	//Used to get the restult of the command file file browsing
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == PICK_COMMAND_FILE)
		{
			if (resultCode == RESULT_OK)
			{
				String path = data.getData().getPath();

				//Required for some devices
				path = path.replace("/document/primary:", Environment.getExternalStorageDirectory().getPath() + "/" );
				try
				{
					changeParameter(path, Parameter.COMMAND_FILE);
				}
				catch (IOException e)
				{
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
			}
		}
	}


	// Fonction used while a parameter is changed
	public void changeParameter(Object param, Parameter property)throws IOException
	{
		FileInputStream configFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
		Properties props = new Properties();
		props.load(configFileIn);
		configFileIn.close();

		switch(property)
		{
			case RUN_WIFI:
				props.setProperty("RUN_WIFI", param.toString());
				binder.parameters.setParameter(Parameter.RUN_WIFI, param);
				break;

			case RUN_CELLULAR:
				props.setProperty("RUN_CELLULAR", param.toString());
				binder.parameters.setParameter(Parameter.RUN_CELLULAR, param);
				break;

			case CONNECT_CELLULAR:
				props.setProperty("CONNECT_CELLULAR", param.toString());
				binder.parameters.setParameter(Parameter.CONNECT_CELLULAR, param);
				break;

			case MIN_BATTERY_LEVEL:
				props.setProperty("MIN_BATTERY_LEVEL", param.toString());
				binder.parameters.setParameter(Parameter.MIN_BATTERY_LEVEL, param);
				break;

			case CONNECT_TO_OPEN_NETWORKS:
				props.setProperty("CONNECT_TO_OPEN_NETWORKS", param.toString());
				binder.parameters.setParameter(Parameter.CONNECT_TO_OPEN_NETWORKS, param);
				break;

			case WIFI_THRESHOLD:
				props.setProperty("WIFI_THRESHOLD",param.toString());
				binder.parameters.setParameter(Parameter.WIFI_THRESHOLD, param);
				break;

			case WIFI_SCAN_INTERVAL:
				props.setProperty("WIFI_SCAN_INTERVAL",param.toString());
				binder.parameters.setParameter(Parameter.WIFI_SCAN_INTERVAL, param);
				break;
			case LOCK_NETWORK:
				props.setProperty("LOCK_NETWORK", param.toString());
				binder.parameters.setParameter(Parameter.LOCK_NETWORK, param);
				break;


			case COMMAND_FILE:
				props.setProperty("COMMAND_FILE",param.toString());
				binder.parameters.setParameter(Parameter.COMMAND_FILE, param);
				break;

			case MONITORED_INTERFACES:
				String propVal = "";
				List<String> ifaces = (List<String>) param;
				for (String nval : ifaces)
				{
					propVal += nval;
					propVal += ConfigurationManager.ARRAY_SEP;
				}
					props.setProperty("MONITORED_INTERFACES", propVal);
					binder.parameters.setParameter(Parameter.MONITORED_INTERFACES, param);
				break;
		}
		//refresh the list showed
		loadConfig();

		//modify the value of the parameter changed in the conf file
		FileOutputStream configFileOut = new FileOutputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
		props.store(configFileOut, null);
		configFileOut.close();
	}

	// fonction to get the value of a parameter in the conf file
	public String readProperties(String type) throws IOException
	{
		FileInputStream configFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + CONFIG_FILE);
		Properties props = new Properties();
		props.load(configFileIn);
		configFileIn.close();
		return props.getProperty(type);
	}

	@Override
	public void onPause(){
		super.onPause();
		if (serviceConnection != null){
			unbindService(serviceConnection);
			serviceConnection = null;
		}

	}
}


