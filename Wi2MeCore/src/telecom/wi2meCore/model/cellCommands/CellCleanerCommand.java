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

import android.os.Build;

import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.model.CleanerCommand;
import telecom.wi2meCore.model.parameters.IParameterManager;

import java.util.HashMap; 

/**
 * Wireless network command used to connect to a cell tower.
 * Forces the cell Service to perform a disconnection
 * @author XXX
 *
 */
public class CellCleanerCommand extends CleanerCommand
{

	public CellCleanerCommand() { }
	public CellCleanerCommand(HashMap<String, String> params) { }


	public void clean(IParameterManager parameters)
	{
		/*---CLEAN EVERYTHING TO START AGAIN---*/
		//the only thing we need is to ensure that the cellular connection is over
		ControllerServices.getInstance().getCell().disconnectOrDie();
		/*------*/
	}
}
