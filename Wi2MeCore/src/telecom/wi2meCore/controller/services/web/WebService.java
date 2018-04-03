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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.exceptions.DownloadingFailException;
import telecom.wi2meCore.controller.services.exceptions.DownloadingInterruptedException;
import telecom.wi2meCore.controller.services.exceptions.TimeoutException;
import telecom.wi2meCore.controller.services.exceptions.UploadingFailException;
import telecom.wi2meCore.controller.services.exceptions.UploadingInterruptedException;
import telecom.wi2meCore.model.Utils;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;




import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.OkHttpClient;


public class WebService implements IWebService {

	private static final String JAVASCRIPT_ERROR = "ABORT - Javascript error.";
	private static final String WEB_VIEW_ERROR = "ABORT - Webview error.";

	private static final int PACKET_SIZE = 1500;

	private static final String START = "START";
	private static final String TRANSFERRING = "TRANSFERRING";
	private static final String UPDATE = "UPDATE";
	private static final String FINISH_COMPLETE = "FINISH_COMPLETE";
	private static final String FINISH_INCOMPLETE = "FINISH_INCOMPLETE";
	private static final String FINISH_INTERRUPTED = "FINISH_INTERRUPTED";
	private static final String FINISH_ERROR = "FINISH_ERROR";
	private static final String SOCKET_TIMEOUT = "SOCKET_TIMEOUT";
	private static final String CONNECT_TIMEOUT = "CONNECTION_TIMEOUT";

	private String urlRef;
	private long uploadFileSize;
	private IBytesTransferredReceiver transferReceiver = null;
	private long totalBytesToTransfer;
	private Random generator;
	private Context context;


	public enum Route
	{
		ANY,
		CELL,
		WIFI
	}



	public WebService(Context context)
	{
		this.context=context;

		generator = new Random(Calendar.getInstance().getTimeInMillis());
	}

	public boolean isOnline()
	{
		return isOnline(Route.ANY);
	}

