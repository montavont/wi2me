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

package telecom.wi2meCore.controller.services.communityNetworks;

import android.net.wifi.WifiInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress; 
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.web.TrustAllSSLSocketFactory;
import telecom.wi2meCore.model.Utils;


/**
 * This class contains all the information concerning the community network SALSA.
 * @author Gilles Vidal
 *
 */
public class SALSA implements ICommunityNetwork {
	
	private String name = "SALSA";
	private String regExp = "SALSA";
	private String pluginFilename = "plugins/salsa.js";
	private String loadedPlugin = "";
	private String nameInApplication = "SALSA";
	private String userParameter = "SALSA_USER";
	private String passwordParameter = "SALSA_PASSWORD";
	private String encryptionKeyLocation = "SALSA.key";
 
	private static final String publicKey = "ae2fb982abcfd574ba571a26333f10bf4cbcc6d7f423b8eb1579907268332236e07a6698e00d54a88db7e1ae0ced3a8dc427f070071998acfa65582c7b3ec90113a5625da9ec400d7e70255e62c0cd8d285fe6f636f70cfd41b6c653be4c8b429ec8cad726fdd458abefded2971a08d0eb1b10fb5c3848b5d31d7e0574ce3a152f05b684d8bfa3357c32ce35d882d89e1e950c31ca3b56e74f46e3b70b9630cc4bec8d05f7d129c5ad882f13acd62a897a44c2c3c6c5f0ccf867810e6ca67ab15465622b2f8ea696f423263a2f4a076631e2c169b9b16c8ef5bf2bd062fe7a2f7c8440bb6452e9b1621f13c5664c6f3bb4343f0b2207e8a2c3b4c17c0e38d513";
	private static final String commonName = "*.telecom-bretagne.eu";

	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getRegExp() {
		return regExp;
	}
	
	@Override
	public String getNameInApplication() {
		return nameInApplication;
	}
	@Override
	public String getUserParameter() {
		return userParameter;
	}
	@Override
	public String getPasswordParameter() {
		return passwordParameter;
	}
	@Override
	public String getEncryptionKeyLocation() {
		return encryptionKeyLocation;
	}
	
	@Override
	public Runnable getAuthenticationRoutine(String UserName, String Password)
	{

		final String username = UserName;
		final String password = Password;

		Runnable routine = new Runnable()
		{
			String wi2meServer = ConfigurationManager.CONNECTION_CHECK_URL;

			String wi2meServerPage = "Telecom Bretagne Server for Wi2MeTraceXPlorer";

			String salsaUrl = "https://webauth.telecom-bretagne.eu/login.html";
			String successfullLoginPage = "Login Successful";
	
			String SCHEME_NAME = "https";

			String str = "";
			String page = "";
			DefaultHttpClient communityNetworkConnectionHttpClient;

			@Override
			public void run()
			{ 

				try 
				{
				    	communityNetworkConnectionHttpClient = new DefaultHttpClient();

					WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
					int wifiAddress = info.getIpAddress();
					Log.d("HTTPGet", "++ " + "Local IP Address " + Utils.intToIp(wifiAddress));
	
					communityNetworkConnectionHttpClient.getParams().setParameter(ConnRoutePNames.LOCAL_ADDRESS, Utils.intToInetAddress(wifiAddress));

					TrustAllSSLSocketFactory tasslf = new TrustAllSSLSocketFactory(publicKey, commonName);
				        Scheme sch = new Scheme(SCHEME_NAME, tasslf, 443);
				        communityNetworkConnectionHttpClient.getConnectionManager().getSchemeRegistry().register(sch);
				    	
					HttpGet initialGet = new HttpGet(wi2meServer);
			
					Log.d("HTTPGet", "++ " + "To execute " + wi2meServer + "-");
				    	HttpResponse response = communityNetworkConnectionHttpClient.execute(initialGet);
				    	Log.d("HTTPGet", "++ " + "Executed " + wi2meServer + "-");
			
					BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
					while ((str = in.readLine()) != null)
					{
						page += str;
					}
			
					if (page.contains(wi2meServerPage))
					{
						ControllerServices.getInstance().getCommunity().CNConnectionAlreadyConnected();
					}
					else
					{

				
					        HttpPost authPost = new HttpPost(salsaUrl);
						page = ""; 
						List<NameValuePair> params = new ArrayList<NameValuePair>(2);
						params.add(new BasicNameValuePair("username", username));
						params.add(new BasicNameValuePair("password", password));
						params.add(new BasicNameValuePair("buttonClicked", "4"));
				
						authPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
						
						Log.d("HTTPGet", "++ " + "Now executing post " + salsaUrl + "-" );
					    	response = communityNetworkConnectionHttpClient.execute(authPost);
					    	Log.d("HTTPGet", "++ " + "Executed post  " + salsaUrl + "-" );
				
						in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
						while ((str = in.readLine()) != null)
						{
							page += str;
						}
	
						if (page.contains(successfullLoginPage))
						{
							ControllerServices.getInstance().getCommunity().CNConnectionEnd();
						}
						else
						{
							ControllerServices.getInstance().getCommunity().CNConnectionFailed();
						}


					}

				}
				catch (Exception e)
				{
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
					ControllerServices.getInstance().getCommunity().CNConnectionFailed();
				}
			}
		};
		return routine;
		
	}
}
