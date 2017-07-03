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

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import java.util.List;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;



public interface ICommunityNetworkService
{

	/**
	 * Gives the name of the corresponding community network
	 * @param cn The community network
	 * @return the name
	 */
	public String getName(CommunityNetworks cn);

	/**
	 * Gives the regular expression used to compare the APs SSID to check if it is a community network.
	 * @param cn The community network
	 * @return The regular expression
	 */
	public String getRegExp(CommunityNetworks cn);


	/**
	 * Gives the name used inside the application between the classes.
	 * This name is also the name shown to the user.
	 * @param cn The community network
	 * @return The name used between the classes for this community network.
	 */
	public String getNameInApplication(CommunityNetworks cn);
	
	/**
	 * Gives the parameter's name for the user login for this community network.
	 * For example, SALSA_USER.
	 * @param cn The community network
	 * @return The user parameter
	 */
	public String getUserParameter(CommunityNetworks cn);

	/**
	 * Gives the parameter's name for the user's password for this community network.
	 * For example, SALSA_PASSWORD.
	 * @param cn The community network
	 * @return The password parameter
	 */
	public String getPasswordParameter(CommunityNetworks cn);



	/**
	 * Gives the path to the key used to encrypt the password of the account of this community network.
	 * @param cn The community network
	 * @return The path to the file containing the key
	 */
	public String getEncryptionKeyLocation(CommunityNetworks cn);



	/**
	 * This method allows us to tell whether an AP is a community network or not
	 * @param ssid : The SSID of the AP
	 * @param comNets : The list of community networks
	 * @return True if it is a community network, false otherwise.
	 */
	public boolean isCommunityNetwork(String ssid, List<CommunityNetworks> comNets);

	/**
	 * This method allows to get the community network corresponding to an SSID.
	 * @param ssid
	 * @param comNets The list of community networks.
	 * @return The community network corresponding to the SSID (null if it is not a community network).
	 */
	public CommunityNetworks whichCommunityNetwork(String ssid, List<CommunityNetworks> comNets);

	/**
	 * This method allows to get the community network corresponding to the given name used in the application.
	 * @param nameInApplication
	 * @param comNets
	 * @return The corresponding community network.
	 */
	public CommunityNetworks getCNFromNameInApplication(String nameInApplication, List<CommunityNetworks> comNets);


	/**
	 * 	Community network end of connection attempt function
	 * 	Failure to connect
 	 */
	public void CNConnectionFailed();

	/**
	 * 	Community network end of connection attempt function
	 * 	Failure to connect with error message
 	 */
        public void CNConnectionFailed(String errorMsg);

	/**
	 * 	Community network end of connection attempt function
	 * 	Successfull end of connection attempt
 	 */
	public void CNConnectionEnd();

	/**
	 * 	Community network end of connection attempt function
	 * 	Attempt aborted since already authentified to CN
 	 */
	public void CNConnectionAlreadyConnected();

	/**
	 *	Trigger connection to a community network
	 */
	public boolean connectToCommunityNetwork(ICNConnectionEventReceiver rec, String username, String password, Runnable routine)  throws TimeoutException, InterruptedException;

	/**
	 * 	Cancel an ongoing network connection
	 */
	public void stopCommunityNetworkConnection();


	/**
	 * Gives the authentication routine
	 * @param cn
	 * @return The Runnable authentication routine
	 */
	public Runnable getAuthenticationRoutine(CommunityNetworks cn, String username, String password);

}
