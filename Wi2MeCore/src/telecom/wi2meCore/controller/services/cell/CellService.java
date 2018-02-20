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

package telecom.wi2meCore.controller.services.cell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoCdma;
import android.telephony.SignalStrength;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthWcdma;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.model.entities.Cell;
import telecom.wi2meCore.model.entities.CellularSignalStrengthEvent;
import telecom.wi2meCore.model.TraceManager;

/**
 * Service managing the cell connection.
 * Allows to get current status, connect, disconnect, etc.
 * @author XXX
 *
 */
public class CellService implements ICellService {

	private static final String DATA_CONNECTING_TIMEOUT_MESSAGE = "The timeout for connecting to a data network elapsed";
	private static final String DATA_DISCONNECTING_TIMEOUT_MESSAGE = "The timeout for disconnecting to a data network elapsed";

	private static final String CANNOT_DISCONNECT_MESSAGE = "FATAL ERROR: Cellular network cannot be disconnected. Unable to continue";


	private TelephonyManager telephonyManager;
	private ConnectivityManager connectivityManager;
	private ConnectionChangeThread connectionChangeThread;
	private CellInfo currentCell;
	private ServiceStateListener stateListener;
	private CellLocationListener locationListener;
	private SignalStrengthListener signalListener;
	private ScanningThread scanThread;
	private NetworkConnectionEventReceiver netEventReceiver;
	private Context context;
	private List<ICellularConnectionEventReceiver> disconnectionEventReceivers;

