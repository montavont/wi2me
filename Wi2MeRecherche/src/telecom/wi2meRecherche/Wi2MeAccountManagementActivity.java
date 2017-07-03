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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabWidget;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;



/** The screen to modify or add a community network account.
 * @author Xin CHEN*/
public class Wi2MeAccountManagementActivity extends Activity
{

	private static String ACCOUNT_FILE = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.COMMUNITY_ACCOUNTS_FILE;
	private static String ACCOUNT_KEYS_DIRECTORY = ConfigurationManager.WI2ME_DIRECTORY+ConfigurationManager.ENCRYPTIONKEY_DIRECTORY;
	Activity currentActivity;

	ServiceBinder binder;
	ServiceConnection serviceConnection;

	Spinner spinner;
	EditText login;
	EditText password;
	CheckBox show_password;
	Bundle b;

	String operator_trans;

	public void onCreate(Bundle savedInstanceState) 
	{

		currentActivity = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountmanagement);
		ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();


		spinner = (Spinner) findViewById(R.id.operator_name);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
		int numberOfItems = 0;
		//Add the community networks to the list (dynamic)
		for(CommunityNetworks cn:CommunityNetworks.values())
		{
			adapter.add(communityService.getNameInApplication(cn));
			numberOfItems ++;
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		login = (EditText)findViewById(R.id.login);
		password = (EditText)findViewById(R.id.password);
		show_password = (CheckBox)findViewById(R.id.showPassword);

		b = getIntent().getExtras();
		if (b != null) {
			String operator_recv= b.getString("operator_name");
			for (int i = 0;i<numberOfItems;i++){
				if(spinner.getItemAtPosition(i).equals(operator_recv)){
					spinner.setSelection(i);
					spinner.setClickable(false);
				}
			}

			String login_recv= b.getString("login");
			login.setText(login_recv);	

			String password_recv = b.getString("password");
			password.setText(password_recv);
		}  

		show_password.setChecked(false);
		password.setTransformationMethod(new PasswordTransformationMethod());
		show_password.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if(isChecked){
					password.setTransformationMethod(null);
				}
				else{
					password.setTransformationMethod(new PasswordTransformationMethod());
				}
			}

		});

	}

	@Override
	public void onResume(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onResume");
		super.onResume();
		serviceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(getClass().getSimpleName(), "?? " + "Bind connection");

				binder = (ServiceBinder) service;	
				if (binder.loadingError){
					finish();
				}else{
					setAccountManagement();

				}
			}
			public void onServiceDisconnected(ComponentName name){}
		};

		getApplicationContext().bindService(new Intent(this, ApplicationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onPause(){
		Log.d(getClass().getSimpleName(), "?? " + "Running onPause");
		super.onPause();
		if (serviceConnection != null){
			getApplicationContext().unbindService(serviceConnection);
			serviceConnection = null;
		}	

	}	

	/** Function called to load the screen, with the informations filled (the case of modification ) or without the informations filled (the case of add a account)*/
	public void setAccountManagement(){

		Button buttoncancel= (Button) findViewById(R.id.cancel_AM);
		buttoncancel.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				finish();
			}
		});


		Button buttonsave= (Button) findViewById(R.id.save_AM);
		buttonsave.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				final String operator_get = (String)spinner.getSelectedItem();
				final String login_get = login.getText().toString();
				final String password_get = password.getText().toString();
				operator_trans = operator_get;
				ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();

				ArrayList<CommunityNetworks> comNets = new ArrayList<CommunityNetworks>();
				for(CommunityNetworks cn:CommunityNetworks.values()){
					comNets.add(cn);
				}
				final CommunityNetworks cnSelected=communityService.getCNFromNameInApplication(operator_get, comNets);
				try {
					FileInputStream configFileIn = new FileInputStream (Environment.getExternalStorageDirectory() + ACCOUNT_FILE);
					final Properties props = new Properties();
					props.load(configFileIn);
					configFileIn.close();

					if(operator_trans.equals("") || login_get.equals("") || password_get.equals("") ){

						Toast.makeText(currentActivity, "You have to fill in all the fields", Toast.LENGTH_LONG).show();
					}
					else{
						AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
						if(b!=null){
							if(!props.getProperty(communityService.getUserParameter(cnSelected)).equals(login_get) || !props.getProperty(communityService.getPasswordParameter(cnSelected)).equals(password_get)){
								builder.setTitle("Modify an account");
								builder.setMessage("Are you sure to modify this account?");
							}
							else{finish();}
						}
						else{
							if(!props.getProperty(communityService.getUserParameter(cnSelected)).equals("")){
								builder.setTitle("Account exists");
								builder.setMessage("Account exists, are you sure to modify this account?");
							}
							else{
								builder.setTitle("Add an account");
								builder.setMessage("Are you sure to save this account?");
							}
						}

						builder.setPositiveButton("Yes", new OnClickListener(){

							@Override
							public void onClick(DialogInterface arg0, int arg1) {

								// modify or add the account in the account file
								ICommunityNetworkService communityService = ControllerServices.getInstance().getCommunity();
								props.setProperty(communityService.getUserParameter(cnSelected), login_get);
								String KEY_FILE_DIRECTORY = Environment.getExternalStorageDirectory()+ACCOUNT_KEYS_DIRECTORY;
								File file = new File(KEY_FILE_DIRECTORY);

								// modify or add the account in the parameter of ApplicationService
								ArrayList<CommunityNetworks> communityNets = (ArrayList<CommunityNetworks>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORKS);
								ArrayList<User> users = (ArrayList<User>) binder.parameters.getParameter(Parameter.COMMUNITY_NETWORK_USERS);
								User user;

								if (file.exists() == false){
									file.mkdir();
								}
								String KEY_FILE = KEY_FILE_DIRECTORY + "/" + operator_get+ ".key";

								String encryptedPwd = null;

								try {


									encryptedPwd = CryptoUtils.encrypt(password_get, new File(KEY_FILE));
									props.setProperty(communityService.getPasswordParameter(cnSelected), encryptedPwd);


									try {
										FileOutputStream configFileOut = new FileOutputStream (Environment.getExternalStorageDirectory() + ACCOUNT_FILE);
										props.store(configFileOut, null);
										configFileOut.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

									user = new User(login_get, password_get, cnSelected);

									ArrayList<CommunityNetworks> communityNetsCopy = (ArrayList<CommunityNetworks>) communityNets.clone();//to evade concurrent modification
									ArrayList<User> usersCopy = (ArrayList<User>) users.clone();//to evade concurrent modification
									for(CommunityNetworks n : communityNetsCopy){
										if(communityService.getName(n).equals(communityService.getName(cnSelected))){
											communityNets.remove(n);
											for(User u : usersCopy){
												if (u.getCommunityNetwork().equals(n)){
													users.remove(u);
												}

											}
										}
									}

									communityNets.add(cnSelected);
									users.add(user);

									binder.parameters.setParameter(Parameter.COMMUNITY_NETWORK_USERS, users);
									binder.parameters.setParameter(Parameter.COMMUNITY_NETWORKS, communityNets);

									finish();
								} catch (GeneralSecurityException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}	

						});


						builder.setNegativeButton("No", new OnClickListener(){

							@Override
							public void onClick(DialogInterface arg0, int arg1) {

							}
						});

						builder.show();

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			} 
		});


	}



}
