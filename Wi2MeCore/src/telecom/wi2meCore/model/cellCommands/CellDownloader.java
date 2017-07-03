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

import java.util.ArrayList;
import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.configuration.Timers;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.cell.CellInfo;
import telecom.wi2meCore.controller.services.exceptions.DownloadingFailException;
import telecom.wi2meCore.controller.services.exceptions.DownloadingInterruptedException;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.util.Log;



/**
 * Wireless network command used to download a file using the cellular network.
 * @author XXX
 *
 */
public class CellDownloader extends WirelessNetworkCommand{
	
	private String host;
	private String filePath;
	private long length;
	private List<CellInfo> tested;
		
	private static String SERVER_KEY = "server";
	private static String PATH_KEY = "path";
	private static String LENGTH_KEY = "size";

	public CellDownloader(HashMap<String, String> params)
	{
		this.host = params.get(SERVER_KEY);
		this.filePath = params.get(PATH_KEY);
		this.length = Integer.parseInt(params.get(LENGTH_KEY));
		tested = new ArrayList<CellInfo>();
	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING HERE
	}

	@Override
	public void run(IParameterManager parameters)
	{
		boolean canDownload = false;
			IBytesTransferredReceiver downloadReceiver = null;
			if (!(Boolean)parameters.getParameter(Parameter.WIFI_CONNECTION_ATTEMPT)){
				Object connectedObj = parameters.getParameter(Parameter.CELL_CONNECTED);
				if ((Boolean)connectedObj){
					if ((Boolean)parameters.getParameter(Parameter.CELL_CONTINUE_TRANSFERRING)){
						// we will try to transfer only if we should continue. If not it is because an error happened with another transfer, and we should not run and let the cleaner command work
						if (ControllerServices.getInstance().getCell().isDataNetworkConnected()){
							downloadReceiver = new CellBytesTransferedReceiver(true);
							canDownload = true;						
						}
					}
				}
			}
			if (canDownload){
				CellInfo cell = ControllerServices.getInstance().getCell().getLastScannedCell();
				try {
					cell = ControllerServices.getInstance().getCell().getLastScannedCell();
					parameters.setParameter(Parameter.CELL_TRANSFERRING, true);					
					if (download(downloadReceiver, String.valueOf(cell.cid), cell.operatorName)){
						if (cell.equals(ControllerServices.getInstance().getCell().getLastScannedCell())){
							//We add the cell to the tested ones, only if it did not change in the middle of the download
							tested.add(cell);
						}
					}
				} catch (DownloadingInterruptedException e) {
					// If we are interrupted, just finish execution
					Log.d(getClass().getSimpleName()+"-INTERRUPTED", "++ "+e.getMessage(), e);
				} catch (DownloadingFailException e) {
					// If other failure happens, log it (probably connection was lost, but that is normal). Then, make sure you are disconnected
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We make sure that after then error, there will be no more up/downloads, so the cleaner will run
					parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, false);
				}  catch (TimeoutException e) {
					// If timeout was obtained, log and finish connection
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We make sure that after then error, there will be no more up/downloads, so the cleaner will run
					parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, false);
				}finally{
					parameters.setParameter(Parameter.CELL_TRANSFERRING, false);
				}
			}
	}
	
	public boolean download(IBytesTransferredReceiver rec, String ap, String network) throws DownloadingInterruptedException, DownloadingFailException, TimeoutException {
		return ControllerServices.getInstance().getWeb().downloadFile(host, filePath, rec, length, TimeoutConstants.CELL_DOWNLOAD_CONNECT_TIMEOUT, TimeoutConstants.CELL_DOWNLOAD_SOCKET_TIMEOUT, Timers.CELL_DOWNLOAD_RECEIVER_CALL_TIMER, ap, network);
	}

	public boolean wasTested(CellInfo cell) {
		return tested.contains(cell);
	}
	
	
}
