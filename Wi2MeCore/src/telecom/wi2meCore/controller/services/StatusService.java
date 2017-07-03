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

package telecom.wi2meCore.controller.services;

import java.util.Observable;

/**This class is used to update dynamically the status of the application on Wi2MeUser interface
 * @author Gilles Vidal
 */
public class StatusService extends Observable {
	
	private static StatusService instance = null;
	private String status;
	
	/**Basic constructor. Sets the status to "Ready". 
	 */
	private StatusService(){
		status="Ready";
	}
	
	/**Returns the unique instance of StatusService
	 * 
	 * @return StatusService
	 */
	public static StatusService getInstance(){
		if (instance == null){
			instance = new StatusService();
		}
		return instance;
	}
	
	/**Sets the status and notifies the observers. 
	 * @param status
	 */
	public void changeStatus(String status){
		this.status=status;
		this.setChanged();
		this.notifyObservers(status);
	}

	/**Used to get the status
	 * @return String status
	 */
	public String getStatus(){
		return status;
	}

}
