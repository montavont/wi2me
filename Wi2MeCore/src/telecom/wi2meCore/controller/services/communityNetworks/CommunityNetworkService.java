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

import android.os.Handler;
import android.util.Log;

import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;

import android.content.Context;

/**
 * This class manages all the community networks. It has been created to allow future developers to easily add community networks.
 * With this class, there are no references in the code to the community networks, like if (name == SALSA).
 * Everything which has to do with the community networks are now managed in this class.
 * This class has been implemented as a singleton which can be accessed statically because it has to be created before the application initializes the other services.
 * To create a new community network, simply follow those 3 steps:
 * 1. Create a class implementing this interface, copy/paste the code from another community account and change ONLY the private parameters.
 * 2. Add the community network to the list of CommunityNetworks (telecom.wi2meCore.controller.configuration)
 * 3. Add the community network in the CommunityNetworkService (telecom.wi2meCore.controller.services.communityNetworks) simply by
 * adding it in the list of parameters and in the switch of the method getCommunityNetwork ONLY.
 * @author Gilles Vidal
 *
 */
public class CommunityNetworkService implements ICommunityNetworkService{

	ICommunityNetwork SALSA;
	ICommunityNetwork Free;
	ICommunityNetwork Neuf;
	ICommunityNetwork Fon;

	private static final String CONNECTING_TIMEOUT_MESSAGE = "The timeout for connecting to Community Network elapsed";
	private static final String START = "START";
	public static final String END = "END";
	public static final String FAILED = "FAILED";
	public static final String CONNECTED = "CONNECTED";
	
	private ConnectingThread connectingThread;
	private ICNConnectionEventReceiver cnConnectionReceiver;

	private Context context;

	public CommunityNetworkService(Context context){
		SALSA = new SALSA();
		Free = new Free();
		Neuf = new Neuf();
		Fon = new Fon();
		this.context = context;
	}

	/**
	 * This class performs the connections and then waits to be interrupted when connection events are done
	 */
	private class ConnectingThread extends Thread{

		public boolean timeoutElapsed = false;
		public boolean connectingSuccessful = false;
		
		public void run(){
				
			try {
				Thread.sleep(TimeoutConstants.COMMUNITY_NETWORK_CONNECTION_TIMEOUT);
				//If the timeout elapses, connecting was unsuccessful
				timeoutElapsed = true;
				connectingSuccessful = false;
			} catch (InterruptedException e) {
				// If it is interrupted, the timeout did not elapse, but the CommunityNetworkService decides if the connection was successful or not 
				timeoutElapsed = false;
			}
		}
	}

	/**
	 * The core of the management of community networks. "Transforms" a CommunityNetworks into the corresponding class.
	 * Only this method needs to be modified when adding a new community network.
	 * @param cn The community network
	 * @return The class corresponding to the given community network
	 */
	private ICommunityNetwork getCommunityNetwork(CommunityNetworks cn){
		switch (cn) {
		case SALSA:
			return SALSA;
		case FREE:
			return Free;
		case NEUF:
			return Neuf;
		case FON:
			return Fon;
		default:
			return null;
		}		
	}

	/**
	 * Gives the name of the corresponding community network
	 * @param cn The community network
	 * @return the name
	 */
	@Override
	public String getName(CommunityNetworks cn){
		return getCommunityNetwork(cn).getName();
	}
	
	/**
	 * Gives the regular expression used to compare the APs SSID to check if it is a community network.
	 * @param cn The community network
	 * @return The regular expression
	 */
	@Override
	public String getRegExp(CommunityNetworks cn){
		return getCommunityNetwork(cn).getRegExp();
	}
	
	/**
	 * Gives the authentication routine
	 * @param cn
	 * @return The Runnable authentication routine
	 */
	@Override
	public Runnable getAuthenticationRoutine(CommunityNetworks cn, String username, String password){
		return getCommunityNetwork(cn).getAuthenticationRoutine(username, password);
	}
	
	/**
	 * Gives the name used inside the application between the classes.
	 * This name is also the name shown to the user.
	 * @param cn The community network
	 * @return The name used between the classes for this community network.
	 */
	@Override
	public String getNameInApplication(CommunityNetworks cn){
		return getCommunityNetwork(cn).getNameInApplication();
	}
	
	/**
	 * Gives the parameter's name for the user login for this community network.
	 * For example, SALSA_USER.
	 * @param cn The community network
	 * @return The user parameter
	 */
	@Override
	public String getUserParameter(CommunityNetworks cn){
		return getCommunityNetwork(cn).getUserParameter();
	}
	
	/**
	 * Gives the parameter's name for the user's password for this community network.
	 * For example, SALSA_PASSWORD.
	 * @param cn The community network
	 * @return The password parameter
	 */
	@Override
	public String getPasswordParameter(CommunityNetworks cn){
		return getCommunityNetwork(cn).getPasswordParameter();
	}
	
