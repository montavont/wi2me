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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.Semaphore;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.entities.Cell;
import telecom.wi2meCore.model.entities.CellularConnectionData;
import telecom.wi2meCore.model.entities.ConnectionData;
import android.util.Log;

public class CellBytesTransferedReceiver implements IBytesTransferredReceiver {

	private static final String SEPARATOR = "-";
	private String type;
	private int byteCounter;
	private HashMap<Long, Integer> mDatapoints = new HashMap<Long, Integer>();
	private int mAverageWindowSize = 10000; // 10 seconds
	private Semaphore mSema = new Semaphore(1, true);

	public CellBytesTransferedReceiver(boolean download){
		byteCounter = 0;
		if (download){
			type = Utils.TYPE_DOWNLOAD;
		}else{
			type = Utils.TYPE_UPLOAD;
		}
	}

	public String getLocalIpAddress() {
		String ret = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ret = inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException ex) {
            Log.e(getClass().getSimpleName(), "++ "+ex.toString());
        }
        return ret;
    }

	@Override
	public void receiveTransferredBytes(int bytes, long totalBytes) {
		receiveTransferredBytes(bytes, totalBytes, "");
	}

	@Override
	public void receiveTransferredBytes(int bytes, long totalBytes,
			String eventDescription) {

		int tx = 0; // Only for WiFi
		int rx = 0;
		int retries = 0;

		if (eventDescription != ""){
			eventDescription = SEPARATOR + eventDescription;
		}
		byteCounter += bytes;
		Cell currentCell = Cell.getNewCellFromCellInfo(ControllerServices.getInstance().getCell().getLastScannedCell());
		CellularConnectionData connectionData = CellularConnectionData.getNewCellularConnectionData(TraceManager.getTrace(), currentCell, ConnectionData.getNewConnectionData(getLocalIpAddress(), byteCounter, (int) totalBytes, type+eventDescription, tx, rx, retries));
		Logger.getInstance().log(connectionData);

		addDatapoint(bytes);

	}

	@Override
	public int getTransferredBytes() {
		return byteCounter;
	}

	private static RandomAccessFile getFile(String filename) throws IOException {
		File f = new File(filename);
		return new RandomAccessFile(f, "r");
	}

	private static String readBytes(String file){
		RandomAccessFile raf = null;
		try {
			raf = getFile(file);
			return raf.readLine();
		} catch (Exception e) {
			return "0";
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void addDatapoint(int bytes)
	{
		long time = Calendar.getInstance().getTimeInMillis();
		Collection<Long> indexesForRemoval = new ArrayList<Long>();

		// Remove datapoints older than mAverageWindowSize
		try
		{
			mSema.acquire();
			for (Long ts : mDatapoints.keySet())
			{
				if (time - ts > mAverageWindowSize)
				{
					indexesForRemoval.add(ts);
				}
			}
			for (long index : indexesForRemoval)
			{
				mDatapoints.remove(index);
			}

			// Add datapoint to current list
			mDatapoints.put(time, bytes);
			mSema.release();
		}
		catch(InterruptedException e)
		{
            Log.e(getClass().getSimpleName(), "++ "+ e.toString());
		}
	}

	public float getAverageThroughput()
	{
		float totalBytes = 0;
		//Average over current mDatapoints keys
		try{
			mSema.acquire();
			for (int bytes : mDatapoints.values())
			{
				totalBytes+= bytes;
			}
			mSema.release();
		}
		catch(InterruptedException e)
		{
            Log.e(getClass().getSimpleName(), "++ "+ e.toString());
		}
		return totalBytes  * 1000 / mAverageWindowSize;
	}

}
