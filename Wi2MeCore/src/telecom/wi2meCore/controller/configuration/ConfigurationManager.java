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

package telecom.wi2meCore.controller.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import telecom.wi2meCore.model.entities.User;
import telecom.wi2meCore.model.IWirelessNetworkCommandLooper;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.LooperCommand;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.CryptoUtils;
import telecom.wi2meCore.model.WirelessNetworkCommandLooper;
import telecom.wi2meCore.model.WirelessNetworkCommand;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.Toast;



/**This class loads the parameters stored in the files.
 * @author XXX + Gilles Vidal
 */
public class ConfigurationManager
{

	public static String REMOTE_UPLOAD_DIRECTORY;
	public static String SERVER_IP;
	public static String CONNECTION_CHECK_URL;
	public static String COMMAND_FILE;

	public static final String ARRAY_SEP = " ";

	public static final String TXP_FILE = "/statistics/tx_packets";
	public static final String RXP_FILE = "/statistics/rx_packets";
	public static final String TXB_FILE = "/statistics/tx_bytes";
	public static final String RXB_FILE = "/statistics/rx_bytes";
	public static final String RETRIES_FILE = "/wireless/retries";
	public static final String TCP_FILE = "/proc/net/tcp";
	public static final String TCP6_FILE = "/proc/net/tcp6";
	public static final String UDP_FILE = "/proc/net/udp";
	public static final String UDP6_FILE = "/proc/net/udp6";
	public static String WI2ME_DIRECTORY = "/Wi2Me/";
	public static final String ENCRYPTIONKEY_DIRECTORY = "accountKeys/";
	public static final String JSON_COMMAND_DIRECTORY = "configs/";
	public static final String CONFIG_FILE = "traces.conf.txt";
	public static final String AP_GRADE_FILE = "apgrades.conf.txt";
	public static final String COMMUNITY_ACCOUNTS_FILE = "communityaccounts.conf.txt";
	public static final Boolean TRIAL = false;
	public static final int MAX_TRACES = 60000;
	public static final String ASSET_COMMAND_LOOPS = "commandLoops";

	private enum ObjectType{
		Boolean,
		Integer,
		Float
	}

	/**Parses the given object into the given type.
	 * @param object
	 * @param type
	 * @return Parsed Object
	 */
	private static Object tryParse(String object, ObjectType type)
	{
		try{
			switch (type) {
			case Boolean:
				return Boolean.parseBoolean(object);
			case Integer:
				return Integer.parseInt(object);
			case Float:
				return Float.parseFloat(object);
			default:
				return null;
			}
		}
		catch(RuntimeException e)
		{
			Log.w("ConfigurationManager", "++ " + e.getMessage(),e);
			throw e;
			//return null;
		}

	}

	/**Gives the loaded configuration
	 * @return String
	 * @throws Exception
	 */
	public static String getConfiguration() throws Exception
	{



		String ret = "";
		FileInputStream fstream = new FileInputStream(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + CONFIG_FILE);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)
		{
			//filter the user's password
			if (!strLine.contains("PASSWORD"))
			{
				// Print the content on the console
				ret += strLine + "|";
			}

		}
		ret = ret.substring(0, ret.length() - 1);
		//Close the input stream
		in.close();

