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

package telecom.wi2meCore.controller.services.persistance;

import telecom.wi2meCore.model.entities.Trace;

/**
 * Interface to implement when iterating Traces in the database
 * @author Alejandro
 *
 */
public interface ITraceIterator {

	/**
	 * Returns true if there is a Trace to read in the database 
	 */
	public boolean hasNext();
	/**
	 * Return the current Trace in the database
	 */
	public Trace next();
	/**
	 * Closes the iterator connection to the database. The iterator MUST be closed after usage.
	 */
	public void close();
}
