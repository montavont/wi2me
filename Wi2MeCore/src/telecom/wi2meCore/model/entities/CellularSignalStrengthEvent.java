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

import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthCdma;

public class CellularSignalStrengthEvent extends Trace{

	public static final String TABLE_NAME = "CellularSignalStrengthEvent";

	private CellSignalStrength signalStrength;
	private int asulevel = 0;
	private int cqi = 0;
	private int dbm = 0;
	private int level = 0;
	private int rsrp = 0;
	private int rsrq = 0;
	private int rssnr = 0;
	private int timingadvance = 0;

	private Cell connectedTo;

	protected CellularSignalStrengthEvent(Trace trace, Cell connectedTo, CellSignalStrength signalStrength){
		Trace.copy(trace, this);
		this.signalStrength = signalStrength;
		if (signalStrength instanceof CellSignalStrengthLte)
		{
			CellSignalStrengthLte cellStrength = (CellSignalStrengthLte) signalStrength;
			this.asulevel =  cellStrength.getAsuLevel();
			this.cqi = cellStrength.getCqi();
			this.dbm = cellStrength.getDbm();
			this.level = cellStrength.getLevel();
			this.rsrp = cellStrength.getRsrp();
			this.rsrq = cellStrength.getRsrq();
			this.rssnr = cellStrength.getRssnr();
			this.timingadvance = cellStrength.getTimingAdvance();
		}
		this.connectedTo = connectedTo;
		// Only LTE supported for now
	}

	public static CellularSignalStrengthEvent getNewCellularSignalStrengthEvent(Trace trace, Cell connectedTo, CellSignalStrength signalStrength){
		return new CellularSignalStrengthEvent(trace, connectedTo, signalStrength);
	}

	private static final String AP_SEPARATOR = "-";

	public String toString(){
		return super.toString() + "CELL_SIGNAL_EVENT:" + AP_SEPARATOR + signalStrength.toString();
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.CELL_SIGNAL_EVENT;
	}

	public Cell getConnectedTo() {
		return connectedTo;
	}

	public int getAsuLevel()
	{
		return this.asulevel;
	}
	public int getCqi()
	{
		return this.cqi;
	}
	public int getDbm()
	{
		return this.dbm;
	}
	public int getLevel()
	{
		return this.level;
	}
	public int getRsrp()
	{
		return this.rsrp;
	}
	public int getRsrq()
	{
		return this.rsrq;
	}
	public int getRssnr()
	{
		return this.rssnr;
	}
	public int getTimingAdvance()
	{
		return this.timingadvance;
	}

}
