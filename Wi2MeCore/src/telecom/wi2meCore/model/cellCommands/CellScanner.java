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

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.cell.CellInfo;
import telecom.wi2meCore.model.Logger;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.entities.Cell;
import telecom.wi2meCore.model.entities.CellularScanResult;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.parameters.Parameter;
import android.telephony.NeighboringCellInfo;
import android.util.Log;



/**
 * Wireless network command used to connect scan for cell towers.
 * @author XXX
 *
 */
public class CellScanner extends WirelessNetworkCommand{

	public CellScanner() {
		m_params = new HashMap<String, String>();
		m_subclassName = getClass().getCanonicalName();
	}
	public CellScanner(HashMap<String, String> params) { 
		m_params = params;
		m_subclassName = getClass().getCanonicalName();
		}


	@Override
	public void initializeCommand(IParameterManager parameters) {
	}

	@Override
	public void finalizeCommand(IParameterManager parameters) {
	}

	@Override
	public void run(IParameterManager parameters) {
		CellInfo cellInfo = null;
		List<Cell> scannedCells = new ArrayList<Cell>();

		try {
			parameters.setParameter(Parameter.CELL_SCANNING, true);
			cellInfo = readBeacon();
			parameters.setParameter(Parameter.CELL_SCANNING, false);
			Cell cell = Cell.getNewCellFromCellInfo(cellInfo);//current cell
			scannedCells.add(cell);
			for (NeighboringCellInfo n : cellInfo.neighbors){
				scannedCells.add(Cell.getNewCell(null, null, CellInfo.getNetworkType(n.getNetworkType()), null, n.getCid(), n.getLac(), CellInfo.getLeveldBm(n.getRssi()), false));
			}
			CellularScanResult result = CellularScanResult.getNewCellularScanResult(TraceManager.getTrace(), scannedCells);
			Logger.getInstance().log(result);

		} catch (InterruptedException e) {
			parameters.setParameter(Parameter.CELL_SCANNING, false);
			// if we are interrupted, we finish this scan
			Log.d(getClass().getSimpleName(), "++ "+"Scanning Interrupted", e);
		}
	}

	public CellInfo readBeacon() throws InterruptedException{
		return ControllerServices.getInstance().getCell().scan();
	}

}
