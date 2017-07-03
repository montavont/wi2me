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

package telecom.wi2meCore.controller.services.web;

import java.io.InputStream;
import telecom.wi2meCore.controller.services.exceptions.DownloadingFailException;
import telecom.wi2meCore.controller.services.exceptions.DownloadingInterruptedException;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.exceptions.UploadingFailException;
import telecom.wi2meCore.controller.services.exceptions.UploadingInterruptedException;
import telecom.wi2meCore.controller.services.web.WebService;

public interface IWebService {
	/**
	 * Performs an HTTP request to check whether the application is online or not.
	 * HttpRequest is preferred to a ping because some networks don't allow ICMP messages.
	 *
	 * @param route optionnal parameter to explicitely use the wifi or cellular interface
	 * @return True if successful HttpRequest, false otherwise.
	 */
	boolean isOnline(WebService.Route route);
	boolean isOnline();

	void downloadWebPage(String url, WebService.Route route, int numConn, IBytesTransferredReceiver receiver);
	void downloadWebPage(String url, WebService.Route route, int numConn, IBytesTransferredReceiver receiver, String tag);
	void downloadWebPageWSpdy(String url, WebService.Route route, IBytesTransferredReceiver receiver, String tag);

	/**
	 * 
	 * @param host
	 * @param uploadScript
	 * @param receiver
	 * @param bytes
	 * @param timeoutConnection
	 * @param timeoutSocket
	 * @return
	 * @throws UploadingInterruptedException
	 * @throws UploadingFailException
	 * @throws TimeoutException
	 */
	public boolean uploadFile(String host, String uploadScript, IBytesTransferredReceiver receiver, byte[] bytes, int timeoutConnection, int timeoutSocket, int receiverCallTimer) throws UploadingInterruptedException, UploadingFailException, TimeoutException;
	
	public boolean downloadFile(String host, String filePath, IBytesTransferredReceiver receiver, long length, int timeoutConnection, int timeoutSocket, int receiverCallTimer, String ap, String network) throws DownloadingInterruptedException, DownloadingFailException, TimeoutException;
}
