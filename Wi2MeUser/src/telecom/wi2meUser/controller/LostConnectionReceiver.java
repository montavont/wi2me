package telecom.wi2meUser.controller;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class LostConnectionReceiver extends BroadcastReceiver {

	ApplicationService appService;
	
	public LostConnectionReceiver (ApplicationService serv) {
		this.appService = serv;
	}
	
	public void register() {
		//	Register the LostConnectionReceiver for the right event
		Log.d("MOBIDAC_wi2me", "Registering LostConnection receiver");
		IntentFilter filter = new IntentFilter();
		filter.addAction("LOST_INTERNET_CONNECTION");
		this.appService.registerReceiver((BroadcastReceiver) this, filter);	
	}
	
	public void unregister() {
		Log.d("MOBIDAC_wi2me", "Unegistering LostConnection receiver");
		this.appService.unregisterReceiver((BroadcastReceiver) this);
	}
	
	
	//------------------------------------------------------------------------------
	@Override
	public void onReceive(Context context, Intent intent) {
		// One one event is expected, so no choice
		Log.d("MOBIDAC_wi2me", "On receive LostConnection event");
		this.appService.binder.forceReconnection();
	}
	
}