	public boolean isOnline(Route route)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null){
			if (cm.getActiveNetworkInfo().isConnected()){
				if (cm.getActiveNetworkInfo().isAvailable()){
					String response = executeHttpGet(ConfigurationManager.CONNECTION_CHECK_URL, route);
					if (response.contains("News for nerds, stuff that matters")){
						Log.d(getClass().getSimpleName(), "++ Internet Connection Available");
						return true;
					}
				}
			}
		}
		return false;
	}

	private String executeHttpGet(String url, Route route)
	{
		BufferedReader in = null;
		String page = "";
		try {
			HttpClient client = new DefaultHttpClient();

			if (route == Route.WIFI)
			{
				WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
				int wifiAddress = info.getIpAddress();
				client.getParams().setParameter(ConnRoutePNames.LOCAL_ADDRESS, Utils.intToInetAddress(wifiAddress));
			}
			else if (route == Route.CELL)
			{
				///TODO : implements!
			}

			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			HttpResponse response = client.execute(request);
			in = new BufferedReader
					(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			page = sb.toString();
		}catch (Exception e){
			Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
		}finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
			}
		}
		return page;
	}



	private class fileDownloadingThread extends Thread
	{
		private urlQueueHandler handler;
		private int index = 0;
		private Route route = Route.ANY;
		private IBytesTransferredReceiver receiver;
		private String tag;

		public fileDownloadingThread(urlQueueHandler handler, int index, Route route, IBytesTransferredReceiver receiver, String tag)
		{
			super();
			this.handler = handler;
			this.index = index;
			this.route = route;
			this.receiver = receiver;
			this.tag = tag;
		}

		public void run()
		{
			String url = handler.pop();
			while (url != null)
			{
				HttpClient client = new DefaultHttpClient();
				int read = 0;
				int cSize = 0;
				if (route == Route.WIFI)
				{
					WifiInfo info = ControllerServices.getInstance().getWifi().getWifiConnectionInfo();
					int wifiAddress = info.getIpAddress();
					client.getParams().setParameter(ConnRoutePNames.LOCAL_ADDRESS, Utils.intToInetAddress(wifiAddress));
				}
				else if (route == Route.CELL)
				{
					///TODO : implements!
				}

				HttpGet request = new HttpGet();
				try
				{
					request.setURI(new URI(url));
					HttpResponse response = client.execute(request);
					cSize = (int) response.getEntity().getContentLength();
					BufferedReader in = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
				}
				catch	(java.net.URISyntaxException e)
				{
					Log.e(getClass().getSimpleName(), "++ Unable to download from malfored URL : " + url + " : "  + e.getMessage(), e);
				}
				catch	(IOException e)
				{
					Log.e(getClass().getSimpleName(), "++ Interrupted while downloading " + url + " : " + e.getMessage(), e);
					e.printStackTrace();
				}

				receiver.receiveTransferredBytes(cSize, cSize, FINISH_COMPLETE + "-" + tag + "-" + url);
				Log.d(getClass().getSimpleName(), "++ DONE " + url);

				url = handler.pop();
			}

			handler.notifyDone();

		}
	}


	private class urlQueueHandler
	{
		private int deadSons = 0;
		private boolean enabled = true;
		private final ConcurrentLinkedQueue<String> urlQ = new ConcurrentLinkedQueue<String>();

		public urlQueueHandler(List<String> urls)
		{
			for (String url : urls)
				urlQ.add(url);
		}


		public void disable()
		{
			enabled = false;
		}

		public String peek()
		{
			String retval = null;
			if (enabled)
				retval = urlQ.peek();
			return retval;
		}

		public String pop()
		{
			String retval = null;
			if (enabled)
				retval = urlQ.poll();
			return retval;
		}

		//Called by threads to signify end of processing
		public void notifyDone()
		{
			deadSons += 1;
		}

		//return the number of finished threads
		public int getDone()
		{
			return deadSons;
		}

	}



	private List<String> getEmbeddedObjects(Document doc, String url)
	{
		Elements files = null;
		List<String> retval = new ArrayList<String>();

		String host = "";
		final String schemeSep = "://";
		final char urlSep =  '/';
		int schemeSize = url.indexOf(schemeSep);
		if (schemeSize > 0)
			host = url.substring(0, url.indexOf(urlSep, schemeSize + schemeSep.length()));
		else
			host = url.substring(0, url.indexOf(urlSep));



		for (String tag : htmlTag)
		{
			files = doc.getElementsByTag(tag);

			for( Element el : files)
			{
				String embedUrl = el.attr("src");
				if (embedUrl.length() > 0)
				{
					if (embedUrl.indexOf(schemeSep) < 0)
					{
						if (embedUrl.indexOf(urlSep) != 0)
						{
							if (url.endsWith(HTML_DEFAULT_EXT))
								url = url.substring(0, url.lastIndexOf(urlSep));

							embedUrl = url + urlSep + embedUrl;
						}
						else
						{
							embedUrl = host + embedUrl;
						}
					}
					retval.add(embedUrl);
				}
			}
		}
		return retval;
	}

	private final String HTML_DEFAULT_EXT = ".html";
	private final String HTML_DEFAULT_FILE = "/index" + HTML_DEFAULT_EXT;
	private static final List<String> htmlTag = Arrays.asList("img", "script");

	public void downloadWebPage(String url, Route route, int numConn, IBytesTransferredReceiver receiver)
	{
		downloadWebPage(url, route, numConn, receiver, "WEB");
	}

	public void downloadWebPage(String url, Route route, int numConn, IBytesTransferredReceiver receiver, String tag)
	{

		Document doc = null;
		List<String> embeddedUrls = null;
		receiver.receiveTransferredBytes(0, 0, tag + "_START");

		try
		{
			if (!url.endsWith(HTML_DEFAULT_EXT))
				url += HTML_DEFAULT_FILE;
			doc = Jsoup.connect(url).get();
		}
		catch (IOException e)
		{

			Log.e(getClass().getSimpleName(), "++ Unable to download page " + url + " : " + e.getMessage(), e);
			e.printStackTrace();
		}

		if (doc != null)
		{
			embeddedUrls = getEmbeddedObjects(doc, url);
			urlQueueHandler handler = new urlQueueHandler(embeddedUrls);
			for (int i = 0; i < numConn; i ++)
			{
				fileDownloadingThread dler = new fileDownloadingThread(handler, i, route, receiver, tag);
				dler.start();
			}

			int MAX_WAIT = 20000;
			int waitTime = 0;
			int checkFreq = 500;
			while (handler.getDone() != numConn && waitTime < MAX_WAIT)
			{
				try
				{
					Thread.sleep(checkFreq);
					waitTime += checkFreq;
				}
				catch (InterruptedException e)
				{
					handler.disable();
					Log.e(getClass().getSimpleName(), "++ Interrupted while downloading " + e.getMessage(), e);
					e.printStackTrace();
				}
			}
			handler.disable();
		}
	}


	private class urlCounter
	{
		private int currentValue;
		private IBytesTransferredReceiver receiver;
		private String tag;

		public urlCounter(IBytesTransferredReceiver receiver, String tag)
		{
			this.currentValue = 0;
			this.receiver = receiver;
			this.tag = tag;
		}

		public void count()
		{
			currentValue += 1;
		}

		public void countAndLog(com.squareup.okhttp.Response response, String url)
		{
			int i = 0;
			int leng = 0;
			int cLength = (int) response.body().contentLength();
			receiver.receiveTransferredBytes(cLength, cLength, FINISH_COMPLETE + "_SPDY_" + tag + "_" + url);
			count();
		}

		public int getCount()
		{
			return currentValue;
		}
	}

	public void downloadWebPageWSpdy(String url, Route route, IBytesTransferredReceiver receiver, String tag)
	{
		//TODO ROUTE ! (Not necesseraly possible, OkHttpClient does not extend HttpClient, therefore no setParameters
		List<String> embeddedUrls = null;

		OkHttpClient client = new OkHttpClient();


		List<Protocol> plist = new ArrayList<Protocol>();
		plist.add(Protocol.SPDY_3);
		plist.add(Protocol.HTTP_1_1);
		client.setProtocols(plist);

		final urlCounter uCntr = new urlCounter(receiver, tag);

		com.squareup.okhttp.Dispatcher dispatch = new com.squareup.okhttp.Dispatcher();
		dispatch.setMaxRequests(6);
		dispatch.setMaxRequestsPerHost(6);

		com.squareup.okhttp.Response response = null;
		Document doc = null;
		com.squareup.okhttp.Call call = null;

		com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
        		.url(url)
     			.build();

		call = client.newCall(request);
		receiver.receiveTransferredBytes(0, 0, "SPDY_" + tag + "_START");

		try
		{
			doc = Jsoup.connect(url).get();
		}
		catch (IOException e)
		{
			Log.e(getClass().getSimpleName(), "++ Unable to download page " + url + " : " + e.getMessage(), e);
			e.printStackTrace();
		}

		if (doc != null)
		{
			embeddedUrls = getEmbeddedObjects(doc, url);

			if (embeddedUrls != null)
			{
				for (final String eUrl : embeddedUrls)
				{
					final com.squareup.okhttp.Request loopRequest = new com.squareup.okhttp.Request.Builder()
						.url(eUrl)
						.build();

					com.squareup.okhttp.Call loopCall = client.newCall(loopRequest);
					loopCall.enqueue(new com.squareup.okhttp.Callback()
						{

							private urlCounter uCounter = uCntr;


							@Override public void onFailure(com.squareup.okhttp.Request loopRequest, IOException e)
							{
								Log.e(getClass().getSimpleName(), "++ Error  failes to execute " + loopRequest + " : " + e.getMessage(), e);
								uCounter.count();
							}

							@Override public void onResponse( com.squareup.okhttp.Response response) throws IOException
							{
								if (!response.isSuccessful())
								{
									Log.w(getClass().getSimpleName(), "++ Unexpected code " + response);
	    							}
								uCounter.countAndLog(response, eUrl);
							}
						}
					);

				}
			}


			if (uCntr != null && embeddedUrls != null)
			{
				int MAX_WAIT = 20000;
				int waitTime = 0;
				int checkFreq = 500;
				while (uCntr.getCount() != embeddedUrls.size() && waitTime < MAX_WAIT)
				{
					try
					{
						Thread.sleep(checkFreq);
						waitTime += checkFreq;
					}
					catch (InterruptedException e)
					{
						Log.e(getClass().getSimpleName(), "++ Spdy Result waiting interrupted : " + e.getMessage() );
						e.printStackTrace();
						waitTime = MAX_WAIT;
					}
				}
			}
		}
	}


	/*The uploading is done this way (by hand) because if not we cannot put a timeout and the socket would stay blocked until it is able to send something*/
	public boolean uploadFile(String ip, String uploadScript, IBytesTransferredReceiver receiver, byte[] bytes, int timeoutConnection, int timeoutSocket, int receiverCallTimer) throws UploadingInterruptedException, UploadingFailException, TimeoutException
	{
		SocketChannel socketChannel = null;
		try
		{
			int sent = 0;
		    urlRef = ip;
		    uploadFileSize = bytes.length;
		    transferReceiver = receiver;

		    long time = Calendar.getInstance().getTimeInMillis();

		    Log.d(getClass().getSimpleName(), "++ "+ "SIZE " + uploadFileSize + "RECEIVER " + transferReceiver + urlRef);

		    //Compatibility with first server
		    if (ip.contains("192.108.119.11"))
		    {
		    	uploadScript = "/cgi-bin/upload.cgi";
		    }


		    String postHeader = "POST "+ uploadScript +" HTTP/1.1\r\n" +
		                        "Host: " + ip +"\r\n" +
		                        "Content-Length: "+bytes.length+"\r\n" +
		                        "Content-Type: "+time+"\r\n" +
		                        "\r\n";

		    Log.d(getClass().getSimpleName(), "++ POST HEADER "+ postHeader);

		    byte[] headerBytes = postHeader.getBytes();
		    totalBytesToTransfer = headerBytes.length + bytes.length;
		    receiver.receiveTransferredBytes(0, totalBytesToTransfer, START+"-"+time);

		    int sleepConnectionTime = 5;//ms
		    socketChannel = SocketChannel.open();
		    socketChannel.configureBlocking(false);

		    //connect
		    socketChannel.connect(new InetSocketAddress(ip, 80));
		    int connectionDelay = 0;
		    long lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
		    while(! socketChannel.finishConnect())
			{
			    //We sleep so we can be interrupted and finish
			    try
				{
			        Thread.sleep(sleepConnectionTime); //we leave some time for it to connect
			    } catch (InterruptedException e) {
			        // finish
			        throw new UploadingInterruptedException("Size:"+ uploadFileSize +"-"+urlRef);
			    }
			    connectionDelay += sleepConnectionTime;
			    if (Calendar.getInstance().getTimeInMillis() - lastUpdateTimestamp >= receiverCallTimer)
				{
			        receiver.receiveTransferredBytes(0, totalBytesToTransfer, UPDATE);
			        lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
			    }
			    if (connectionDelay >= timeoutConnection){
			        throw new ConnectTimeoutException();
			    }
		    }

		    long lastSentTimestamp = Calendar.getInstance().getTimeInMillis();

		    //send the payload
		    sent = 0;
		    Log.d(getClass().getSimpleName(), "++ "+ "About to start sending");


			while(receiver.getTransferredBytes() < totalBytesToTransfer)
			{
				ByteBuffer buf = ByteBuffer.allocate(PACKET_SIZE * 10);
				sent = socketChannel.write(buf);
			    if (sent < 0)
				{
		       		Log.e(getClass().getSimpleName(), "++ "+ "Connection Refused");
			            throw new SocketTimeoutException();
				}

				//if we did not send anything, check when was the last time we could send, if it is more than the timeoutSocket, throw exception
			    if (sent == 0)
				{
			       	if (Calendar.getInstance().getTimeInMillis() - lastSentTimestamp >= timeoutSocket)
					{
	                	throw new SocketTimeoutException();
					}
			    }
				else
				{
					//if we sent something, call the receiver and change the lastSentTimestamp
			        receiver.receiveTransferredBytes(sent, totalBytesToTransfer);
			        lastSentTimestamp = Calendar.getInstance().getTimeInMillis();
			    }

			    //Either we sent it or not, we see if it is necessary to update the receiver data
			    if (Calendar.getInstance().getTimeInMillis() - lastUpdateTimestamp >= receiverCallTimer){
			        receiver.receiveTransferredBytes(0, totalBytesToTransfer, UPDATE);
			        lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
			    }
			    //We sleep so we can be interrupted and finish
			    try {
			        Thread.sleep(0);
			    } catch (InterruptedException e) {
			        // finish
			        throw new UploadingInterruptedException("Size:"+ uploadFileSize +"-"+urlRef);
			    }
		    }

		    if (receiver.getTransferredBytes() >= totalBytesToTransfer){
		        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_COMPLETE);
		        return true;
		    }else{
		        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INCOMPLETE);
		        return false;
		    }

	    } catch(UploadingInterruptedException ex){
	        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INTERRUPTED);
	        throw new UploadingInterruptedException(ex.getMessage());
	    }catch(SocketTimeoutException e){
	        String msg = SOCKET_TIMEOUT+"("+ timeoutSocket +"ms)";
	        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
	        throw new TimeoutException(msg);
	    }catch(ConnectTimeoutException e){
	        String msg = CONNECT_TIMEOUT+"("+ timeoutConnection +"ms)";
	        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
	        throw new TimeoutException(msg);
	    }catch(Exception e){
	        Log.w(getClass().getSimpleName(), "++ "+ e.getMessage(), e);
	        receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+e.getMessage());
	        throw new UploadingFailException(e.getMessage());
	    } finally{
	        if (socketChannel != null){
	            try {
	                socketChannel.close();
	            } catch (IOException e) {
	                Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
	            }
	        }
	    }
	}

	@Override
	public boolean downloadFile(String host, String filePath, IBytesTransferredReceiver receiver, long length, int timeoutConnection, int timeoutSocket, int receiverCallTimer, String ap, String network) throws DownloadingInterruptedException, DownloadingFailException, TimeoutException
	{
		transferReceiver = receiver;
		SocketChannel socketChannel = null;
		try {
			totalBytesToTransfer = length;
		    long time = Calendar.getInstance().getTimeInMillis();

			receiver.receiveTransferredBytes(0, totalBytesToTransfer, START+"-"+time);

			int sleepConnectionTime = 5;//ms
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			//connect

			socketChannel.connect(new InetSocketAddress(host, 80));
			int connectionDelay = 0;
			//int lastConnectionDelayUpdate = 0;
			long lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
			while(! socketChannel.finishConnect() ){
				//We sleep so we can be interrupted and finish
				try {
					Thread.sleep(sleepConnectionTime); //we leave some time for it to connect
				} catch (InterruptedException e) {
					// finish
					throw new DownloadingInterruptedException(host + filePath);
				}
				connectionDelay += sleepConnectionTime;
			    //we try to log an Update if the receiverCallTimer elapsed
		    	if (Calendar.getInstance().getTimeInMillis() - lastUpdateTimestamp >= receiverCallTimer){
		    		receiver.receiveTransferredBytes(0, totalBytesToTransfer, UPDATE);
		    		lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
		    	}
				if (connectionDelay >= timeoutConnection){
					throw new ConnectTimeoutException();
				}
			}

			//Prepare and send the GET REQUEST


			network = network.replaceAll(" ", "-");

			String getHeader;

			getHeader = "GET "+ filePath +"?id="+time + "&mac="+ap+"&ssid="+network+"&size="+totalBytesToTransfer+" HTTP/1.1\n" +
			"Host: " + host +"\n" +
			"Connection: Keep-Alive\n" +
			"\n";


			Log.d(getClass().getSimpleName(), "++ " + getHeader);

			byte[] headerBytes = getHeader.getBytes();

			ByteBuffer buf = ByteBuffer.allocate((int)headerBytes.length);
			buf.clear();
			buf.put(headerBytes);//we put the GET REQUEST in the buffer
			buf.flip();

			long lastSentTimestamp = Calendar.getInstance().getTimeInMillis();

			//send the request
			int sent = 0;
			Log.d(getClass().getSimpleName(), "++ "+ "About to start sending request");



			while(buf.hasRemaining())
			{
				//Log.d(getClass().getSimpleName(), "++ "+ "Has remaining");
				sent = socketChannel.write(buf);
			    //Log.d(getClass().getSimpleName(), "++ "+ "sent: "+sent);
			    if (sent == 0){ //if we did not send anything, check when was the last time we could send, if it is more than the timeoutSocket, throw exception
			    	if (Calendar.getInstance().getTimeInMillis() - lastSentTimestamp >= timeoutSocket){
			    		throw new SocketTimeoutException();
			    	}
			    }else{
			    	lastSentTimestamp = Calendar.getInstance().getTimeInMillis();
			    }
			    //Either we sent it or not, we see if it is necessary to update the receiver data
		    	if (Calendar.getInstance().getTimeInMillis() - lastUpdateTimestamp >= receiverCallTimer){
		    		receiver.receiveTransferredBytes(0, totalBytesToTransfer, UPDATE);
		    		lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
		    	}
				//We sleep so we can be interrupted and finish
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					// finish
					throw new DownloadingInterruptedException(host + filePath);
				}
			}

			//we sent the complete request, now we read the response (the file)
			long lastReadTimestamp = Calendar.getInstance().getTimeInMillis();


			receiver.receiveTransferredBytes(0, totalBytesToTransfer, TRANSFERRING+"-"+lastSentTimestamp);

			int read = 0;
			ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE * 10);

			Log.d(getClass().getSimpleName(), "++ "+ "About to start downloading");

			while(receiver.getTransferredBytes() < totalBytesToTransfer)
			{
				read = socketChannel.read(buffer);

				buffer.clear();

				//The channel was closed, possibly from outside wi2me
				if (read < 0)
				{
					Log.d(getClass().getSimpleName(), "++ "+ "Channel Closed, interrupting");
			    		throw new SocketTimeoutException();
				}

				if (read == 0)
				{
					//if we did not read anything, check when was the last time we could read, if it is more than the timeoutSocket, throw exception
			    		if (Calendar.getInstance().getTimeInMillis() - lastReadTimestamp >= timeoutSocket)
					{
			    			throw new SocketTimeoutException();
			    		}
				}
				else
				{
					//if we read something, call the receiver and change the lastReadTimestamp
			    		receiver.receiveTransferredBytes(read, totalBytesToTransfer);
			    		lastReadTimestamp = Calendar.getInstance().getTimeInMillis();
			    		buffer.flip(); //return buffer to the beginning to read again
			    	}

				//Either we read it or not, we see if it is necessary to update the receiver data
		    		if (Calendar.getInstance().getTimeInMillis() - lastUpdateTimestamp >= receiverCallTimer)
				{
		    			receiver.receiveTransferredBytes(0, totalBytesToTransfer, UPDATE);
		    			lastUpdateTimestamp = Calendar.getInstance().getTimeInMillis();
		    		}

				//We sleep so we can be interrupted and finish
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					// finish
					throw new DownloadingInterruptedException(host + filePath);
				}
			}

	        if (receiver.getTransferredBytes() >= totalBytesToTransfer){
	        	receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_COMPLETE);
	        	return true;
	        }else{
	        	receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INCOMPLETE);
	        	return false;
	        }
		}catch(DownloadingInterruptedException e){
				receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INTERRUPTED);
				throw e;
		}catch(SocketTimeoutException e){
			String msg = SOCKET_TIMEOUT+"("+ timeoutSocket +"ms)";
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
			throw new TimeoutException(msg);
		}catch(ConnectTimeoutException e){
			String msg = CONNECT_TIMEOUT+"("+ timeoutConnection +"ms)";
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
			throw new TimeoutException(msg);
		}catch(Exception e){
			e.printStackTrace();


			Log.w(getClass().getSimpleName(), e.getMessage(), e);
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+e.getMessage());
			throw new DownloadingFailException(e.getMessage());
		} finally{
			if (socketChannel != null){
				try {
					socketChannel.close();
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "++ " + e.getMessage(), e);
				}
			}
		}
	}

	private boolean downloadFileWithLength(String url, IBytesTransferredReceiver receiver, long length, int timeoutConnection, int timeoutSocket) throws DownloadingInterruptedException, DownloadingFailException, TimeoutException{
    	byte[] buffer = new byte[100000];
    	int bytesReceived = 0;

		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		try{
			boolean downloadComplete = false;

	    	HttpGet request = new HttpGet(url);
	    	HttpResponse response = httpClient.execute(request);

	    	//Broadcast to know it is starting
	    	receiver.receiveTransferredBytes(bytesReceived, totalBytesToTransfer, START);

	    	InputStream in = response.getEntity().getContent();


	        while ((bytesReceived = in.read(buffer)) != -1){
	        	receiver.receiveTransferredBytes(bytesReceived, totalBytesToTransfer);
	        	//We sleep so we can be interrupted
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					// finish
					throw new DownloadingInterruptedException(url);
				}
	        }

	        if (receiver.getTransferredBytes() == totalBytesToTransfer){
	        	receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_COMPLETE);
	        	downloadComplete = true;
	        }else{
	        	receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INCOMPLETE);
	        }
	        return downloadComplete;
		}catch(DownloadingInterruptedException ex){
				receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_INTERRUPTED);
				throw ex;
		}catch(SocketTimeoutException e){
			String msg = SOCKET_TIMEOUT+"("+ timeoutSocket +"ms)";
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
			throw new TimeoutException(msg);
		}catch(ConnectTimeoutException e){
			String msg = CONNECT_TIMEOUT+"("+ timeoutConnection +"ms)";
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+msg);
			throw new TimeoutException(msg);
		}catch(Exception e){
			Log.w(getClass().getSimpleName(), "++ "+e.getMessage(), e);
			receiver.receiveTransferredBytes(0, totalBytesToTransfer, FINISH_ERROR+"-Description:"+e.getMessage());
			throw new DownloadingFailException(e.getMessage());
		} finally{
			httpClient.getConnectionManager().shutdown();
		}
	}

	public float getAverageThroughput()
	{
		float retval = 0;
		if (transferReceiver != null)
		{
			retval = transferReceiver.getAverageThroughput();
		}
		return retval;
	}


}
