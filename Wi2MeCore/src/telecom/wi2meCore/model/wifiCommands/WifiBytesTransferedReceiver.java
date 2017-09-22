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

package telecom.wi2meCore.model.wifiCommands;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays; 
import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List; 
import java.util.ListIterator;
import java.util.Set;


import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.web.IBytesTransferredReceiver;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.Utils;
import telecom.wi2meCore.model.entities.ConnectionData;
import telecom.wi2meCore.model.entities.ConnectionInfo;
import telecom.wi2meCore.model.entities.ConnectionInfo.ConnectionState;
import telecom.wi2meCore.model.entities.WifiAP;
import telecom.wi2meCore.model.entities.WifiConnectionData;
import telecom.wi2meCore.model.entities.SnifferData;
import telecom.wi2meCore.model.entities.WifiConnectionInfo;
import telecom.wi2meCore.model.entities.WifiSnifferData;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.TrafficStats;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;



/**
 * This class offers WifiUploader, WifiDownloader and WifiStayConnected the methods to examinate some local files to get the traffic information
 * @author XXX
 *
 */
public class WifiBytesTransferedReceiver implements IBytesTransferredReceiver{
	private static final String SEPARATOR = "-";
	private String type;
	private ScanResult connectedTo;
	private int byteCounter;
	private static ArrayList<ConnectionInfo> listCI = null;
	private static HashMap<Integer,Long> uidTxHash = null;
	private static HashMap<Integer,Long> uidRxHash = null;
	private static long sniffSequence = 0;
	private ListIterator<ConnectionInfo> ite;
	private Set<Integer> uidSet;
	private Iterator<Integer> uidIte;
	private IParameterManager parameters = null;


	private final Semaphore mSema = new Semaphore(1, true);

	private static HashMap<String, Integer> lastValues;
	private static HashMap<String, RandomAccessFile> debugFSFiles;
	
	/** 
	 * Reset the sniffSequece when reseting database
	 */
	public static void resetSniffSequence()
	{
		sniffSequence = 0;
	}
	
	/** 
	 * Constructor 
	 * @param boolean  download or upload
	 * @param ScanResult  current connected AP
	 */
	//public WifiBytesTransferedReceiver(ScanResult connectedTo, String type, WifiInfo wifiInfo)
	public WifiBytesTransferedReceiver(String type, IParameterManager parameters)
	{
		byteCounter = 0;
		this.parameters = parameters;
		this.connectedTo = (ScanResult) parameters.getParameter(Parameter.WIFI_CONNECTED_TO_AP);

		this.lastValues = new HashMap<String, Integer>();
		this.debugFSFiles = new HashMap<String, RandomAccessFile>();
		this.type = type;
		this.uidSet = new HashSet<Integer>();
		
		if (this.type.equals(Utils.TYPE_SNIFF))
		{
			if (listCI == null)
			{
				this.listCI = new ArrayList<ConnectionInfo> ();
			}
			if (uidTxHash == null) this.uidTxHash = new HashMap<Integer,Long> ();
			if (uidRxHash == null) this.uidRxHash = new HashMap<Integer,Long> ();
			sniffSequence++;
		}
	}
	
