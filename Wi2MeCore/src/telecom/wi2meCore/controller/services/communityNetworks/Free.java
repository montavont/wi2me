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
 * This class contains all the information concerning the community network Free Wifi.
 * @author Gilles Vidal
 *
 */
public class Free implements ICommunityNetwork {

	private String name = "FreeWifi";
	private String regExp = "FreeWifi";
	private String nameInApplication = "FreeWifi";
	private String userParameter = "FREEWIFI_USER";
	private String passwordParameter = "FREEWIFI_PASSWORD";
	private String encryptionKeyLocation = "FREEWIFI.key";

	//private static final String publicKey = "c5ca4db0fdca261a23d39a36805bef5ac3ed7958cc3518480cf4e35f76b5d84dc084f296aae9c118f57cdda9ae6271f1bb2bef62b883371f115d44eb6c692112f05d6cd86c5fd051dbdfcdffac5ad84a9f00e0f2f37c92f3a0be1fa766cb0cf58d87043dce89fa670fdd0b90a237535c47698e6747afe5856da65dad3c338bc835db6244a631d4bc4e53795f96d4212b5a781d622936a36598b478065fb0a854dcf12204a658d82981fd04a92b2d04c1c8a694e976526eee6b2ba46ed777f770ca9bcb32ce18e686e8cf5e758ead8a55ce589091c5f03691c02af4efb11f1df14597f124c13d87905ff8ab46ade1c99eaf14be787c067365ff3eb74cf5b0f3bf";
	private static final String publicKey = "bd236ff992fbf8c11a1ffda3a2992b4940d771d4143ca56f1257976a17ddd126a1a7329a94a828f4e7bde48b6d6766dcbf162c38169214c6e2a410348c077e39a9ba0eef470803aaabd9489303f6c957ec062d84aa97f4a4b1ef47c79b663b21fd8ef447ee418057f0914c008a85bab06dcd94f89f591eefff95f4acbcf716eb063da9529c2de169d1e828170578c3ad4f5e88084d268dca5f5d07645ece2fe3a4db9412209c79727176052cb0f801138c2a0f1d7f579a76aa666d6c507dd985a39d3efffa9ea97bff189ad0010cbd299762fee02ab011ce7af6cf2236245607a6df5cd22fb5eff3422b18bf4283326f902be1b25480485edc83167e87653387";
	private static final String commonName = "*.free.fr";

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
			String freeWifiServer =  "https://wifi.free.fr/Auth";
			String wi2meServer = "https://slashdot.org";

			String wi2meServerPage = "News for nerds, stuff that matters";

			String successfullLoginPage = "CONNEXION AU SERVICE REUSSIE";
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

						page = "";
						HttpPost authPost = new HttpPost(freeWifiServer);
						List<NameValuePair> params = new ArrayList<NameValuePair>(2);
						params.add(new BasicNameValuePair("login", username));
						params.add(new BasicNameValuePair("password", password));
						params.add(new BasicNameValuePair("submit", "Valider"));

						authPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

						response = communityNetworkConnectionHttpClient.execute(authPost);

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
							Log.e(getClass().getSimpleName(), "++ FreeWifi Connection Failed " + page);
						}
					}
				}
				catch (Exception e)
				{
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);

					communityNetworkConnectionHttpClient.getConnectionManager().shutdown();

					ControllerServices.getInstance().getCommunity().CNConnectionFailed();
				}
			}
		};
		return routine;

	}
}
