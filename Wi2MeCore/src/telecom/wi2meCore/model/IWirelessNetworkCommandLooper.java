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

package telecom.wi2meCore.model;

import java.util.HashMap;
import java.util.List;

import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.Flag;
import telecom.wi2meCore.model.WirelessNetworkCommand;

public interface IWirelessNetworkCommandLooper {

	/**
	 * Initializes all commands within the looper.
	 * @param parameters the parameters for the commands to initialize
	 */
	void initializeCommands(IParameterManager parameters);
	/**
	 * Finalizes all commands within the looper.
	 * @param parameters the parameters for the commands to finalize
	 */
	void finalizeCommands(IParameterManager parameters);

	/**
	 * Appends a command to the looper.
	 * @param command to be appended
	 */
	void addCommand(WirelessNetworkCommand command);

	/**
	 * Get the looper's commands
	 */
	List<WirelessNetworkCommand> getCommands();

	/**
	 * Stops looping the commands
	 */
	void breakLoop();

	/**
	 * Get a string describing the state of the looper
	 */
	HashMap<String, String> getStates();

	/**
	 * Loops the commands one after another until the passed Flag is deactivated.
	 * @param parameters The parameters needed by the commands. A Flag is also needed within the parameters to inform the looper when to stop working
	 * @param workingFlag The variable that makes the looper continue looping the following command or stop
	 */
	void loop(IParameterManager parameters);

}
