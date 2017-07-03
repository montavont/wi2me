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

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.web.TrustAllSSLSocketFactory;


/**
 * This class contains all the information concerning the community network Neuf/SFR WiFi.
 * @author Gilles Vidal
 *
 */
public class Neuf implements ICommunityNetwork{

	private String name = "NEUF WIFI";
	private String regExp = "^(Neuf|SFR) WiFi( Public| FON)?$";
	private String pluginFilename = "plugins/neufwifi.js";
	private String loadedPlugin = "";
	private String nameInApplication = "Neuf WiFi";
	private String userParameter = "NEUF_USER";
	private String passwordParameter = "NEUF_PASSWORD";
	private String encryptionKeyLocation = "NEUF WIFI.key";

	private static final String publicKey = "a39fabd5008d0029d8e2923ec476270f9d54e8b18364f96c1fc50be2f9a692eabf6dbb269d1acb44107f18ccbf7cc57015a917e13c8a795bbf0e73bcb400fc8dac7d9727981299249cff1d2170678054279662580cdd280279f53b60ea299864ae8dd804a81c9c7ad03afaa2d8f81871aa06f974a31807e59e71737ccad4ea03b20fe6ec701a4699aa5ae5e8bebd8e7ae05e6c8a80765d3ac3b2086673aedfab69f596b70bf05d151cbfb777e2e692a0b5e6daf02eca5acb19ebf67e154c850a68429b5bb3e28e8566cec788aae57927dadd8f35fb84d7a8567a48bdc273c38ba9c4f685f0deda247ec51d0b3372442baf66ad460fdfc13285d790174c7755af";
	private static final String commonName = "*.wifi.sfr.fr";
	private static String SCHEME_NAME = "https";


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

	
	private String extractAuthValue(String data, String start, String end)
	{
		return data.substring(data.indexOf(start) + start.length(), data.indexOf(end));
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

			String nb4Url = "https://hotspot.wifi.sfr.fr/nb4_crypt.php";
			String portalUrl = "";

			String connectionParameters = "";
			String connectionParametersKey = "!--SFRLoginURL_JIL";
			String authResponseKey = "&amp;response=";
			String successfullLoginPage = "Authentication Success";
	
			String baseUserName = "ssowifi.neuf.fr/";

			String str = "";
			String page = "";
			DefaultHttpClient communityNetworkConnectionHttpClient;

			String challenge = "";
			String mode = "";
			String uamip = "";
			String userUrl = "";
			String uamport = "";

			String authResponse = "";

			@Override
			public void run()
			{ 

				try 
				{


				    	communityNetworkConnectionHttpClient = new DefaultHttpClient();
		

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
						if (str.contains(connectionParametersKey))
							connectionParameters = str;

					}
			
					if (page.contains(wi2meServerPage))
					{
						ControllerServices.getInstance().getCommunity().CNConnectionAlreadyConnected();
					}
					else
					{

						//Extract challenge
						challenge = extractAuthValue(connectionParameters, "&challenge=", "&userurl=");

						//Extract mode
						mode = extractAuthValue(connectionParameters, "&mode=", "&channel=");
							
						//Extract redirect URL
						userUrl = extractAuthValue(connectionParameters, "&userurl=", "&nasid=");

						//Extract portal IP
						uamip = extractAuthValue(connectionParameters, "&uamip=", "&uamport=");
						
						//Extract portal port
						uamport = extractAuthValue(connectionParameters, "&uamport=", "&challenge=");
						
						if (challenge.length() * mode.length() * uamip.length() * userUrl.length() * uamport.length() == 0)
						{
						        Log.e("Neuf Authentication", "Missing parameter on login page : " + challenge + " " + mode + " " + uamip + " " + userUrl + " " + uamport + " ");
						        Log.e("Neuf Authentication", page);
							ControllerServices.getInstance().getCommunity().CNConnectionFailed();
						}
						else
						{
						
						        HttpPost authPost = new HttpPost(nb4Url);
							page = ""; 
							List<NameValuePair> params = new ArrayList<NameValuePair>(2);
							params.add(new BasicNameValuePair("username", username));
							params.add(new BasicNameValuePair("password", password));
							params.add(new BasicNameValuePair("cond", "on"));
							params.add(new BasicNameValuePair("accessType", "neuf"));
							params.add(new BasicNameValuePair("nb4", nb4Url));
							params.add(new BasicNameValuePair("challenge", challenge));
					
							authPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
							
						    	response = communityNetworkConnectionHttpClient.execute(authPost);
					
							in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
							while ((str = in.readLine()) != null)
							{
								page += str;
								if (str.contains(authResponseKey))
								{
									authResponse = extractAuthValue(str, "&amp;response=", "&amp;uamip=");
									break;
								}
							}
						
							if (authResponse.length() == 0)
							{
						        	Log.e("Neuf Authentication", "Failure to authenticate : ");
						        	Log.e("Neuf Authentication", page);
							}
							else
							{
						        
								portalUrl = "http://" + uamip + ":" + uamport + "/logon";
								portalUrl += "?username=" + baseUserName + username;
								portalUrl += "&response=" + authResponse;
								portalUrl += "&uamip=" + uamip;
								portalUrl += "&userurl=" + userUrl;
								portalUrl += "&lang=" + "fr";
								portalUrl += "&ARCHI";

								HttpGet finalRequest = new HttpGet(portalUrl);
								page = ""; 

								Log.d("HTTPGet", "++ " + "To execute " + portalUrl + "-");
							    	response = communityNetworkConnectionHttpClient.execute(finalRequest);
							    	Log.d("HTTPGet", "++ " + "Executed " + portalUrl + "-");
						
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
						        		Log.e("Neuf Authentication", "Failure to authenticate : ");
							        	Log.e("Neuf Authentication", page);
								}
							}

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
