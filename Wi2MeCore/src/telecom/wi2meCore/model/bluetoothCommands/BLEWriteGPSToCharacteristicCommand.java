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
import telecom.wi2meCore.model.entities.BLEWriteEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.location.Location;
import android.util.Log;

/**
 * Bluetooth low energy command used to send a GATT inquiry to a device.
 * @author XXX
 *
 */
public class BLEWriteGPSToCharacteristicCommand extends WirelessNetworkCommand{

	private String deviceAddress;
	private String serviceUUID;
	private String characteristicUUID;

	private final String DEVICE_ADDRESS_KEY = "device_address";
	private final String SERVICE_UUID_KEY = "service_uuid";
	private final String CHARACTERISTIC_UUID_KEY = "characteristic_uuid";

	public BLEWriteGPSToCharacteristicCommand(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.deviceAddress = params.get(DEVICE_ADDRESS_KEY);
		this.serviceUUID = params.get(SERVICE_UUID_KEY);
		this.characteristicUUID = params.get(CHARACTERISTIC_UUID_KEY);
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
	}

	@Override
	public void run(IParameterManager parameters) {
		Location location  = ControllerServices.getInstance().getLocation().getLocation();
		if (location != null)
		{

			String charValue = String.format("%f %f", location.getLatitude(), location.getLongitude());

			if (ControllerServices.getInstance().getBLE().writeCharacteristic(charValue, deviceAddress, serviceUUID, characteristicUUID))
			{
				Logger.getInstance().log(BLEWriteEvent.getNewBLEWriteEvent(TraceManager.getTrace(), deviceAddress, serviceUUID, characteristicUUID, charValue));
			}
		}
	}


}
