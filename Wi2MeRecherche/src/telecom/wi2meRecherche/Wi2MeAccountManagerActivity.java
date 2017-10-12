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

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.CryptoUtils;
import telecom.wi2meCore.model.entities.User;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meRecherche.controller.ApplicationService;
import telecom.wi2meRecherche.controller.ApplicationService.ServiceBinder;
//import telecom.wi2meRecherche.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;


/** The Community Network Manager screen of Wi2MeUser.
 * @author Xin CHEN*/

public class Wi2MeAccountManagerActivity extends PreferenceActivity{

	private static String ACCOUNT_FILE = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.COMMUNITY_ACCOUNTS_FILE;
	private static String ACCOUNT_KEYS_DIRECTORY = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.ENCRYPTIONKEY_DIRECTORY;
	PreferenceActivity currentActivity;
	PreferenceCategory communityNetwork;

	ServiceBinder binder;
	ServiceConnection serviceConnection;

	String operator_trans;

	@Override
	public void onCreate(Bundle savedInstanceState) {


		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.accountmanager);

        currentActivity = this;
   	
        communityNetwork = (PreferenceCategory) currentActivity.findPreference("CommunityNetworks");

	}

	@Override
	 public void onResume(){
		
		 super.onResume();
		 communityNetwork.removeAll();
		
		 //Verify the USB storage
			File accountFile = new File(Environment.getExternalStorageDirectory() + ACCOUNT_FILE);
			if(!accountFile.exists()){
				Toast.makeText(currentActivity, "ERROR LOADING ACCOUNT FILE. Please, check ensure USB storage is off.", Toast.LENGTH_LONG).show();
			}
		
		 serviceConnection = new ServiceConnection() {
				public void onServiceConnected(ComponentName name, IBinder service) {
					Log.d(getClass().getSimpleName(), "?? " + "Bind connection");

					binder = (ServiceBinder) service;
					if (binder.loadingError){
						finish();
					}else{

						 try {
								loadAccountList(communityNetwork);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					     setListListener();

					}
				}
				public void onServiceDisconnected(ComponentName name){}
			};

			getApplicationContext().bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

	 }
	
		@Override
		public void onPause(){
	 	super.onPause();
	 	if (serviceConnection != null){
	 		getApplicationContext().unbindService(serviceConnection);
	 		serviceConnection = null;
	 	}

		}


	/** Function to show the list of the  accounts of community networks */
	public void loadAccountList(PreferenceCategory cn) throws IOException{

		// Get the list of accounts.
		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

		ArrayList<HashMap<Object,Object>> account = new ArrayList<HashMap<Object,Object>>();

    	List<User> users = (ArrayList<User>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORK_USERS);
    	for(User user: users){
    		HashMap<Object,Object> newAccount = new HashMap<Object,Object>();
    		newAccount.put("operator_name", communityService.getNameInApplication(user.getCommunityNetwork()));
    		newAccount.put("login", user.getName());
    		newAccount.put("password", user.getPassword());
    		if(!newAccount.isEmpty()){
    			account.add(newAccount);
    		}
    	}
    	// Show the list
    	if(!account.isEmpty()){
    		for(final HashMap<Object, Object> hm : account){


    		MyPreference pref = new MyPreference(this);
    		pref.setKey((String) hm.get("operator_name"));
    		pref.setTitle((String) hm.get("operator_name").toString());
    		pref.setSummary("Login : "+(String) hm.get("login"));
    		cn.addPreference(pref);


    		pref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(currentActivity, Wi2MeAccountManagementActivity.class);
					Bundle b = new Bundle();
    				b.putString("operator_name", (String) hm.get("operator_name"));
    		  		b.putString("login",(String) hm.get("login"));
    		  		b.putString("password",(String) hm.get("password"));
	    			intent.putExtras(b);
	
				  	startActivity(intent);
				  	intent = new Intent(currentActivity, Wi2MeAccountManagementActivity.class);
	    			return false;

			}});

    	}
	}
    	// Show the option for add a account
    	Preference prefadd = new Preference(this);
    	prefadd.setKey("addCN");
    	prefadd.setTitle("Add Community Network");
    	prefadd.setOnPreferenceClickListener(new OnPreferenceClickListener(){

    		@Override
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(currentActivity, Wi2MeAccountManagementActivity.class));
				return false;
			}
		});

    	cn.addPreference(prefadd);


	}

	/** Function to set a Long Click action to a item of the list */
	public void setListListener(){
        ListView listView = getListView();
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0,	android.view.View arg1, int arg2, long arg3) {
				ListView listView = (ListView) arg0;
				ListAdapter listAdapter = listView.getAdapter();
				Object obj = listAdapter.getItem(arg2);
				if (obj != null && obj instanceof android.view.View .OnLongClickListener) {
					android.view.View.OnLongClickListener longListener = (android.view.View.OnLongClickListener) obj;
					return longListener.onLongClick(arg1);
				}
				return false;
			}
		});
	}

	/** Customer item of the list of preferences */
	public class MyPreference extends Preference implements android.view.View.OnLongClickListener {

		public MyPreference(Context context) {
			super(context);
		}

		@Override
		public boolean onLongClick(android.view.View v) {
			final MyPreference item = this;
			final String operator = item.getKey();

			AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
			builder.setTitle("Deletion");
			builder.setMessage("Are you sure you want to delete this "+operator+" account?");
			builder.setPositiveButton("Yes", new OnClickListener(){

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
	    			operator_trans = operator;
				ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();
					ArrayList<CommunityNetworks> comNets = new ArrayList<CommunityNetworks>();
					for(CommunityNetworks cn:CommunityNetworks.values()){
						comNets.add(cn);
					}
	    			CommunityNetworks cnSelected=communityService.getCNFromNameInApplication(operator, comNets);
					//delete the account from the account file
					try {

					FileInputStream accountFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + ACCOUNT_FILE);
					Properties props = new Properties();
			        props.load(accountFileIn);
			        accountFileIn.close();
			        props.setProperty(communityService.getUserParameter(cnSelected), "");
			        props.setProperty(communityService.getPasswordParameter(cnSelected), "");
			        FileOutputStream configFileOut = new FileOutputStream (Environment.getExternalStorageDirectory() + ACCOUNT_FILE);
			        props.store(configFileOut, null);
			        configFileOut.close();
			
			        String fileName = Environment.getExternalStorageDirectory()+ ACCOUNT_KEYS_DIRECTORY + operator_trans + ".key";
			        // A File object to represent the filename
			        File f = new File(fileName);

			        // Make sure the file or directory exists and isn't write protected
			        if (!f.exists())
			          throw new IllegalArgumentException(
			              "Delete: no such file or directory: " + fileName);

			        if (!f.canWrite())
			          throw new IllegalArgumentException("Delete: write protected: "
			              + fileName);

			        // If it is a directory, make sure it is empty
			        if (f.isDirectory()) {
			          String[] files = f.list();
			          if (files.length > 0)
			            throw new IllegalArgumentException(
			                "Delete: directory not empty: " + fileName);
			        }

			        // Attempt to delete it
			        boolean success = f.delete();

			        if (!success)
			          throw new IllegalArgumentException("Delete: deletion failed");
			
			
			        }catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
					}

					// delete the account from parameters of ApplicationService
					ArrayList<CommunityNetworks> communityNets = (ArrayList<CommunityNetworks>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORKS);
					ArrayList<User> users = (ArrayList<User>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORK_USERS);
					ArrayList<CommunityNetworks> communityNetsCopy = (ArrayList<CommunityNetworks>) communityNets.clone();
					ArrayList<User> usersCopy = (ArrayList<User>) users.clone();

					for(CommunityNetworks cn : communityNetsCopy){
						if (communityService.getName(cn).equals(operator)){
							communityNets.remove(cn);
							System.out.println("ok");
							for(User user : usersCopy){
								if(user.getCommunityNetwork().equals(cn)){
									users.remove(user);
								}
							}
						}
					}

			        binder.parameters.setParameter(Parameter.COMMUNITY_NETWORK_USERS, users);
			        binder.parameters.setParameter(Parameter.COMMUNITY_NETWORKS, communityNets);

					// display the account
					communityNetwork.removePreference(item);

				}
			});

			builder.setNegativeButton("No", new OnClickListener(){

				@Override
				public void onClick(DialogInterface arg0, int arg1) {

					}
			});

			builder.show();

			return false;
		}
	}



}