	/** 
	 * Read the local file (TCP/TCP6/UDP/UDP6) to get level 3 traffic information,
	 * trace the new and updated information
	 * @param String  download or upload
	 * @param ScanResult  current connected AP
	 */
	private void readConnectionFile(String fileName, String protocol) throws IOException{
		String line = "";
		String localAdd = "";
		String remoteAdd = "";
		ConnectionState cs = null;
		int uid = -1;
		
		FileReader tcp = new FileReader(fileName);
		LineNumberReader lnr = new LineNumberReader(tcp);
	    
		line = lnr.readLine();
		while((line = lnr.readLine()) != null)
		{
			String[] pieces = line.trim().split(" ");				
			
			if (protocol.contains("6")) {			
				localAdd = Utils.string2IPv6(pieces[1].split(":")[0]);
				remoteAdd = Utils.string2IPv6(pieces[2].split(":")[0]);
			}
			else
			{
				localAdd = Utils.intToIp(Long.parseLong(pieces[1].split(":")[0], 16));
				remoteAdd = Utils.intToIp(Long.parseLong(pieces[2].split(":")[0], 16));
			}
			if (localAdd.equals("127.0.0.1") || localAdd.equals("0000:0000:0000:0000:0000:0000:0000:0000")) continue;
			
			int localPort = Integer.parseInt(pieces[1].split(":")[1],16);
			int remotePort = Integer.parseInt(pieces[2].split(":")[1],16);
			
			if (pieces[7].length() != 0) uid = Integer.parseInt(pieces[7]);
			if (pieces[8].length() != 0) uid = Integer.parseInt(pieces[8]);
			if (uid != -1 && !uidSet.contains(uid)) uidSet.add(uid);
			
			switch (Integer.parseInt(pieces[3], 16)){
				case 1:
					cs = ConnectionState.ESTABLISHED;
					break;
				case 2:
					cs = ConnectionState.SYN_SENT;
					break;
				case 3:
					cs = ConnectionState.SYN_RECV;
					break;
				case 4:
					cs = ConnectionState.FIN_WAIT1;
					break;
				case 5:
					cs = ConnectionState.FIN_WAIT2;
					break;
				case 6:
					cs = ConnectionState.TIME_WAIT;
					break;
				case 7:
					cs = ConnectionState.CLOSE;
					break;
				case 8:
					cs = ConnectionState.CLOSE_WAIT;
					break;
				case 9:
					cs = ConnectionState.LAST_ACK;
					break;
				case 10:
					cs = ConnectionState.LISTEN;
					break;
				case 11:
					cs = ConnectionState.CLOSING;
					break;
				case 0:
				case 255:
				default:
					cs = ConnectionState.UNKNOWN;
					break;
			}

			ConnectionInfo ci = ConnectionInfo.getNewConnectionInfo(protocol, localAdd, remoteAdd, localPort, remotePort, cs, uid);
			ite = listCI.listIterator();
			while(ite.hasNext())
			{
				ConnectionInfo element = ite.next();
				if (element.equalsExceptState(ci))
				{
			    		element.setUpdated();
					ci.setUpdated();
					if (!element.getConnectionState().equals(ci.getConnectionState()))	
					{
			    			ite.set(ci);			    	    	  			
				  		WifiConnectionInfo wci = WifiConnectionInfo.getNewWifiConnectionInfo(TraceManager.getTrace(), sniffSequence, ci);			
				  		Logger.getInstance().log(wci);			  			
			  		}
			    	}
			}	
			if (!ci.getUpdated())
			{
				ci.setUpdated();
				listCI.add(ci);
				WifiConnectionInfo wci = WifiConnectionInfo.getNewWifiConnectionInfo(TraceManager.getTrace(), sniffSequence, ci);			
		  		Logger.getInstance().log(wci);	
			}
		}		
		lnr.close();
		tcp.close();
	}
	
