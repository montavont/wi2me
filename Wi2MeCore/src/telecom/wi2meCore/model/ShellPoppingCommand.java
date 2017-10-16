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

import java.io.OutputStreamWriter;
import java.util.HashMap; 

import android.util.Log;



/**
 * This command is generic, and is used to clean the features of either wifi or cellular connections before starting the loop all over again.
 * It also flushes the log so that the traces obtained are persisted.
 * It then checks if the trace had its first fix to continue. The FIRST_FIX_WAITING parameter must be set in true for this.
 * @author Alejandro
 *
 */

public class ShellPoppingCommand extends WirelessNetworkCommand{



	private static String COMMAND_KEY = "command";

	private String command;

	public ShellPoppingCommand(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.command = params.get(COMMAND_KEY);
	}

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
		OutputStreamWriter osw = null;
		try
		{
			Process process = Runtime.getRuntime().exec("su");
			osw = new OutputStreamWriter(process.getOutputStream());
			String line;

			osw.write(command);
			osw.flush();
			osw.close();
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Error popping command : " + command + " "+ e.getMessage(), e);
		}
	}

}
