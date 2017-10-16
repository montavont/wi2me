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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap; 
import java.util.List;

import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.configuration.TimeoutConstants;
import telecom.wi2meCore.controller.configuration.Timers;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.cell.CellInfo;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.exceptions.UploadingFailException;
import telecom.wi2meCore.controller.services.exceptions.UploadingInterruptedException;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;

import android.os.Environment;
import android.util.Log;



/**
 * Wireless network command used to upload a file using the cellular network.
 * @author XXX
 *
 */
public class CellUploader extends WirelessNetworkCommand{

	private String server;
	private String script;
	private List<CellInfo> tested;
	private byte[] file;

	private static String DATA_DIR = "upload_files/";

	private static String SERVER_KEY = "server";
	private static String SCRIPT_KEY = "script";
	private static String SIZE_KEY = "size";


	public CellUploader(HashMap<String, String> params)
	{

		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		this.server = params.get(SERVER_KEY);
		this.script = params.get(SCRIPT_KEY);
		try
		{
			File uploadDataDir = new File(Environment.getExternalStorageDirectory() + ConfigurationManager.WI2ME_DIRECTORY + DATA_DIR);
			uploadDataDir.mkdir();

			RandomAccessFile f = new RandomAccessFile(Environment.getExternalStorageDirectory() +  ConfigurationManager.WI2ME_DIRECTORY + DATA_DIR + params.get(SIZE_KEY) , "rw");
			f.setLength(Integer.parseInt(params.get(SIZE_KEY)));
			file = Utils.RAFToByteArray(f);

		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ "+e.getMessage());
		}

	}

	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
		// DO NOTHING HERE
	}

	@Override
	public void run(IParameterManager parameters) {
		boolean canUpload = false;
			IBytesTransferredReceiver uploadReceiver = null;
			if (!(Boolean)parameters.getParameter(Parameter.WIFI_CONNECTION_ATTEMPT)){
				Object connectedObj = parameters.getParameter(Parameter.CELL_CONNECTED);
				if ((Boolean)connectedObj){
					if ((Boolean)parameters.getParameter(Parameter.CELL_CONTINUE_TRANSFERRING)){
						// we will try to transfer only if we should continue. If not it is because an error happened with another transfer, and we should not run and let the cleaner command work
						if (ControllerServices.getInstance().getCell().isDataNetworkConnected()){
							uploadReceiver = new CellBytesTransferedReceiver(false);
							canUpload = true;
						}
					}
				}
			}
			if (canUpload){
				CellInfo cell = ControllerServices.getInstance().getCell().getLastScannedCell();
				try {
					parameters.setParameter(Parameter.CELL_TRANSFERRING, true);
					if (upload(uploadReceiver)){ //If the file was completely uploaded
						if (cell.equals(ControllerServices.getInstance().getCell().getLastScannedCell())){
							//We add the cell to the tested ones, only if it did not change in the middle of the upload
							tested.add(cell);
						}
					}
				} catch (UploadingInterruptedException e) {
					// If we are interrupted, just finish execution
					Log.d(getClass().getSimpleName()+"-INTERRUPTED", "++ "+e.getMessage(), e);
				} catch (UploadingFailException e) {
					// If other failure happens, log it (probably connection was lost, but that is normal).
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We make sure that after then error, there will be no more up/downloads, so the cleaner will run
					parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, false);
				}  catch (TimeoutException e) {
					// If timeout was obtained, log and finish connection
					Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
					// We make sure that after then error, there will be no more up/downloads, so the cleaner will run
					parameters.setParameter(Parameter.CELL_CONTINUE_TRANSFERRING, false);
				} finally{
					parameters.setParameter(Parameter.CELL_TRANSFERRING, false);
				}
			}


	}

	public boolean upload(IBytesTransferredReceiver rec) throws UploadingInterruptedException, UploadingFailException, TimeoutException{
		return ControllerServices.getInstance().getWeb().uploadFile(server, script, rec, file, TimeoutConstants.CELL_UPLOAD_CONNECT_TIMEOUT, TimeoutConstants.CELL_UPLOAD_SOCKET_TIMEOUT, Timers.CELL_UPLOAD_RECEIVER_CALL_TIMER);
	}

	public boolean wasTested(CellInfo cell) {
		return tested.contains(cell);
	}
}
