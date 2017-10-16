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

import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.util.Log;
import java.util.HashMap;

/**
 * This command is generic, and is used to clean the features of either wifi or cellular connections before starting the loop all over again.
 * It also flushes the log so that the traces obtained are persisted.
 * It then checks if the trace had its first fix to continue. The FIRST_FIX_WAITING parameter must be set in true for this.
 * @author Alejandro
 *
 */




public abstract class CleanerCommand extends WirelessNetworkCommand
{

	private static final int FIRST_FIX_WAITING_TIME = 3000;

	@Override
	public void initializeCommand(IParameterManager parameters) {
		// DO NOTHING
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING
	}

	@Override
	public void run(IParameterManager parameters)
	{
		//first we clean
		clean(parameters);

		//then we flush the log
		Logger.getInstance().flush();

		//now check for first fix to continue
		 if ((Boolean)parameters.getParameter(Parameter.FIRST_FIX_WAITING)){
			//Check if a first fix has been obtained by the trace so everything is ready to run the following commands
			while (!TraceManager.isFirstFixed()){
				try {
					Log.d(getClass().getSimpleName(), "++ "+"Not first fixed. About to sleep.");
					Thread.sleep(FIRST_FIX_WAITING_TIME);
				} catch (InterruptedException e) {
					break; //if interrupted, leave the loop
				}
			}
		}
	}

	/**
	 * Cleans the data of the concerned network interface.
	 * Used to reset all data at the beginning of each loop.
	 * @param parameters
	 */
	public abstract void clean(IParameterManager parameters);

}
