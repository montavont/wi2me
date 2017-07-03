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

package telecom.wi2meCore.controller.services;

import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.cell.ICellularConnectionEventReceiver;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;

public class ThreadSynchronizingService implements IThreadSynchronizingService {
	private CellThreadContainer cellThreadContainer;
	private DisconnectionReceiver receiver;
	private DisconnectionWaitingThread disconnectionWaitingThread;
	
	
	public ThreadSynchronizingService(CellThreadContainer cellThreadCont){
		this.cellThreadContainer = cellThreadCont;
		receiver = new DisconnectionReceiver();		
	}
	

	@Override
	public void interruptCellThread() {
		cellThreadContainer.cellThread.interrupt();
	}
	
	
	/* (non-Javadoc)
	 * @see telecom.wi2meTraces.services.IThreadSynchonizingService#stopCellThread()
	 */
	@Override
	public void syncCellThread(boolean interrupt) throws TimeoutException, InterruptedException{	
		ControllerServices.getInstance().getCell().registerDisconnectionReceiver(receiver);		
		synchronized (receiver) {
			disconnectionWaitingThread = new DisconnectionWaitingThread();
		}		
		disconnectionWaitingThread.start();
				
		//Interrupt the cell thread if it is needed 
		if (interrupt)
			interruptCellThread();
		
		//wait until it finishes disconnecting
		try {
			disconnectionWaitingThread.join();
			if (disconnectionWaitingThread.timeout){
				throw new TimeoutException("Cannot interrupt cellular connection.");
			}
				
		} catch (InterruptedException e) {
			throw e;
		} finally{
			synchronized (receiver) {
				disconnectionWaitingThread = null;
			}
			ControllerServices.getInstance().getCell().unregisterDisconnectionReceiver(receiver);
		}
		
	}
	
	private void announceDisconnection(){
		if (disconnectionWaitingThread != null){
			disconnectionWaitingThread.interrupt();
		}
	}
	
	private class DisconnectionWaitingThread extends Thread{
		public boolean timeout;
		public void run(){
			try {
				Thread.sleep(TimeoutConstants.CELL_CONNECTION_CHANGE_TIMEOUT);
				timeout = true;
			} catch (InterruptedException e) {
				//if it is interrupted, finish ok
				timeout = false;
			}
		}
	}
	
	private class DisconnectionReceiver implements ICellularConnectionEventReceiver{

		@Override
		public void receiveEvent(String event) {
			synchronized(this){
				announceDisconnection();
			}			
		}
		
	}


}
