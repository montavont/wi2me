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

import telecom.wi2meCore.controller.services.exceptions.TimeoutException;


public interface IThreadSynchronizingService {
/**
 * This method is used to synchronize the cell thread with the wifi thread.
 * If the cell thread is downloading or uploading, it is needed to interrupt it, and then the thread might need to read a variable that does not allow it to go on and make a disconnection. This method will wait for that disconnection to take place.
 * @param interrupt If the thread must be interrupted or not
 * @throws TimeoutException If waiting for disconnection for more than the timeout
 * @throws InterruptedException If this method is interrupted
 */
	void syncCellThread(boolean interrupt) throws TimeoutException, InterruptedException;
	
	/**
	 * Only interrupts the cell thread, and it may continue with its following duties.
	 */
	void interruptCellThread();

}