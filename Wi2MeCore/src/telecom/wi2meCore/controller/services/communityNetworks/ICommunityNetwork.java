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

/**
 * This interface has to be implemented to create a new community network.
 * To create a new community network, simply follow those 3 steps:
 * 1. Create a class implementing this interface, copy/paste the code from another community account and change ONLY the private parameters.
 * 2. Add the community network to the list of CommunityNetworks (telecom.wi2meCore.controller.configuration)
 * 3. Add the community network in the CommunityNetworkService (telecom.wi2meCore.controller.services.communityNetworks) simply by
 * adding it in the list of parameters and in the switch of the method getCommunityNetwork ONLY.
 * @author Gilles Vidal
 *
 */
public interface ICommunityNetwork {
	
	//Variables used for the columns names in the database.
    public static final String TABLE_NAME = "CommunityNetwork";
    public static final String NAME = "name";
    public static final String MATCH_SSID_REG_EX = "regEx";
    public static final String JS_CONNECTION_PLUGIN = "jsPlugin";
	
	/**
	 * Gives the name of the corresponding community network
	 * @return the name
	 */
	String getName();
	
	/**
	 * Gives the regular expression used to compare the APs SSID to check if it is a community network.
	 * @return The regular expression
	 */
	String getRegExp();
	
	/**
	 * Gives the name used inside the application between the classes.
	 * This name is also the name shown to the user.
	 * @return The name used between the classes for this community network.
	 */
	String getNameInApplication();
	
	/**
	 * Gives the parameter's name for the user login for this community network.
	 * For example, SALSA_USER.
	 * @return The user parameter
	 */
	String getUserParameter();
	
	/**
	 * Gives the parameter's name for the user's password for this community network.
	 * For example, SALSA_PASSWORD.
	 * @return The password parameter
	 */
	String getPasswordParameter();
	
	/**
	 * Gives the path to the key used to encrypt the password of the account of this community network.
	 * @return The path to the file containing the key
	 */
	String getEncryptionKeyLocation();

	/**
	 * Returns a Runnable to be executed by the main looper in order to fille the community network's authentication page. 
	 * @param UserName 
	 * @param Password 
	 * @return The path to the file containing the key
	 */
	Runnable getAuthenticationRoutine(String UserName, String Password);

}
