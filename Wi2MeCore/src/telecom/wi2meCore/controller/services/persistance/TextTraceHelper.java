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

package telecom.wi2meCore.controller.services.persistance;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.model.entities.*;

public class TextTraceHelper implements ITraceDatabase
{

	public static final String TRACEFILE_NAME = "trace";
	private static final String CSV_SEP = "	";
	private static String storageFile;
	private static String packageName;

	private static TextTraceHelper tth = null;



	public enum TraceType{
		WIFI_SCAN_RESULT,
		WIFI_CONNECTION_EVENT,
		COMMUNITY_NETWORK_CONNECTION_EVENT,
		WIFI_CONNECTION_DATA,
		WIFI_SNIFFER_DATA,
		WIFI_CONNECTION_INFO,
		BYTES_PER_UID,
		CELL_SCAN_RESULT,
		CELL_CONNECTION_EVENT,
		CELL_CONNECTION_DATA,
		EXTERNAL_EVENT,
		WIFI_PING
	}


	private TextTraceHelper()
	{
	}

	public static void initialize(Context context)
	{
		packageName = context.getPackageName();
		storageFile = Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + TRACEFILE_NAME;
		tth = new TextTraceHelper();
	}

	public static TextTraceHelper getTextTraceHelper()
	{
		return tth;
	}

	public static boolean dataAvailable()
	{
		File f = new File(storageFile);
		return f.exists();
	}

	public void resetTables()
	{
		File f = new File(storageFile);
		f.delete();
		return;
	}
	public void closeDatabase()
	{
		return;
	}

	private String apToString(WifiAP ap)
	{
		String retval = "";

		retval += ap.getBSSID();
		retval += CSV_SEP;
		retval += ap.getSsid();
		retval += CSV_SEP;
		retval += ap.getLevel();
		retval += CSV_SEP;
		retval += ap.getChannel();
		retval += CSV_SEP;
		retval += ap.getLinkSpeed();
		retval += CSV_SEP;
		retval += ap.getCapabilities();

		return retval;
	}

	private String cellToString(Cell cell)
	{
		String retval = "";

		retval += cell.getOperatorName();
		retval += CSV_SEP;
		retval += cell.getOperator();
		retval += CSV_SEP;
		retval += cell.getNetworkType();
		retval += CSV_SEP;
		retval += cell.getPhoneType();
		retval += CSV_SEP;
		retval += cell.getCid();
		retval += CSV_SEP;
		retval += cell.getLac();
		retval += CSV_SEP;
		retval += cell.getLeveldBm();
		retval += CSV_SEP;
		retval += cell.isCurrent();
		return retval;
	}



