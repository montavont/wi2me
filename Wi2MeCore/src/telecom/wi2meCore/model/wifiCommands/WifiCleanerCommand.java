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

package telecom.wi2meCore.model.wifiCommands;

import java.util.HashMap;
import android.util.Log;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.StatusService;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.model.CleanerCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.entities.ExternalEvent;



/**
 * This class cleans the wifi features to start again (probably with a new scan process). Closes connections by forgetting networks, and resynchronizes the cell thread if necessary.
 * Also works as a LocationFixChecker if this feature is requested.
 * @author Alejandro
 *
 */
public class WifiCleanerCommand extends CleanerCommand{
	private static final String WIFI_CONNECTION_FINISHED_EVENT = "WIFI_DISCONNECTED";

	public WifiCleanerCommand() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public WifiCleanerCommand(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
	}

	@Override
	public void clean(IParameterManager parameters){
		//To change the status of the application in Wi2MeUser
		StatusService.getInstance().changeStatus("Preparing to scan...");

		//On first loop, we deactivate the Wifi to get total control.
		if((Boolean) parameters.getParameter(Parameter.IS_FIRST_LOOP)){
			try {
				ControllerServices.getInstance().getWifi().disable();
			} catch (TimeoutException e) {
				Log.e(getClass().getSimpleName(),"++ "+e.toString());
				throw new RuntimeException("FATAL ERROR: Wifi cannot be disabled in cleaner, unable to continue");
			}
			parameters.setParameter(Parameter.IS_FIRST_LOOP, false);
		}

		/*---CLEAN EVERYTHING TO START AGAIN---*/
		ControllerServices.getInstance().getWifi().cleanNetworks(); //ensures disconnection if connected
		/* *******************************************************************************************************************************************
		 * We check if a connection was manually asked by the user. 
		 * If it was just asked, we change the SSID from _OLD** to _OLD***, 
		 * if it was already connected before, we set the grade back to the original state.
		 */
		HashMap<String,Integer> apGradeMap = (HashMap<String,Integer>) parameters.getParameter(Parameter.AP_GRADE_MAP);
		HashMap<String,Integer> apGradeMapCopy = (HashMap<String, Integer>) apGradeMap.clone();// To evade concurrent modification
		HashMap<String,Integer> apGradeMapCopy2 = (HashMap<String, Integer>) apGradeMap.clone();// To evade concurrent modification
		for(String SSID:apGradeMapCopy.keySet()){
			/* If the user forced a connection and connection was already established
			 * We change the apGradeMap to set the grade back to its initial state.
			 */
			if(SSID.length()>7){
				if(SSID.substring(SSID.length()-7, SSID.length()).equals("_OLD***")){
					boolean setGradeBack = true;
					//We give the inital grade to this AP, unless the user asked again to manually connect to it.
					for(String SSID2:apGradeMapCopy2.keySet()){
						if(SSID2.equals(SSID.substring(0, SSID.length()-1))){
							setGradeBack = false;
							break;
						}
					}
					if(apGradeMap.get(SSID.substring(0, SSID.length()-7))==null){
						//The grade has been removed. We don't put the previous saved value.
						setGradeBack=false;
					}else if(apGradeMap.get(SSID.substring(0, SSID.length()-7))!=10){
						//If the grade of the AP is not 10, it means it has been changed after the manual connection. We don't put the previous saved value.
						setGradeBack=false;
					}
					if(setGradeBack==true){
						apGradeMap.put(SSID.substring(0, SSID.length()-7), apGradeMap.get(SSID));
					}
					apGradeMap.remove(SSID);
				}
			}
			/* If the user forced a connection and connection was not already established
			 * We change the SSID from _OLD** to _OLD*** to tell the application next time it goes to the cleaner that we already tried to connect.
			 */
			if(SSID.length()>6)
			{
				if(SSID.substring(SSID.length()-6, SSID.length()).equals("_OLD**"))
				{
					apGradeMap.put(SSID+"*", apGradeMap.get(SSID));
					apGradeMap.remove(SSID);
				}
			}
		}
		parameters.setParameter(Parameter.AP_GRADE_MAP, apGradeMap);

		parameters.setParameter(Parameter.WIFI_CONNECTION_ATTEMPT, false); //wifi connection attempt is over
		Logger.getInstance().log(ExternalEvent.getNewExternalEvent(TraceManager.getTrace(), WIFI_CONNECTION_FINISHED_EVENT));
	}
}
