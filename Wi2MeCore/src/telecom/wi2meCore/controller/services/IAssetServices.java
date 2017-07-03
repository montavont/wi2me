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

/**Interface to implement for a Service managing the assets. 
 * @author XXX
 */
public interface IAssetServices {
	
	/**Gives the stream of data contained in the file of the given name. 
	 * @param filename String
	 * @return InputStream of the file
	 * @throws IOException
	 */
	InputStream getStream(String filename) throws IOException;
	
	/**Returns the length of the file of the given name. 
	 * @param filename String
	 * @return long: size of the file
	 * @throws IOException
	 */
	long getSize(String filename) throws IOException;

	/**Lists the files in an asset directory. 
	 * @param path String path to the directory
	 * @return String[]: list of path in direcctory
	 * @throws IOException
	 */
	String[] list(String path) throws IOException;

}
