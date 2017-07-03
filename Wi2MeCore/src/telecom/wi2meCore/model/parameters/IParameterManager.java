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

package telecom.wi2meCore.model.parameters;

/**
 * This class manages the parameters.
 * It is organized as follows:
 * at the first creation, it is created as a Generic one.
 * Then, when attempting to get a parameter, the generic parameterManager will create the ParameterManager for the given parameter and stores it in the variable "next".
 * Then, when attempting to get another parameter, the generic parameterManager will forward the request to its next ParameterManager.
 * It can't handle the request (not the same parameter), so it will create the ParameterManager for the given parameter and stores it in the variable "next".
 * And so on and so on.
 * @author XXX + Gilles Vidal
 *
 */
public interface IParameterManager {
	
	/**
	 * Returns the parameter if corresponds to this object, else relays the request to the "next".
	 * If no value can be found, the default value is returned.
	 * @param type
	 * @return The parameter value as an Object
	 */
	Object getParameter(Parameter type);
	/**
	 * Used to build the configuration files when they are missing/defective. Gives the default values to all the parameters?
	 * @return
	 */
	String buildConfigFile();
	/**
	 * Sets the parameter if corresponds to this object, else relays the request to the "next".
	 * @param type
	 * @param parameter
	 */
	void setParameter(Parameter type, Object parameter);

}