	public void saveAllTraces(List<Trace> traces)
	{

		FileOutputStream traceOutputStream = null;
		OutputStreamWriter traceWriter = null;


		try
		{
			File file = new File(Environment.getDataDirectory() + "/data/" + packageName + "/databases/", TRACEFILE_NAME);
			//traceOutputStream = new FileOutputStream(Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + TRACEFILE_NAME);
			traceOutputStream = new FileOutputStream(file, true);
			traceWriter = new OutputStreamWriter(traceOutputStream);
		}
		catch (java.io.FileNotFoundException e)
		{
			Log.e(getClass().getSimpleName(), "++ " + "Error creating/accessing trace file : " + e.getMessage());
		}

		if (traceWriter != null)
		{
			for (Trace trace : traces)
			{

				String trace_str_prefix = "";
				ArrayList<String> trace_strings = new ArrayList<String>();
				trace_str_prefix += trace.getTimestamp();
				trace_str_prefix += CSV_SEP;
				/*trace_str_prefix += trace.getAltitude();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getLongitude();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getLatitude();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getAccuracy();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getSpeed();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getBearing();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getProvider();
				trace_str_prefix += CSV_SEP;*/
				trace_str_prefix += trace.getBatteryLevel();
				trace_str_prefix += CSV_SEP;
				trace_str_prefix += trace.getStoringType();

				switch (trace.getStoringType())
				{
					case WIFI_SCAN_RESULT:
						WifiScanResult wScanResult = (WifiScanResult) trace;
						for (WifiAP ap : wScanResult.getResults())
						{
							trace_strings.add(apToString(ap));
						}
						break;

					case WIFI_CONNECTION_EVENT:
						WifiConnectionEvent wConnectionEvent = (WifiConnectionEvent) trace;
						trace_strings.add(wConnectionEvent.getEvent() + CSV_SEP + apToString(wConnectionEvent.getConnectionTo()));
						break;
					case COMMUNITY_NETWORK_CONNECTION_EVENT:
						CommunityNetworkConnectionEvent cnConnectionEvent = (CommunityNetworkConnectionEvent) trace;
						trace_strings.add(
								cnConnectionEvent.getEvent()
								+ CSV_SEP
								+ cnConnectionEvent.getUsername()
								+ CSV_SEP
								+ apToString(cnConnectionEvent.getConnectedTo())
						);
						break;
					case WIFI_CONNECTION_DATA:
						WifiConnectionData wcd = (WifiConnectionData) trace;
						trace_strings.add(
								wcd.getConnectionData().getType()
								+ CSV_SEP
								+ wcd.getConnectionData().getIp()
								+ CSV_SEP
								+ wcd.getConnectionData().getBytesTransferred()
								+ CSV_SEP
								+ wcd.getConnectionData().getTotalBytes()
								+ CSV_SEP
								+ wcd.getConnectionData().getTxPackets()
								+ CSV_SEP
								+ wcd.getConnectionData().getRxPackets()
								+ CSV_SEP
								+ wcd.getConnectionData().getRetries()
								+ CSV_SEP
								+ apToString(wcd.getConnectedTo())
						);
						break;

					case WIFI_CONNECTION_INFO:
						WifiConnectionInfo wci = (WifiConnectionInfo) trace;
						trace_strings.add(
								wci.getSniffSequence()
								+ CSV_SEP
								+ wci.getConnectionInfo().getProtocol()
								+ CSV_SEP
								+ wci.getConnectionInfo().getLocalAdd()
								+ CSV_SEP
								+ wci.getConnectionInfo().getRemoteAdd()
								+ CSV_SEP
								+ wci.getConnectionInfo().getLocalPort()
								+ CSV_SEP
								+ wci.getConnectionInfo().getRemotePort()
								+ CSV_SEP
								+ wci.getConnectionInfo().getConnectionState().toString()
						);
						break;

					case WIFI_SNIFFER_DATA:
						WifiSnifferData wsd = (WifiSnifferData) trace;
						trace_strings.add(
								wsd.getSnifferData().getRxBytes()
								+ CSV_SEP
								+ wsd.getSnifferData().getTxBytes()
								+ CSV_SEP
								+ wsd.getSnifferData().getTxPackets()
								+ CSV_SEP
								+ wsd.getSnifferData().getRxPackets()
								+ CSV_SEP
								+ wsd.getSnifferData().getRetries()
								+ CSV_SEP
								+ wsd.getSniffSequence()
								+ CSV_SEP
								+ apToString(wsd.getConnectedTo())
						);
						break;

					case BYTES_PER_UID:
						BytesperUid bpu = (BytesperUid) trace;
						trace_strings.add(
								bpu.getSniffSequence()
								+ CSV_SEP
								+ bpu.getUid()
								+ CSV_SEP
								+ bpu.getTxBytes()
								+ CSV_SEP
								+ bpu.getRxBytes()
						);
						break;
					case WIFI_PING:
						WifiPing ping = (WifiPing) trace;
						trace_strings.add(
								ping.getPingedIp()
								+ CSV_SEP
								+ ping.getPacketsSent()
								+ CSV_SEP
								+ ping.getPacketsReceived()
								+ CSV_SEP
								+ ping.getRttMin()
								+ CSV_SEP
								+ ping.getRttMax()
								+ CSV_SEP
								+ ping.getRttAvg()
								+ CSV_SEP
								+ ping.getRttMdev()
								+ CSV_SEP
								+ apToString(ping.getConnectionTo())
						);
						break;
					case CELL_CONNECTION_EVENT:
						CellularConnectionEvent cConnectionEvent = (CellularConnectionEvent) trace;
						trace_strings.add(
								cConnectionEvent.getEvent()
								+ CSV_SEP
								+ cellToString(cConnectionEvent.getConnectionTo())
						);
						break;
					case CELL_CONNECTION_DATA:
						CellularConnectionData ccd = (CellularConnectionData) trace;
						trace_strings.add(
							ccd.getConnectionData().getType()
							+ CSV_SEP
							+ ccd.getConnectionData().getIp()
							+ CSV_SEP
							+ ccd.getConnectionData().getBytesTransferred()
							+ CSV_SEP
							+ ccd.getConnectionData().getTotalBytes()
							+ CSV_SEP
							+ ccd.getConnectionData().getTxPackets()
							+ CSV_SEP
							+ ccd.getConnectionData().getRxPackets()
							+ CSV_SEP
							+ ccd.getConnectionData().getRetries()
							+ CSV_SEP
							+ cellToString(ccd.getConnectedTo())
						);
						break;
					case CELL_SCAN_RESULT:
						CellularScanResult cScanResult = (CellularScanResult) trace;
						//Now we save the scan results in the corresponding table, using the Id of the trace
						for (Cell c : cScanResult.getResults())
						{
							trace_strings.add(
								cellToString(c)
							);
						}
						break;
					case EXTERNAL_EVENT:
						ExternalEvent externalEvent = (ExternalEvent) trace;
						trace_strings.add(
							externalEvent.getEvent()
						);
						break;
					case LOCATION_EVENT:
						Location location = ((LocationEvent) trace).getLocation();
						trace_strings.add(
	           				location.getProvider()
							+ CSV_SEP
							+ location.getAltitude()
							+ CSV_SEP
	 				        + location.getLatitude()
							+ CSV_SEP
			    	       	+ location.getLongitude()
							+ CSV_SEP
	        				+ location.getSpeed()
							+ CSV_SEP
					        + location.getAccuracy()
							+ CSV_SEP
					        + location.getBearing()
						);
				}

				try
				{
					for (String line : trace_strings)
					{
						traceWriter.write(trace_str_prefix);
						traceWriter.write(CSV_SEP);
						traceWriter.write(line);
						traceWriter.write("\n");
					}
				}
				catch (java.io.IOException e)
				{
					Log.e(getClass().getSimpleName(), "++ " + "Error writing trace : "+e.getMessage());
				}
			}
		}

		try
		{
			if (traceOutputStream != null)
			{
				traceWriter.close();
				traceOutputStream.close();
			}
		}
		catch (java.io.IOException e)
		{

			Log.e(getClass().getSimpleName(), "++ " + "Error closing trace file: "+e.getMessage());
		}

		return;
	}

}