		return ret;
	}

	/**Loads the parameters from the files, loads the community accounts.
	 * If one of the configuration file is missing, it creates one using the default values.
	 * If there is an exception while loading, the concerned file is renamed and a new one is generated using the default values.
	 * @param context
	 * @param parameters
	 * @throws IOException
	 */
	public static void loadParameters(Context context, IParameterManager parameters) throws IOException
	{
		WI2ME_DIRECTORY=(String) parameters.getParameter((Parameter.WI2ME_DIRECTORY)); //Returns the default value stored in the class ParameterDefaultValues.
		boolean loadingSuccessful = false;
		int type; // the type of the file that was currently read ; used in the catch to remove the defective file
		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();
		while(loadingSuccessful==false)
		{
			type=0;
			//We check if all files and directory exist
			//1. Create the directory
			File dir = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY);
			dir.mkdir();
			//2. Create the configuration file
			File file = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + CONFIG_FILE);
			if(!file.exists()){
				File outputFile = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY, CONFIG_FILE);
				FileWriter filewriter = new FileWriter(outputFile);
				BufferedWriter out = new BufferedWriter(filewriter);
				out.write(parameters.buildConfigFile());
				out.close();
			}
			//3. Create the AP grading file
			file = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + AP_GRADE_FILE);
			if(!file.exists()){
				File outputFile = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY, AP_GRADE_FILE);
				FileWriter filewriter = new FileWriter(outputFile);
				BufferedWriter out = new BufferedWriter(filewriter);
				out.close();
			}
			//4. Create the community accounts file
			file = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + COMMUNITY_ACCOUNTS_FILE);
			if(!file.exists()){
				File outputFile = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY, COMMUNITY_ACCOUNTS_FILE);
				FileWriter filewriter = new FileWriter(outputFile);
				BufferedWriter out = new BufferedWriter(filewriter);
				String ret = "";
				for(CommunityNetworks cn : CommunityNetworks.values()){
					ret=ret+communityService.getUserParameter(cn)+"=\n"+communityService.getPasswordParameter(cn)+"=\n";
				}
				out.write(ret);
				out.close();
			}

			//**********************************************************************************************************************************************\\
			//We take the parameters from the files.
			InputStream configFile = new FileInputStream(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + CONFIG_FILE);
			InputStream apGradeFile = new FileInputStream(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + AP_GRADE_FILE);
			InputStream communityAccountsFile = new FileInputStream(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + COMMUNITY_ACCOUNTS_FILE);

			//create an instance of properties class
			Properties props = new Properties();
			props.load(configFile);
			props.load(communityAccountsFile);

			//create the reader of the AP Grades File
			BufferedReader apGradeReader = new BufferedReader(new InputStreamReader(apGradeFile));

			try{
				//*************************************************************************************************************************************************
				type=1; //File CONFIG_FILE

				REMOTE_UPLOAD_DIRECTORY=props.getProperty(Parameter.REMOTE_UPLOAD_DIRECTORY.name()).toString();
				SERVER_IP=props.getProperty(Parameter.SERVER_IP.name()).toString();
				CONNECTION_CHECK_URL=props.getProperty(Parameter.CONNECTION_CHECK_URL.name()).toString();


				parameters.setParameter(Parameter.MONITORED_INTERFACES, new ArrayList<String>(Arrays.asList(props.getProperty(Parameter.MONITORED_INTERFACES.name()).split(ARRAY_SEP))));

				parameters.setParameter(Parameter.FIRST_FIX_WAITING, tryParse(props.getProperty(Parameter.FIRST_FIX_WAITING.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.CELL_CONNECTION_DELAY, tryParse(props.getProperty(Parameter.CELL_CONNECTION_DELAY.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.WIFI_SCAN_INTERVAL, tryParse(props.getProperty(Parameter.WIFI_SCAN_INTERVAL.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.WIFI_THRESHOLD, tryParse(props.getProperty(Parameter.WIFI_THRESHOLD.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.NOT_MOVING_TIME, tryParse(props.getProperty(Parameter.NOT_MOVING_TIME.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.RUN_WIFI, tryParse(props.getProperty(Parameter.RUN_WIFI.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.RUN_CELLULAR, tryParse(props.getProperty(Parameter.RUN_CELLULAR.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.CONNECT_CELLULAR, tryParse(props.getProperty(Parameter.CONNECT_CELLULAR.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.MIN_BATTERY_LEVEL, tryParse(props.getProperty(Parameter.MIN_BATTERY_LEVEL.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.PING_PACKETS, tryParse(props.getProperty(Parameter.PING_PACKETS.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.PING_INTERVAL, tryParse(props.getProperty(Parameter.PING_INTERVAL.name()), ObjectType.Float));
				parameters.setParameter(Parameter.PING_DEADLINE, tryParse(props.getProperty(Parameter.PING_DEADLINE.name()), ObjectType.Float));
				parameters.setParameter(Parameter.NOTIFY_WHEN_WIFI_CONNECTED, tryParse(props.getProperty(Parameter.NOTIFY_WHEN_WIFI_CONNECTED.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.CONNECT_TO_OPEN_NETWORKS, tryParse(props.getProperty(Parameter.CONNECT_TO_OPEN_NETWORKS.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.SENSOR_ONLY, tryParse(props.getProperty(Parameter.SENSOR_ONLY.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.NOTIFY_SERVICE_STATUS, tryParse(props.getProperty(Parameter.NOTIFY_SERVICE_STATUS.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.USE_GPS_POSITION, tryParse(props.getProperty(Parameter.USE_GPS_POSITION.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.ALLOW_TRACE_CONNECTIONS, tryParse(props.getProperty(Parameter.ALLOW_TRACE_CONNECTIONS.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.ALLOW_UPLOAD_TRACES, tryParse(props.getProperty(Parameter.ALLOW_UPLOAD_TRACES.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.CONNECTIVITY_CHECK_FREQUENCY,tryParse(props.getProperty(Parameter.CONNECTIVITY_CHECK_FREQUENCY.name()), ObjectType.Integer));
				parameters.setParameter(Parameter.PERFORM_CONNECTIVITY_CHECK, tryParse(props.getProperty(Parameter.PERFORM_CONNECTIVITY_CHECK.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.PING_SERVER_IP, SERVER_IP);
				parameters.setParameter(Parameter.LOCK_NETWORK, tryParse(props.getProperty(Parameter.LOCK_NETWORK.name()), ObjectType.Boolean));
				parameters.setParameter(Parameter.STORAGE_TYPE, tryParse(props.getProperty(Parameter.STORAGE_TYPE.name()), ObjectType.Integer));


				COMMAND_FILE = props.getProperty(Parameter.COMMAND_FILE.name());
				parameters.setParameter(Parameter.COMMAND_FILE, COMMAND_FILE);


				//*************************************************************************************************************************************************
				type=2;//File COMMUNITY_ACCOUNTS_FILE
				List<CommunityNetworks> communityNets = new ArrayList<CommunityNetworks>();
				List<User> users = new ArrayList<User>();
				String KEY_FILE;
				String decryptedPwd;

				for(CommunityNetworks cn:CommunityNetworks.values()){
					if(!props.getProperty(communityService.getUserParameter(cn)).equals("") && !props.getProperty(communityService.getPasswordParameter(cn)).equals("")){
						KEY_FILE = Environment.getExternalStorageDirectory()+WI2ME_DIRECTORY+ENCRYPTIONKEY_DIRECTORY+communityService.getEncryptionKeyLocation(cn);
						try {
							decryptedPwd = CryptoUtils.decrypt(props.getProperty(communityService.getPasswordParameter(cn)), new File(KEY_FILE));
						 	User user = new User(props.getProperty(communityService.getUserParameter(cn)),decryptedPwd,cn);
							communityNets.add(cn);
							users.add(user);
						} catch (GeneralSecurityException e) {
							Log.e("ConfigurationManager","++ "+e.toString());
						}

					}
				}
				parameters.setParameter(Parameter.COMMUNITY_NETWORK_USERS, users);
				parameters.setParameter(Parameter.COMMUNITY_NETWORKS, communityNets);

				//*************************************************************************************************************************************************
				type=3;//File AP_GRADE_FILE
				//Get the grades for each AP graded
				HashMap<String,Integer> apGradeMap = new HashMap<String,Integer>();
				apGradeMap.put("NotNull",0); //to make sure the apGradeMap is not empty
				String line;
				while ((line = apGradeReader.readLine()) != null) {
					String[] RowData = line.split("=");
					if(RowData[0]!=null&&RowData[1]!=null){
						apGradeMap.put(RowData[0],Integer.parseInt(RowData[1]));
					}
				}
				parameters.setParameter(Parameter.AP_GRADE_MAP, apGradeMap);

				loadingSuccessful=true;

			}
			catch (RuntimeException e)
			{
				//If there is an error, we delete all the concerned configuration files if necessary.
				File fileToRename;
				switch (type){
				case 0:
					throw e;
				case 1:
					fileToRename = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + CONFIG_FILE);
					fileToRename.renameTo(new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + CONFIG_FILE +".defective"));
					break;
				case 2:
					fileToRename = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + COMMUNITY_ACCOUNTS_FILE);
					fileToRename.renameTo(new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + COMMUNITY_ACCOUNTS_FILE +".defective"));
					break;
				case 3:
					fileToRename = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + AP_GRADE_FILE);
					fileToRename.renameTo(new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + AP_GRADE_FILE +".defective"));
					break;
				}
				Log.e("ConfigurationManager","++ Error Detected when loading configuration files, removing them! File type: " + type);
				CharSequence text = "Error loading configuration files! Parameters have been reset! Defective files available.";
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		}




		//If there is no config dir, create it and load the built in configs along.
		File dir = new File(Environment.getExternalStorageDirectory() + WI2ME_DIRECTORY + JSON_COMMAND_DIRECTORY);
		if (!dir.exists())
		{
			dir.mkdir();
		}

		try
		{
			String[] fileList = ControllerServices.getInstance().getAssets().list( ASSET_COMMAND_LOOPS);
			for (String path : fileList)
			{
				File assetFile = new File(path);
				File outFile = new File(dir.getPath() + "/" + assetFile.getName());
				if(!outFile.exists())
				{
    				HashMap<String, IWirelessNetworkCommandLooper> loopers = readCommandFile(ControllerServices.getInstance().getAssets().getStream(ASSET_COMMAND_LOOPS + "/" + path));
					if (loopers.size() > 0)
					{
						saveCommandFile(loopers, outFile.getPath());
					}
				}
			}
		}
		catch (IOException e )
		{
			Log.e("ConfigurationManager", "++ " + "IOException trying to list default configs " +e.getMessage());
		}
	}


    public static HashMap<String, IWirelessNetworkCommandLooper> readCommandFile(InputStream stream) throws IOException
	{
    	HashMap<String, IWirelessNetworkCommandLooper> retval = new HashMap<String, IWirelessNetworkCommandLooper>();
		JsonReader reader = new JsonReader(new InputStreamReader(stream));
		reader.beginObject();
		while (reader.hasNext())
		{
			String LooperName = reader.nextName();
			reader.beginObject();
			while (reader.hasNext())
			{
				String looperContentKey = reader.nextName();
				if (looperContentKey.equals("command"))
				{
					String commandModule = "";
					HashMap<String, String> commandParams = new HashMap<String, String>();
					reader.beginObject();
					while (reader.hasNext())
					{
						String key = reader.nextName();
						if (key.equals("module"))
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
						if (commandModule.length() > 0 && LooperName.length() > 0)
						{
							Class<?> clazz = Class.forName(commandModule);
							Constructor<?> ctor = clazz.getConstructor(HashMap.class);
							if (!retval.containsKey(LooperName))
							{
								retval.put(LooperName, new WirelessNetworkCommandLooper());
							}
							retval.get(LooperName).addCommand((WirelessNetworkCommand) ctor.newInstance(new Object[] {commandParams}));
						}
					}
					catch (ClassNotFoundException e)
					{
    						Log.e("ConfigurationManager", "++ " + "ClassNotFoundException parsing json configuration file: "+e.getMessage());
					}
					catch (NoSuchMethodException e)
					{
    						Log.e("ConfigurationManager", "++ " + "NoSuchMethodException parsing json configuration file: "+e.getMessage());
					}
					catch (InstantiationException e)
					{
    						Log.e("ConfigurationManager", "++ " + "InstantiationException parsing json configuration file: "+e.getMessage());
					}
					catch (IllegalAccessException e)
					{
    						Log.e("ConfigurationManager", "++ " + "IllegalAccessException parsing json configuration file: "+e.getMessage());
					}
					catch (java.lang.reflect.InvocationTargetException e)
					{
    					Log.e("ConfigurationManager", "++ " + "InvocationTargetException parsing json configuration file: "+e.getMessage());
						e.printStackTrace();
					}
				}
			}
			reader.endObject();
		}
		reader.endObject();

		return retval;

	}

	public static void saveCommandFile(HashMap<String, IWirelessNetworkCommandLooper> loopers, String path) throws IOException
	{
		try
		{

	    	JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
			writer.setIndent("  ");
			writer.beginObject();

			for (String looperName : loopers.keySet())
			{
				writer.name(looperName);
				writer.beginObject();
				for (WirelessNetworkCommand command : loopers.get(looperName).getCommands())
				{

					HashMap<String, String> parameters = command.getParameters();
					String module = command.getSubclassName();

					writer.name("command");
					writer.beginObject();
					writer.name("module").value(module);
					if (parameters.size() > 0)
					{
						writer.name("params");
						writer.beginObject();
						for (Entry<String, String> param : parameters.entrySet())
						{
							writer.name(param.getKey()).value(param.getValue());
						}
						writer.endObject();
					}
					writer.endObject();
				}
				writer.endObject();
			}
			writer.endObject();
			writer.close();
		}
		catch (IOException e )
		{
			Log.e("ConfigurationManager", "++ " + "IOException trying to access configuration file: " +e.getMessage());
		}
	}
}
