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

package telecom.wi2meCore.controller.services.wifi;

import java.util.HashMap;

import android.util.Log;

/**This class manages the failures of connection.
 * Goal: after 3 unsuccessful pings, we don't connect to the concerned AP anymore.
 * List of number of failures managed by a HashMap<SSID,Number of failures>.
 * @author Gilles Vidal
 *
 */
public class APStatusService {
	
	private static APStatusService instance = null;
	private HashMap<String,Integer> failureNumbers;
	
	/**Basic constructor, initialize the list of failures. 
	 */
	private APStatusService(){
		failureNumbers = new HashMap<String, Integer>();
	}
	
	/**Returns the instance of APStatusService 
	 * @return APStatusService
	 */
	public static APStatusService getInstance(){
		if (instance == null){
			instance = new APStatusService();
		}
		return instance;
	}
	
	/**Erases all the values. 
	 */
	public void reset(){
		failureNumbers.clear();
	}
	
	/**Gives the number of failures for a given SSID. 
	 * @param SSID
	 * @return Number of failures for this SSID (0 if none).
	 */
	public Integer getFailureNumber(String SSID){
		if(failureNumbers.containsKey(SSID)){
			return failureNumbers.get(SSID);
		}else {return 0;}
	}
	
	/**Increases the number of failings for the given AP. 
	 * @param SSID
	 */
	public void addFailing(String SSID){
		if(failureNumbers.containsKey(SSID)){
			int failureNumber=failureNumbers.get(SSID)+1;
			failureNumbers.put(SSID, failureNumber);
		}else{
			failureNumbers.put(SSID, 1);
		}
		Log.w(getClass().getSimpleName(),"++ "+failureNumbers.toString());
	}

}
