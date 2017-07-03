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

package telecom.wi2meCore.controller.services;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;

/**
 * Service managing the assets contained in the folder assets. 
 * Allows the program to load the assets files or to get their size.
 * Assets files : javascript plugins (manages the authentication on the community networks) and files to transfer (Wi2MeRecherche only)
 * @author XXX
 */
public class AssetServices implements IAssetServices{
	
	private AssetManager assetManager;
	
	/**Constructor loads the assets 
	 * @param context
	 */
	public AssetServices(Context context){
		assetManager = context.getResources().getAssets();
	}

	@Override
	public InputStream getStream(String filename) throws IOException {
		return assetManager.open(filename);
	}

	@Override
	public long getSize(String filename) throws IOException {
		return assetManager.openFd(filename).getLength();
	}

	@Override
	public String[] list(String path) throws IOException
	{
		return assetManager.list(path);
	}
}
