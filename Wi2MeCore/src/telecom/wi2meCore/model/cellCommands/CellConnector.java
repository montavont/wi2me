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

package telecom.wi2meCore.model.cellCommands;


import java.util.HashMap;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.cell.CellInfo;
import telecom.wi2meCore.controller.services.cell.ICellularConnectionEventReceiver;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.Cell;
import telecom.wi2meCore.model.entities.CellularConnectionEvent;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.os.Build;
import android.util.Log;

/**
 * Wireless network command used to connect to a cell tower.
 * @author XXX
 *
 */
public class CellConnector extends WirelessNetworkCommand{

	private static final String FAILED = "FAILED";
	private static final String CONNECTING = "CONNECTING";
	private static final String CONNECTED = "CONNECTED";
	private static final String DISCONNECTED = "DISCONNECTED";
	private static final String CONNECTING_TIMEOUT = "TIMEOUT";
	private static final String CONNECTING_INTERRUPTED = "INTERRUPTED";

	private IParameterManager parameters;
	private ICellularConnectionEventReceiver connectionEventReceiver;

	public CellConnector() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public CellConnector(HashMap<String, String> params)
	{
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
		this.parameters = parameters;
		this.connectionEventReceiver = new CellularConnectionEventReceiver();
		ControllerServices.getInstance().getCell().registerDisconnectionReceiver(connectionEventReceiver);
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		ControllerServices.getInstance().getCell().unregisterDisconnectionReceiver(connectionEventReceiver);
	}

	@Override
	public void run(IParameterManager parameters) {
		if (!(Boolean)parameters.getParameter(Parameter.CONNECT_CELLULAR))
			return; //if we are not meant to connect, we don't
		CellInfo lastScannedCell = ControllerServices.getInstance().getCell().getLastScannedCell();
		//just in case, we check that we don't get an invalid CellInfo
		if (lastScannedCell.equals(new CellInfo()))
			return;
		//now we may continue
		if (!(Boolean)parameters.getParameter(Parameter.WIFI_CONNECTION_ATTEMPT)){
			boolean connected = false;
			CellTransferrerContainer transferCommands = (CellTransferrerContainer) parameters.getParameter(Parameter.CELL_TRANSFER_COMMANDS);
			//If the current cell was not completely tested, we attempt to connect and test it
			if (!transferCommands.wasCompletelyTested(lastScannedCell)){

				if (!(Boolean)parameters.getParameter(Parameter.CELL_CONNECTED)){
					//we first wait a period of time
					try {
						Thread.sleep((Integer)parameters.getParameter(Parameter.CELL_CONNECTION_DELAY));
					} catch (InterruptedException e1) {
						// If we are interrupted, we finish execution
						Log.d(getClass().getSimpleName(), "++ "+"Connecting Interrupted while sleeping to connect", e1);
						return;
					}

					//we check that the cell did not change again to the previous one while we slept
					if (!lastScannedCell.equals(ControllerServices.getInstance().getCell().getLastScannedCell()))
						return;

					//now we will try to connect, but just in case we check for a wifi attempt, as we have been sleeping
					if ((Boolean)parameters.getParameter(Parameter.WIFI_CONNECTION_ATTEMPT))
						return;
					else{
						//Inform about the connection attempt
						parameters.setParameter(Parameter.CELL_CONNECTION_ATTEMPT, true);
						parameters.setParameter(Parameter.CELL_CONNECTING, true);
					}

					connectionEventReceiver.receiveEvent(CONNECTING);
					try {
						if (connect()){
							connected = true;
							connectionEventReceiver.receiveEvent(CONNECTED);
						}else{
							connected = false;
							connectionEventReceiver.receiveEvent(FAILED);
						}
					} catch (TimeoutException e) {
						Log.e(getClass().getSimpleName(), "++ "+"Connecting Timeout", e);
						connectionEventReceiver.receiveEvent(CONNECTING_TIMEOUT+"("+ TimeoutConstants.CELL_CONNECTION_CHANGE_TIMEOUT +"ms)");
						connected = false;
					} catch (InterruptedException e) {
						Log.d(getClass().getSimpleName(), "++ "+"Connecting Interrupted", e);
						connectionEventReceiver.receiveEvent(CONNECTING_INTERRUPTED);
						// If connection is interrupted, we finish here
						connected = false;
					}
				}
			}
			parameters.setParameter(Parameter.CELL_CONNECTION_ATTEMPT, connected);
			parameters.setParameter(Parameter.CELL_CONNECTED, connected);
			parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, connected);
			parameters.setParameter(Parameter.CELL_CONNECTING, false); //we make sure connecting state is not true
		}
	}

	public boolean connect() throws TimeoutException, InterruptedException
	{
		return ControllerServices.getInstance().getCell().connect();
	}


	/**
	 * This class is only to make sure that the parameters are in the correct state after disconnection
	 * @author Alejandro
	 *
	 */
	private class CellularConnectionEventReceiver implements ICellularConnectionEventReceiver{

		@Override
		public void receiveEvent(String event) {
			if ((Boolean)parameters.getParameter(Parameter.CELL_CONNECTION_ATTEMPT)){
				//No matter which event, we log it
				CellInfo cellInfo = ControllerServices.getInstance().getCell().getLastScannedCell();
				Cell connectionTo = Cell.getNewCellFromCellInfo(cellInfo);
				CellularConnectionEvent cellConnectionEvent = CellularConnectionEvent.getNewCellularConnectionEvent(TraceManager.getTrace(), event, connectionTo);
				Logger.getInstance().log(cellConnectionEvent);

				//we check if it is a disconnection event, and it was connected or connecting
				if ((Boolean)parameters.getParameter(Parameter.CELL_CONNECTED) || (Boolean)parameters.getParameter(Parameter.CELL_CONNECTING)){

					if (event.equals(DISCONNECTED)){
						parameters.setParameter(Parameter.CELL_TRANSFERRING, false);
						parameters.setParameter(Parameter.CELL_CONNECTING, false);
						parameters.setParameter(Parameter.CELL_CONNECTED, false);
						parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, false);
						parameters.setParameter(Parameter.CELL_CONNECTION_ATTEMPT, false);
					}
				}
			}
		}

	}

}