	/**
	 * Gives the path to the key used to encrypt the password of the account of this community network.
	 * @param cn The community network
	 * @return The path to the file containing the key
	 */
	@Override
	public String getEncryptionKeyLocation(CommunityNetworks cn){
		return getCommunityNetwork(cn).getEncryptionKeyLocation();
	}
	
	/**
	 * This method allows us to tell whether an AP is a community network or not
	 * @param ssid : The SSID of the AP
	 * @param comNets : The list of community networks
	 * @return True if it is a community network, false otherwise.
	 */
	@Override
	public boolean isCommunityNetwork(String ssid, List<CommunityNetworks> comNets){
		for (CommunityNetworks cn : comNets){
			if (ssid.matches(getRegExp(cn))){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method allows to get the community network corresponding to an SSID.
	 * @param ssid
	 * @param comNets The list of community networks.
	 * @return The community network corresponding to the SSID (null if it is not a community network).
	 */
	@Override
	public CommunityNetworks whichCommunityNetwork(String ssid, List<CommunityNetworks> comNets){
		for (CommunityNetworks cn : comNets){
			if (ssid.matches(getRegExp(cn))){
				return cn;
			}
		}
		return null;
	}
	
	/**
	 * This method allows to get the community network corresponding to the given name used in the application.
	 * @param nameInApplication
	 * @param comNets
	 * @return The corresponding community network.
	 */
	@Override
	public CommunityNetworks getCNFromNameInApplication(String nameInApplication, List<CommunityNetworks> comNets){
		for(CommunityNetworks cn:comNets){
			if(getNameInApplication(cn).equals(nameInApplication)){
				return cn;
			}
		}
		return null;
	}
	

	@Override
	public boolean connectToCommunityNetwork(ICNConnectionEventReceiver rec, String username, String password, Runnable routine) throws TimeoutException, InterruptedException
	{
		boolean retval = false;
	    	
				        
		if (connectingThread != null){
			if (connectingThread.isAlive())
				//If it is still running, we need to wait it to finish
				retval = false;			
		}
		else
		{
			this.cnConnectionReceiver = rec;
			
			connectingThread = new ConnectingThread();
			connectingThread.start();		
			try {
				rec.receiveConnectionEvent(START);
				
				Thread authenticatingThread = new Thread(routine);
				authenticatingThread.start();

				connectingThread.join();
				if (connectingThread.timeoutElapsed)
					throw new TimeoutException(CONNECTING_TIMEOUT_MESSAGE);
				//if timeout did not elapse, we return the connection status
				retval = connectingThread.connectingSuccessful;
			} catch (InterruptedException e) {
				//If somebody interrupts us before finishing, throw the exception
				throw e;
			}finally{
				connectingThread = null;
			}
		}
		return retval;
	}

	@Override
	public void stopCommunityNetworkConnection() 
	{
	    	Log.d(getClass().getSimpleName(),"++ "+"Hotspot connection timeout !");
		if (connectingThread != null)
		{
			connectingThread.interrupt();
		}
	}

	
	private void announceConnectionFinished(boolean successful)
	{
		if (connectingThread != null)
		{
			connectingThread.connectingSuccessful = successful;
			connectingThread.interrupt();
		}
	}

	public void signalCNConnectionEnd(boolean finished, String Event)
	{
	    	cnConnectionReceiver.receiveConnectionEvent(Event);	    	
	    	announceConnectionFinished(finished);
	}



	@Override
	public void CNConnectionEnd()
	{ 	
		Log.d(getClass().getSimpleName(),"++ "+"Hotspot authentication successful!");
	    	cnConnectionReceiver.receiveConnectionEvent(END);	    	
	    	announceConnectionFinished(true);
	}
	    
	/**
	 * Call this function when the authentication fail
	 */
	@Override
	public void CNConnectionFailed()
	{ 
	    	Log.e(getClass().getSimpleName(),"++ "+"Hotspot authentication failed!");
	    	cnConnectionReceiver.receiveConnectionEvent(FAILED);
	    	announceConnectionFinished(false);
	}
	    
	@Override
        public void CNConnectionFailed(String errorMsg)
	{ 
		Log.e(getClass().getSimpleName(),"++ "+"Hotspot authentication failed!");
        	Log.e(getClass().getSimpleName(),"++ "+errorMsg);
            	cnConnectionReceiver.receiveConnectionEvent(FAILED+"-ERROR:"+errorMsg);
        	announceConnectionFinished(false);
        }
        

	/**
	 * Call this function when the authentication is already active
	 */
	@Override
	public void CNConnectionAlreadyConnected()
	{ 
		Log.d(getClass().getSimpleName(),"++ "+"Already authenticated to hotspot.");
	    	cnConnectionReceiver.receiveConnectionEvent(CONNECTED);
	    	announceConnectionFinished(true);
	}

}
