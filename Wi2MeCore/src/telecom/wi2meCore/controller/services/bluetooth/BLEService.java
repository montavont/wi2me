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

package telecom.wi2meCore.controller.services.ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

public class BLEService implements IBLEService{

	private Context context;

	public BLEService(Context context){
		this.context = context;
		mBluetoothManager = (BluetoothManager)this.context.getApplicationContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		mBluetoothAdapter.enable();
	}

	@Override
	public void finalizeService() {
	}

   	private final static String TAG = BLEService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    // Various callback methods defined by the BLE API.
    private class localBLEGattCallback extends BluetoothGattCallback
	{
			public boolean discovered = false;
			public boolean characteristic_read = false;

	        @Override
    	    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
			{
        	    //String intentAction;
            	if (newState == BluetoothProfile.STATE_CONNECTED)
				{
                    gatt.discoverServices();
				}
				else
				{
                	Log.w(TAG, "Not connected: " + status + "	" + newState);
				}
	        }

	        @Override
    	    // New services discovered
        	public void onServicesDiscovered(BluetoothGatt gatt, int status)
			{
	            if (status == BluetoothGatt.GATT_SUCCESS) {
					discovered = true;
            	}
				else
				{
                	Log.w(TAG, "onServicesDiscovered received: " + status);
	            }
	        }

	        @Override
    	    // Result of a characteristic read operation
        	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
			{
            	if (status == BluetoothGatt.GATT_SUCCESS)
				{
					characteristic_read = true;
	            }
    	    }
    }

	@Override
	public boolean writeCharacteristic(String charValue, String deviceAddr, String serviceUUID, String characteristicUUID)
	{
		boolean retval = false;
		int timeOut = 10000;
		int discoveryLoop = 500;
    	localBLEGattCallback btcb = new localBLEGattCallback();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
		BluetoothGatt gatt = device.connectGatt(this.context, false, (BluetoothGattCallback) btcb);

        //check mBluetoothGatt is available
        if (gatt == null) {
            Log.e(getClass().getSimpleName(), "lost connection");
        }
		else
		{
			for (int i = 0; i <= timeOut; i += discoveryLoop)
			{
				if(btcb.discovered)
				{
					break;
				}
				else
				{
					try{
						Thread.sleep(discoveryLoop);
					}
					catch (InterruptedException e)
					{
						break;
					}
				}
			}

	        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
    	    if (service == null) {
        	    Log.e(getClass().getSimpleName(), "service not found!");
	        }
			else
			{
		        BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString(characteristicUUID));
		        if (charac == null) {
        		    Log.e(getClass().getSimpleName(), "characateristic not found!");
        		}
				else
				{
			        charac.setValue(charValue);
        			retval = gatt.writeCharacteristic(charac);
				}
			}
		}
		gatt.close();
   		return retval;
	}

	@Override
	public String readCharacteristic(String deviceAddr, String serviceUUID, String characteristicUUID)
	{
		String retval = "";
		String part = " ";
		while (part != null && part.length() > 0)
		{
			part = readCharacteristicPart(deviceAddr, serviceUUID, characteristicUUID);
			if (part != null)
			{
				retval += part;
			}
		}
		return retval;
	}

	private String readCharacteristicPart(String deviceAddr, String serviceUUID, String characteristicUUID)
	{
		String retval = null;
		int timeOut = 10000;
		int discoveryLoop = 500;
    	localBLEGattCallback btcb = new localBLEGattCallback();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
		BluetoothGatt gatt = device.connectGatt(this.context, true, (BluetoothGattCallback) btcb);

        //check mBluetoothGatt is available
        if (gatt == null) {
            Log.e(getClass().getSimpleName(), "lost connection");
        }
		else
		{
			for (int i = 0; i <= timeOut; i += discoveryLoop)
			{
				if(btcb.discovered)
				{
            		Log.e(getClass().getSimpleName(), "discovered  !");
					break;
				}
				else
				{
					try{
						Thread.sleep(discoveryLoop);
					}
					catch (InterruptedException e)
					{
						break;
					}
				}
			}

        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.e(getClass().getSimpleName(), "service not found!");
        }
		else
		{
		        BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString(characteristicUUID));
   			    if (charac == null) {
        		    Log.e(getClass().getSimpleName(), "characteristic not found!");
				}
				else
				{

        			boolean status = gatt.readCharacteristic(charac);
					if (status)
					{
						for (int i = 0; i <= timeOut; i += discoveryLoop)
						{
							if(btcb.characteristic_read)
							{
								btcb.characteristic_read = false;
								break;
							}
							else
							{
								try{
									Thread.sleep(discoveryLoop);
								}
								catch (InterruptedException e)
								{
									break;
								}
							}
						}
		        		retval = charac.getStringValue(0);
					}
				}
			}
		}
		gatt.close();
    	return retval;
	}
}
