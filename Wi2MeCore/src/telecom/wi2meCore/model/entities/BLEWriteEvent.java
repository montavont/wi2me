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

package telecom.wi2meCore.model.entities;

import telecom.wi2meCore.model.entities.Trace.TraceType;

public class BLEWriteEvent extends Trace{

	public static final String TABLE_NAME = "BLEWriteEvent";
	private String deviceAddress;
	private String serviceUuid;
	private String characteristicUuid;
	private String charValue;

	protected BLEWriteEvent(Trace trace, String deviceAddress, String serviceUuid, String characteristicUuid, String charValue)
	{
		Trace.copy(trace, this);
		this.deviceAddress = deviceAddress;
		this.serviceUuid = serviceUuid;
		this.characteristicUuid = characteristicUuid;
		this.charValue = charValue;
	}

	public static BLEWriteEvent getNewBLEWriteEvent(Trace trace, String deviceAddress, String serviceUuid, String characteristicUuid, String charValue)
	{
		return new BLEWriteEvent(trace, deviceAddress, serviceUuid, characteristicUuid, charValue);
	}

	public String toString(){
		return super.toString() + "BLE_WRITE_EVENT:" +  deviceAddress + ':' +  serviceUuid + ':' +  characteristicUuid + ':' +  charValue;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.BLE_WRITE_EVENT;
	}

	public String getDeviceAddress()
	{
		return deviceAddress;
	}

	public String getServiceUuid()
	{
		return serviceUuid;
	}

	public String getCharacteristicUuid()
	{
		return characteristicUuid;
	}

	public String getCharValue()
	{
		return charValue;
	}

}
