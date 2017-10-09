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

package telecom.wi2meCore.controller.services.trace;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BatteryService implements IBatteryService{
	
	private Context context;

	private List<BroadcastReceiver> broadcastReceivers;
	
	public BatteryService(Context context){
		this.context = context;
		broadcastReceivers = new ArrayList<BroadcastReceiver> ();
	}

	@Override
	public void registerLevelReceiver(IBatteryLevelReceiver receiver) {
		synchronized (this){
			BroadcastReceiver bReceiver = new BatteryBroadcastReceiver(receiver);
			broadcastReceivers.add(bReceiver);
			context.registerReceiver(bReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		}
	}

	@Override
	public void unregisterLevelReceiver(IBatteryLevelReceiver receiver) {
		synchronized (this){
			try{
				int index = broadcastReceivers.indexOf(new BatteryBroadcastReceiver(receiver));
				if (index == -1){ //receiver not found
					throw new Exception("Battery Receiver to unregister, not registered.");
				}
				BroadcastReceiver toUnregister = broadcastReceivers.remove(index);
				context.unregisterReceiver(toUnregister);
	    	}catch(Exception e){
	    		Log.e("Battery", "Unregistering Battery Level Receiver "+e.getMessage(), e);
	    	}
		}
	}

	private class BatteryBroadcastReceiver extends BroadcastReceiver {

		private IBatteryLevelReceiver receiver;

		public BatteryBroadcastReceiver(IBatteryLevelReceiver levelReceiver){
			this.receiver = levelReceiver;
		}

		@Override
		public boolean equals(Object o) {
			BatteryBroadcastReceiver other = (BatteryBroadcastReceiver) o;
			return receiver == other.receiver;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra("level", 0);
			receiver.receiveBatteryLevel(level);
		}
		
	}

	@Override
	public void finalizeService() {
		//if there is any receiver that was forgotten to unregister, this will do
		for (BroadcastReceiver r : this.broadcastReceivers){
			context.unregisterReceiver(r);
		}
	}

}
