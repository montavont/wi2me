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
import telecom.wi2meCore.controller.services.ControllerServices;
import android.util.Log;
 
/**
 * This class contains all the information concerning the community network Neuf/SFR WiFi.
 * @author Gilles Vidal
 *
 */
public class Fon implements ICommunityNetwork{

	private String name = "FON";
	private String regExp = "FON_FREE_INTERNET";
	private String pluginFilename = "plugins/fon.js";
	private String loadedPlugin = "";
	private String nameInApplication = "FON";
	private String userParameter = "FON_USER";
	private String passwordParameter = "FON_PASSWORD";
	private String encryptionKeyLocation = "FON.key";

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
		return new Runnable()
		{
			@Override
			public void run()
			{ 
				Log.e("FON", "++ " + "NOT IMPLEMENTED -");
				ControllerServices.getInstance().getCommunity().CNConnectionFailed();
			}
		};
	}
}
