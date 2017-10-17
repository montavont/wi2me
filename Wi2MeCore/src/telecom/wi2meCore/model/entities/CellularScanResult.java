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

import java.util.List;
import telecom.wi2meCore.model.entities.Cell;
import telecom.wi2meCore.model.entities.Trace.TraceType;

public class CellularScanResult extends Trace{

	public static final String TABLE_NAME = "CellularScanResult";

	private List<Cell> results;

	protected CellularScanResult(Trace trace, List<Cell> results){
		Trace.copy(trace, this);
		this.results = results;
	}

	public List<Cell> getResults() {
		return results;
	}

	public static CellularScanResult getNewCellularScanResult(Trace trace, List<Cell> results){
		return new CellularScanResult(trace, results);
	}

	public String toString(){
		return super.toString() + "CELL_SCAN_RESULT:" + getCellsAsString();
	}

	private static final String CELL_SEPARATOR = "-";

	private String getCellsAsString() {
		String ret = "";
		for (Cell c : results){
			ret += c.toString() + CELL_SEPARATOR;
		}
		//Remove the last separator
		if (ret.endsWith(CELL_SEPARATOR))
			ret = ret.substring(0, ret.lastIndexOf(CELL_SEPARATOR));
		return ret;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.CELL_SCAN_RESULT;
	}

}
