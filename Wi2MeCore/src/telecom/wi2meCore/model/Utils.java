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

package telecom.wi2meCore.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile; 
import java.nio.ByteOrder;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;


public class Utils
{
	
	public static final String TYPE_START = "START";
	public static final String TYPE_DOWNLOAD = "IN";
	public static final String TYPE_UPLOAD = "OUT";
	public static final String TYPE_SNIFF = "SNIFF";
	
	public static byte[] inStreamToByteArray(InputStream inStream)
	{
		int bytesRead = 0;
		final int BUFFER_SIZE=256;
		byte[] buffer = new byte[BUFFER_SIZE];
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			while ((bytesRead = inStream.read(buffer)) != -1){
				outStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			Log.e("Error converting inputstream to byte array", "++ "+e.getMessage(), e);
		}
		
		return outStream.toByteArray();
	}
	
	public static byte[] RAFToByteArray(RandomAccessFile raf)
	{
		int bytesRead = 0;
		final int BUFFER_SIZE=256;
		byte[] buffer = new byte[BUFFER_SIZE];
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try {
			while ((bytesRead = raf.read(buffer)) != -1){
				outStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			Log.e("Error converting inputstream to byte array", "++ "+e.getMessage(), e);
		}
		
		return outStream.toByteArray();
	}
	
	public static String intToIp(long i) {
		if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
			return ((i >> 24 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ( i        & 0xFF);
		else
			return ( i        & 0xFF) + "." +
				((i >>  8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +
				((i >> 24 ) & 0xFF);
	}

	public static String string2IPv6(String originAdd){
		return originAdd.substring(0, 4) + ":" 
				+ originAdd.substring(4, 8) + ":"
				+ originAdd.substring(8, 12) + ":"
				+ originAdd.substring(12, 16) + ":"
				+ originAdd.substring(16, 20) + ":"
				+ originAdd.substring(20, 24) + ":"
				+ originAdd.substring(24, 28) + ":"
				+ originAdd.substring(28, 32);
	}

	/**
	* Convert a IPv4 address from an integer to an InetAddress.
	* @param hostAddress an int corresponding to the IPv4 address in network byte order
	* Shamelessly copied from SDK's NetworkUtils. Replace if one day made publicly available
	*/
	public static InetAddress intToInetAddress(int hostAddress) {
	    byte[] addressBytes = { (byte)(0xff & hostAddress),
	                            (byte)(0xff & (hostAddress >> 8)),
	                            (byte)(0xff & (hostAddress >> 16)),
	                            (byte)(0xff & (hostAddress >> 24)) };

	    try {
	       return InetAddress.getByAddress(addressBytes);
	    } catch (UnknownHostException e) {
	       throw new AssertionError();
	    }
	}
	

}
