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

package telecom.wi2meCore.controller.services.move;

public interface IMoveService {

	/**
	 * Stops the service.
	 */
	void finalizeService();

	/**
	 * Tells whether the device is moving or not
	 * @return The moving state of the device
	 */
	boolean isMoving();

	/**
	 * Gives the move lock
	 * @return The move lock
	 */
	IMoveLock getMovingLock();
	
	/**
	 * Gives the time of the last movement
	 * @return The timestamp of the last movement
	 */
	long getLastMovementTimestamp();
	
	/**
	 * Sets the last movement timestamp to the current time.
	 */
	void resetLastMovementTimestamp();

}