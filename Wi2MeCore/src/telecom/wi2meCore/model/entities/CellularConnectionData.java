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
import telecom.wi2meCore.model.entities.Cell;


public class CellularConnectionData extends Trace{

	public static final String TABLE_NAME = "CellularConnectionData";

	private Cell connectedTo;
	private ConnectionData connectionData;

	protected CellularConnectionData(Trace trace, Cell connectedTo, ConnectionData connectionData){
		Trace.copy(trace, this);
		this.connectedTo = connectedTo;
		this.connectionData = connectionData;
	}

	public Cell getConnectedTo() {
		return connectedTo;
	}

	public ConnectionData getConnectionData() {
		return connectionData;
	}

	public static CellularConnectionData getNewCellularConnectionData(Trace trace, Cell connectedTo, ConnectionData connectionData){
		return new CellularConnectionData(trace, connectedTo, connectionData);
	}

	private static final String SEPARATOR = "-";

	public String toString(){
		return super.toString() + "CELL_CONNECTION_DATA:" + connectionData.toString() + SEPARATOR + connectedTo.toString();
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.CELL_CONNECTION_DATA;
	}

}