	/** 
	 * Examinate several local files that contain traffic information, both network level and application level
	 * generate the traces with the wifi information
	 * @param IParameterManager  parameter "ALLOW_TRACE_CONNECTIONS" to give the permission of trace
	 * @throws IOException throw exception when encounter the problem of reading file 
	*/
	public void sniffer() throws IOException
	{

		boolean logData = false;
		boolean allowTraceConnections = false;
		int retries = 0;
		int rxp = 0;
		int txp = 0;
		int rxb = 0;
		int txb = 0;
		String iface = "";
		String debugFolder= "/sys/class/net/";
		
		allowTraceConnections = (Boolean) this.parameters.getParameter(Parameter.ALLOW_TRACE_CONNECTIONS);

		WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo(); 
		if (info != null)
		{
			List<String> ifaces = (List<String>) this.parameters.getParameter(Parameter.MONITORED_INTERFACES);
			List<String> debugfsEntries = Arrays.asList( ConfigurationManager.RXP_FILE, ConfigurationManager.TXP_FILE, ConfigurationManager.RXB_FILE, ConfigurationManager.TXB_FILE);
			WifiAP connectionToWifiAP = WifiAP.getNewWifiAP(info.getBSSID(), info.getSSID(), info.getRssi(), WifiAP.frequencyToChannel(connectedTo.frequency), connectedTo.capabilities, info.getLinkSpeed());
			
			for (int i = 0; i < ifaces.size(); i++)
			{
				logData = false;

				iface = ifaces.get(i);
				retries = 0;
				rxp = 0;
				txp = 0;
				rxb = 0;
				txb = 0;

				try
				{
					for (String f : debugfsEntries)
					{
						if (! debugFSFiles.containsKey(iface+f))
						{
							debugFSFiles.put(iface+f, getFile(debugFolder  + iface + f));
						}
					}

					try
					{
						mSema.acquire();
						rxp = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.RXP_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.RXP_FILE, rxp);

						txp = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.TXP_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.TXP_FILE, txp);

						rxb = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.RXB_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.RXB_FILE, rxb);

						txb = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.TXB_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.TXB_FILE, txb);


						for (String f : debugfsEntries)
						{
							debugFSFiles.get(iface+f).seek(0);
						}
						mSema.release();
					}
					catch(InterruptedException e)
					{
						Log.e("Wifi", "++ Unable to obtain debugfs semaphore" + e.toString());
					}
				}
				catch (IOException e)
				{
					Log.e("Wifi", "++ Error reading in debugfs" + e.toString());
					ifaces.remove(iface);
				}
				if (logData)
				{
					SnifferData snifferData = SnifferData.getNewSnifferData(txb, rxb, txp, rxp, retries, Utils.intToIp(info.getIpAddress()));
					WifiSnifferData wifiSnifferData = WifiSnifferData.getNewWifiSnifferData(TraceManager.getTrace(), connectionToWifiAP, snifferData, i);
					Logger.getInstance().log(wifiSnifferData);
				}

			}

		}
		return;
	}
	
	@Override
	public void receiveTransferredBytes(int bytes, long totalBytes) {
		receiveTransferredBytes(bytes, totalBytes, "");
	}

	@Override
	public void receiveTransferredBytes(int bytes, long totalBytes, String eventDescription)
	{
		int tx = 0;
		int rx = 0;
		int retries = 0;
		String debugFolder= "/sys/class/net/";

		boolean logData = false;

		if (eventDescription != "")
		{
			eventDescription = SEPARATOR + eventDescription;
		}

		if (bytes != 0 || eventDescription.contains("START") || eventDescription.contains("TRANSFERRING"))
		{
		
			List<String> ifaces = (ArrayList<String>)this.parameters.getParameter(Parameter.MONITORED_INTERFACES);

			List<String> debugfsEntries = Arrays.asList( ConfigurationManager.RXP_FILE, ConfigurationManager.TXP_FILE);
			for (String iface : ifaces)
			{
					
				logData = false;
				try
				{

					try
					{
						mSema.acquire();
						for (String f : debugfsEntries)
						{
							if (! debugFSFiles.containsKey(iface + f))
							{
								debugFSFiles.put(iface+f, getFile(debugFolder + iface + f));
							}
						}
	
						rx = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.RXP_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.RXP_FILE, rx);

						tx = Integer.parseInt(debugFSFiles.get(iface + ConfigurationManager.TXP_FILE).readLine());
						logData = logData || isNewData(iface + ConfigurationManager.TXP_FILE, tx);

						for (String f : debugfsEntries)
						{
							debugFSFiles.get(iface+f).seek(0);
						}
						mSema.release();
					}
					catch(InterruptedException e)
					{
						Log.e("Wifi", "++ Unable to obtain debugfs semaphore" + e.toString());
					}
				}
				catch (IOException e)
				{
					Log.e("Wifi", "++ Error messing with debugfs" + e.toString());
				}

				byteCounter += bytes;
				WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
				if (info != null && logData)
				{
					int ip = info.getIpAddress();

					WifiConnectionData connectionData = WifiConnectionData.getNewWifiConnectionData(
								TraceManager.getTrace(),
								WifiAP.getNewWifiAP(info.getBSSID(),
								info.getSSID(),
								info.getRssi(),
								WifiAP.frequencyToChannel(connectedTo.frequency),
								connectedTo.capabilities,
								info.getLinkSpeed()),
								ConnectionData.getNewConnectionData(Utils.intToIp(ip),
													byteCounter,
													(int)totalBytes,
													type+eventDescription+"-"+iface,
													tx,
													rx,
													(int) retries)
					);			

					Logger.getInstance().log(connectionData);
				}
			}
		}
	}

	@Override
	public int getTransferredBytes() {
		return byteCounter;
	}
	/** 
	 * Get access of local file
	 * @param String file path
	*/
	private static RandomAccessFile getFile(String filename) throws IOException
	{
		File f = new File(filename);
		return new RandomAccessFile(f, "r");
	}
	/** 
	 * Read local file
	 * @param String file path
	*/
	private String readBytes(String file)
	{
		String retval = "0";
		long now = Calendar.getInstance().getTimeInMillis();
		RandomAccessFile raf = null;
		try
		{
			raf = getFile(file);
			retval = raf.readLine();
		}
		catch (Exception e)
		{
			Log.w("Wifi", "++ Error reading file: "+e.toString());
		}
		finally
		{
			if (raf != null)
			{
				try
				{
					raf.close();
				}
				catch (IOException e)
				{
					Log.w("Wifi", "++ Error closing file: "+e.toString());
				}
			}
		}
		return retval;
	}
	

	private boolean isNewData(String file, Integer value)
	{
		boolean retval = false;

		if (! lastValues.containsKey(file))
		{
			retval = true;
		}
		else
		{
			retval = !value.equals(lastValues.get(file));
		}

		if (retval)
		{
			lastValues.put(file, value);
		}
	
		return retval;
	}

		
}

