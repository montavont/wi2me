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

package telecom.wi2meCore.model.bluetoothCommands;

import java.util.HashMap;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.location.Location;
import android.util.Log;

/**
 * Bluetooth low energy command used to send a GATT inquiry to a device.
 * @author XXX
 *
 */
public class BLEDummy extends WirelessNetworkCommand{

	public BLEDummy(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
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
	public void run(IParameterManager parameters) {
		Log.d(getClass().getSimpleName(), "Yay, we is of hellfest !");

		Location location  = ControllerServices.getInstance().getLocation().getLocation();
		if (location != null)
		{

			String charValue = String.format("%f %f", location.getLatitude(), location.getLongitude());

			ControllerServices.getInstance().getBLE().writeCharacteristic(charValue);
		}

		try{
			Thread.sleep(10000);
		}
		catch (InterruptedException e)
		{
		}
	}


}
