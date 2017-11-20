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

	private static final String publicKey = "b74e42e8c18e03c79e54a1095f2faf7949affcc76974faac804e2daad9ccc4b4123268868d55ffce20b686c0c4ffa9083c06ec78e6a93833b3dd12c2b748635b6b4a5031c6f75d0cbe027cf325c46041ad83b8ec6cbe75f7581905b6382966b1bbc7aa634b7c044173aaed4f6ba372ba1e7a205d1934b648dce3c345965c1c228b9258030f6089f80682e25428d9d9bd9c45887f91bdeba21db58dff4579cc0df2d51c25826ad8315c518a6397d8ccf9ad670ce6b7e90888a58069baf9d8ef3b9832f41503d4b7ca989935ea9e918af540b572bfd37dc74c2767b0303e0bb1530bc6af41c8f59a8453c59efa8280ba6a7224e321133488ab836071db19e354c3";

	//private static final String publicKey = "a6110b39014d2f768f6bc61cc5e3ad480c576d7759a76b97a81a9d0cb1cad0d9decc22cb6fbcbb0fe84d7213e8204de0e53c706481844e998f9e4c44d349379ee9c04f72de6e3a3c44772eedb313cb9a4a2e8f5c6d74e2ba41ea325a153acd893ea3e3120e700a131f273ad331d4225990325631758623cd4f0e62ecd17212112ed8ba0cabd345dedc3bcb08d190ca6f3a3eb5892eb45b9dd74ef7dfcc98dcd90cf4408d66e5e5134c176bc408ab88ea74dd3d5e0973af9160b7674d0d616441521169374e7a067b70e2192587a55ce70717172ebe1cdf155ae29941d2fed9980b76c6f10a9135c509bc480e755713440279a00f0021230689bbd9571e34ece7";

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
			String wi2meServer = "http://216.34.181.45";
			String wi2meServerPage = "News for nerds, stuff that matters";

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
						communityNetworkConnectionHttpClient.getParams().setParameter(ConnRoutePNames.LOCAL_ADDRESS, Utils.intToInetAddress(wifiAddress));

						HttpGet initialGet = new HttpGet(wi2meServer);
				    	HttpResponse response = communityNetworkConnectionHttpClient.execute(initialGet);

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


						TrustAllSSLSocketFactory tasslf = new TrustAllSSLSocketFactory(publicKey, commonName);
				        Scheme sch = new Scheme(SCHEME_NAME, tasslf, 443);
				        communityNetworkConnectionHttpClient.getConnectionManager().getSchemeRegistry().register(sch);

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
