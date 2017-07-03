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

import java.util.ArrayList;
import java.util.List;
import telecom.wi2meCore.model.Flag;
import telecom.wi2meCore.model.parameters.IParameterManager;
import telecom.wi2meCore.model.WirelessNetworkCommand;
import telecom.wi2meCore.model.CleanerCommand;
import telecom.wi2meCore.model.Logger;


import android.util.Log;


public class WirelessNetworkCommandLooper implements IWirelessNetworkCommandLooper{
	
	private List<WirelessNetworkCommand> commands;
	private boolean running = true;
	
	/*public WirelessNetworkCommandLooper(CleanerCommand cleaner){
		commands = new ArrayList<WirelessNetworkCommand>();
		commands.add(cleaner);
	}*/

	public WirelessNetworkCommandLooper()
	{
		commands = new ArrayList<WirelessNetworkCommand>();
	}

	@Override
	public void initializeCommands(IParameterManager parameters) {
		for(WirelessNetworkCommand c : commands){
			c.initializeCommand(parameters);
		}
	}

	@Override
	public void finalizeCommands(IParameterManager parameters)
	{
		for(WirelessNetworkCommand c : commands){
			c.finalizeCommand(parameters);
		}
	}

	@Override
	public void addCommand(WirelessNetworkCommand command)
	{
		commands.add(command);
	}

	@Override
	public void breakLoop()
	{
		this.running = false;
	}

	//@Override
	@Override
	public void loop(IParameterManager parameters)
	{
		int index = 0;
		while (running)
		{
			commands.get(index).run(parameters);
			++index;
			index = index % commands.size();//this restarts the index (in 0) when it reaches the last possible index 

		}
		//we flush it before finishing cause it probably has some logs left to persist
		Logger.getInstance().flush();
	}

}
