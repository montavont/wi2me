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

import java.util.HashMap;

import android.util.Log;

public class SleepCommand extends WirelessNetworkCommand{

	private final String SLEEP_KEY = "sleep_ms";
	private int ms = 0;

	public SleepCommand(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.ms = Integer.parseInt(params.get(SLEEP_KEY));
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
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			Log.d(getClass().getSimpleName(), "Interrupted.");
		}
	}
}