	public CellService(Context context){
		this.context = context;
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		netEventReceiver = new NetworkConnectionEventReceiver();
		context.registerReceiver(netEventReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		stateListener = new ServiceStateListener();
		telephonyManager.listen(stateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
		locationListener = new CellLocationListener();
		telephonyManager.listen(locationListener, PhoneStateListener.LISTEN_CELL_LOCATION);
		signalListener = new SignalStrengthListener();
		telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		currentCell = new CellInfo();

		disconnectionEventReceivers = new ArrayList<ICellularConnectionEventReceiver>();
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#finalizeService()
	 */
	@Override
	public void finalizeService(){
		//unregister
		telephonyManager.listen(stateListener, PhoneStateListener.LISTEN_NONE);
		//unregister
		telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE);
		//unregister
		telephonyManager.listen(locationListener, PhoneStateListener.LISTEN_NONE);

		context.unregisterReceiver(netEventReceiver);
	}

	public NetworkInfo getCellularDataNetworkInfo(){
		return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#scan()
	 */
	@Override
	public CellInfo scan() throws InterruptedException{
		CellInfo ret = new CellInfo();
		synchronized (currentCell) {
			if (currentCell.isChanged()){
				currentCell.copy(ret);
				currentCell.setChanged(false);
				return ret;
			}
		}
		//If it has not changed since the last copy requested, we need to wait for a new result to appear and scan again
		if (scanThread != null){
			if (scanThread.isAlive()){ //If it is running we should wait it to finish
				return null;
			}
		}
		scanThread = new ScanningThread();
		scanThread.start();
		try {
			scanThread.join();
			//If we reach this is because the thread was interrupted, so we return changes
			return scan();
		} catch (InterruptedException e) {
			//If we are interrupted is because we needed interruption from the outside, stop thread and rethrow
			scanThread.interrupt();
			throw e;
		} finally{
			scanThread = null;
		}

	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#getLastScannedCell()
	 */
	@Override
	public CellInfo getLastScannedCell(){
		//As the currentCell has synchronized methods, we do not need to synchronize
		return currentCell.getCopyOfCurrentCell();
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#isDataNetworkConnected()
	 */
	@Override
	public boolean isDataNetworkConnected(){
		//return telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED;

		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		if (netInfo != null){
			return netInfo.isConnected() && (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) && (telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED);
		}else{
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#isPhoneNetworkConnected()
	 */
	@Override
	public boolean isPhoneNetworkConnected(){
		return telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#isDataTransferringEnabled()
	 */
	@Override
	public boolean isDataTransferringEnabled(){
		return getMobileDataEnabled();
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#connect()
	 */
	@Override
	public boolean connect() throws InterruptedException, TimeoutException{
		return changeConnectionState(true);
	}

	/* (non-Javadoc)
	 * @see android.servicetest.ICellService#disconnect()
	 */
	@Override
	public boolean disconnect() throws InterruptedException, TimeoutException{
		return changeConnectionState(false);
	}

	private class ConnectionMethod {

		private Method dataConnSwitchmethod;
		private Object ITelephonyStub;;

		@SuppressWarnings("rawtypes")
		public ConnectionMethod(boolean connect) throws Exception{
	        Class telephonyManagerClass;
	        Class ITelephonyClass;
	        String methodToRun;

	        if (connect){
	        	methodToRun = "enableDataConnectivity";
	        }else{
	        	methodToRun = "disableDataConnectivity";
	        }
	        try{
	            telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
		        Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
		        getITelephonyMethod.setAccessible(true);
		        ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
		        ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
		        dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod(methodToRun);
		        dataConnSwitchmethod.setAccessible(true);
	        }catch (Exception e){
	        	throw e;
	        }
		}

		public void run() throws Exception
		{
		        //we call the method
		        try
			{
				dataConnSwitchmethod.invoke(ITelephonyStub);
			}
			catch (Exception e)
			{
            			Log.e(this.getClass().getSimpleName(), "++ "+e.getMessage(), e);
				throw e;
			}
		}

	}

	private boolean changeConnectionState(boolean finalStateConnected) throws InterruptedException, TimeoutException{
		ConnectionMethod connectionMethod;
        String timeoutMessage;

 	//TKER much of the following code is deprecated, look into
	if (true)
	{
		return true;
	}

        if(finalStateConnected == this.isDataNetworkConnected()){
            return true;
        }
        if (connectionChangeThread != null)
        	if (connectionChangeThread.isAlive())
        		return false;

        try{
	        if (finalStateConnected){
	            if (!this.isDataTransferringEnabled())
	            	return false;
	            connectionMethod = new ConnectionMethod(true);
	        	timeoutMessage = DATA_CONNECTING_TIMEOUT_MESSAGE;
	        	//connectionStateListener.setDesiredConnectionState(TelephonyManager.DATA_CONNECTED);
	        	netEventReceiver.setDesiredState(NetworkInfo.DetailedState.CONNECTED);

	        }else{
	        	connectionMethod = new ConnectionMethod(false);
	        	timeoutMessage = DATA_DISCONNECTING_TIMEOUT_MESSAGE;
	        	//connectionStateListener.setDesiredConnectionState(TelephonyManager.DATA_DISCONNECTED);
	        	netEventReceiver.setDesiredState(NetworkInfo.DetailedState.DISCONNECTED);
	        }

        	/*
            telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
	        Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
	        getITelephonyMethod.setAccessible(true);
	        ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
	        ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
	        dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod(methodToRun);
	        dataConnSwitchmethod.setAccessible(true);	 */

	        //We run the thread so that it can be interrupted when connected and we can return the function when connected
	        //We synchronize with the thread listening for state changed
	        synchronized (/*connectionStateListener*/netEventReceiver) {
				connectionChangeThread = new ConnectionChangeThread();
				connectionChangeThread.start();
			}
	        /*
	        //we call the method
	        dataConnSwitchmethod.invoke(ITelephonyStub);
	        */
	        connectionMethod.run();

	        //we wait for it to finish
	        connectionChangeThread.join();
	        if (connectionChangeThread.timeoutElapsed){//If the timeout elapsed, throw an exception
	        	throw new TimeoutException(timeoutMessage);
	        }
	        return true;

        } catch(InterruptedException e){
        	/*
        	//If we are interrupted, re-throw exception
        	connectionChangeThread.join(TimeoutConstants.CELL_CONNECTION_CHANGE_TIMEOUT);
        	if (finalStateConnected){//If we were connecting we also want connection not to take place! So wait until it finishes and disconnect
	        	connectionChangeThread = null;
	        	disconnect();
        	}
        	*/
            throw e;
        } catch (TimeoutException ex){
        	throw ex;
        } catch(Exception e){
            Log.e(this.getClass().getSimpleName(), "++ "+e.getMessage(), e);
            return false;
        }finally{
        	synchronized (/*connectionStateListener*/netEventReceiver) {
        		connectionChangeThread = null;
        	}
        }
	}

    @SuppressWarnings("rawtypes")
	private boolean getMobileDataEnabled() {
    	try{
	    	Class c = Class.forName(connectivityManager.getClass().getName());
			Method getMobileDataEnabledMethod = c.getDeclaredMethod("getMobileDataEnabled");
		 	getMobileDataEnabledMethod.setAccessible(true);
			Object ret = getMobileDataEnabledMethod.invoke(connectivityManager);
			return ((Boolean) ret).booleanValue();
    	}catch (NoSuchMethodException e){
    		//If we do not have the method it is because it is enabled by default (version 2.1 or lower)
    		Log.w(this.getClass().getSimpleName(), "++ "+e.getMessage(), e);
    		return true;
    	}catch(Exception e){
    		Log.e(this.getClass().getSimpleName(), "++ "+e.getMessage(), e);
    		return false;
    	}
	}
	private void announceConnectionChanged(){
		synchronized (/*connectionStateListener*/netEventReceiver) {
			if (connectionChangeThread != null){
				connectionChangeThread.interrupt();
			}
		}
	}

	private class ConnectionChangeThread extends Thread{

		public boolean timeoutElapsed = false;
		public void run(){
			try {
				Thread.sleep(TimeoutConstants.CELL_CONNECTION_CHANGE_TIMEOUT);
				timeoutElapsed = true;
			} catch (InterruptedException e) {
				timeoutElapsed = false;
			}
		}
	}


	private class ScanningThread extends Thread{
		public static final int SLEEP_TIME = 1000000; //in milliseconds
		public void run(){
			while(true){
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					//If interrupted, we have the change we were waiting for. Finish
					break;
				}
			}
		}
	}

	private void announceCellChanged(){
		if (scanThread != null){
			scanThread.interrupt();
		}
	}

	private class CellLocationListener extends PhoneStateListener{
		public void onCellLocationChanged (CellLocation location){
			super.onCellLocationChanged(location);
			//Cell has changed, so we update its state and inform it has changed
			//As the currentCell has synchronized methods, we do not need to synchronize
			currentCell.update(telephonyManager);
			announceCellChanged();
		}
	}

	private class SignalStrengthListener extends PhoneStateListener{
		public void onSignalStrengthsChanged(SignalStrength signalStrength){
			super.onSignalStrengthsChanged(signalStrength);

			// Update current cell independently of cellular type
			currentCell.setLevel(signalStrength.getCdmaDbm());
			announceCellChanged();

			// Look into more precise cell info
			// Can't believe this is not passed to LISTEN_CELL_INFO listeners...
			List<android.telephony.CellInfo> cellInfos = telephonyManager.getAllCellInfo();
			if(cellInfos!=null){
				for (int i = 0 ; i<cellInfos.size(); i++){
       				if (cellInfos.get(i).isRegistered()){
						//Only LTE supported for now
    	            	if(cellInfos.get(i) instanceof CellInfoLte){
	                	    CellInfoLte cellInfoLte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            	        	CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
							Cell currentCell = Cell.getNewCellFromCellInfo(ControllerServices.getInstance().getCell().getLastScannedCell());
							Logger.getInstance().log(CellularSignalStrengthEvent.getNewCellularSignalStrengthEvent(TraceManager.getTrace(), currentCell , (CellSignalStrength)cellSignalStrengthLte));
                		}
            		}
        		}
			}
		}
	}

	/**
	 * This class is only for getting the ServiceState, which informs the operator (MCC + 00) for CDMA networks
	 * @author Alejandro
	 *
	 */

	private class ServiceStateListener extends PhoneStateListener{

		@Override
        public void onServiceStateChanged(ServiceState serviceState) {
			currentCell.setServiceState(serviceState);
		}
	}

	private class NetworkConnectionEventReceiver extends BroadcastReceiver{
		private NetworkInfo.DetailedState desiredState;
		public synchronized NetworkInfo.DetailedState getDesiredState() {
			return desiredState;
		}
		public synchronized void setDesiredState(NetworkInfo.DetailedState desiredState) {
			this.desiredState = desiredState;
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if (mNetworkInfo != null){
				if (mNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
					if (mNetworkInfo.getDetailedState() == getDesiredState()){
						announceConnectionChanged();
					}
					switch (mNetworkInfo.getDetailedState()){
					/*
					case CONNECTED:
						break;
						*/
					case DISCONNECTED:
						broadcastDisconnectionEvent(mNetworkInfo.getDetailedState().name());
						break;
					}
					Log.d(this.getClass().getSimpleName() + "-CellService", "++ "+mNetworkInfo.getDetailedState().name());
				}
			}else{
				Log.d(this.getClass().getSimpleName() + "-CellService", "++ "+"NetworkInfo = null");
			}
		}

	}

	@Override
	public synchronized void registerDisconnectionReceiver(ICellularConnectionEventReceiver receiver) {
		disconnectionEventReceivers.add(receiver);
	}

	@Override
	public synchronized void unregisterDisconnectionReceiver(ICellularConnectionEventReceiver receiver) {
		disconnectionEventReceivers.remove(receiver);
	}

	private synchronized void broadcastDisconnectionEvent(String event){
		for (ICellularConnectionEventReceiver rec : disconnectionEventReceivers){
			rec.receiveEvent(event);
		}
	}

	@Override
	public void disconnectOrDie() {
		//Make sure previous connections are closed
        try {
			if (!disconnect())
				throw new RuntimeException(CANNOT_DISCONNECT_MESSAGE);
		} catch (TimeoutException e) {
			// fatal error
			Log.e(getClass().getSimpleName(), "++ "+"Could not disconnect cellular network", e);
			throw new RuntimeException(CANNOT_DISCONNECT_MESSAGE);
		} catch (InterruptedException e) {
			// if we are interrupted, we finish
			Log.d(getClass().getSimpleName(), "++ "+"Interrupted while disconnecting", e);
		}
	}

	@Override
	public void connectAsync() {
		try{
			ConnectionMethod connect = new ConnectionMethod(true);
			connect.run();
		}catch (Exception e){
			// if an exception happens, connection cannot be made
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		}
	}


}
