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

import java.util.List;

import telecom.wi2meCore.controller.services.cell.CellInfo;

public class CellTransferrerContainer {
	private List<CellUploader> uploaders;
	private List<CellDownloader> downloaders;
	
	public CellTransferrerContainer(List<CellUploader> uploaders, List<CellDownloader> downloaders) {
		this.uploaders = uploaders;
		this.downloaders = downloaders;
	}
	
	public boolean wasCompletelyTested(CellInfo cell){
		//If it was not tested by any of these transferrers, return false
		for (CellUploader u : uploaders){
			if (!u.wasTested(cell)){
				return false;
			}
		}
		for (CellDownloader d: downloaders){
			if (!d.wasTested(cell)){
				return false;
			}
		}
		return true;
	}

}
