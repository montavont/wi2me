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

package telecom.wi2meCore.model.entities;

import telecom.wi2meCore.model.entities.Trace.TraceType;

import telecom.wi2meCore.model.Logger;


public class BytesperUid extends Trace{
	public static final String TABLE_NAME = "BytesperUid";
	public static final String SNIFF_SEQUENCE = "SniffSequence";
	public static final String UID = "Uid";
	public static final String TXBYTES = "Tx_Bytes";
	public static final String RXBYTES = "Rx_Bytes";

	private long sniffSequence;
	private int uid;
	private long txBytes;
	private long rxBytes;


	protected BytesperUid(Trace trace, long sniffSequence, int uid, long txBytes, long rxBytes){
		Trace.copy(trace, this);
		this.sniffSequence = sniffSequence;
		this.uid = uid;
		this.txBytes = txBytes;
		this.rxBytes = rxBytes;
	}

	public long getSniffSequence() {
		return sniffSequence;
	}

	public int getUid(){
		return uid;
	}

	public long getTxBytes() {
		return txBytes;
	}

	public long getRxBytes() {
		return rxBytes;
	}

	public static BytesperUid getNewBytesperUid(Trace trace, long sniffSequence, int uid, long txBytes, long rxBytes){
		return new BytesperUid(trace, sniffSequence, uid, txBytes, rxBytes);
	}

	private static final String SEPARATOR = "-";

	public String toString(){
		return super.toString() + "BYTES_PER_UID:" + sniffSequence + SEPARATOR + uid + SEPARATOR + txBytes + SEPARATOR + rxBytes;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.BYTES_PER_UID;
	}

}
