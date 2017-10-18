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
	}

	@Override
	public void finalizeService() {
	}

   	private final static String TAG = BLEService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // Various callback methods defined by the BLE API.
    //private final BluetoothGattCallback mGattCallback =
    private class localBLEGattCallback extends BluetoothGattCallback
	{
			public boolean discovered = false;
			public boolean characteristic_read = false;

	        @Override
    	    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
			{
        	    String intentAction;
            	if (newState == BluetoothProfile.STATE_CONNECTED)
				{
                    gatt.discoverServices();
				}
	        }

	        @Override
    	    // New services discovered
        	public void onServicesDiscovered(BluetoothGatt gatt, int status)
			{
	            if (status == BluetoothGatt.GATT_SUCCESS) {
					discovered = true;
        			Log.d(getClass().getSimpleName(), " discovered serve size "  + gatt.getServices().size());
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
		mBluetoothManager = (BluetoothManager)this.context.getApplicationContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
		BluetoothGatt gatt = device.connectGatt(this.context, true, (BluetoothGattCallback) btcb);//TKE TODO : reconnect only if necessary

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
        			boolean status = gatt.writeCharacteristic(charac);
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
		int timeOut = 10000;
		int discoveryLoop = 500;
    	localBLEGattCallback btcb = new localBLEGattCallback();
		mBluetoothManager = (BluetoothManager)this.context.getApplicationContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddr);
		BluetoothGatt gatt = device.connectGatt(this.context, true, (BluetoothGattCallback) btcb);//TKE TODO : reconnect only if necessary

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
