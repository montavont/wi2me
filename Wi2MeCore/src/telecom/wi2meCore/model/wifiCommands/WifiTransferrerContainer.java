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

import java.util.List;


public class WifiTransferrerContainer {
	
	private List<WifiUploader> uploaders;
	private List<WifiDownloader> downloaders;
	private List<Pinger> pingers;
	
	public WifiTransferrerContainer(List<WifiUploader> uploaders, List<WifiDownloader> downloaders, List<Pinger> pingers) {
		this.uploaders = uploaders;
		this.downloaders = downloaders;
		this.pingers = pingers;
	}
	
}
