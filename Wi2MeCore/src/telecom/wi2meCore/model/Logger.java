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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.Date;
import java.util.List;

import telecom.wi2meCore.controller.services.persistance.TextTraceHelper;
import telecom.wi2meCore.controller.services.persistance.ITraceIterator;
import telecom.wi2meCore.controller.configuration.ConfigurationManager;
import telecom.wi2meCore.model.entities.Trace;
import android.os.Environment;
import android.util.Log;
import telecom.wi2meCore.model.entities.Trace.TraceType;


public class Logger extends ILogger
{



	public enum StorageFormat
	{
		STORAGE_SQLITE,
		STORAGE_TEXT
	}


	private static ILogger instance = null;
	private Boolean trial = ConfigurationManager.TRIAL;
	private int count;

	//private static final int maxCachedTraces = 1000;
	private static final int maxCachedTraces = 10;

	private ArrayList<Trace> traces;
	private ArrayList<Trace> toRegister;

	private Semaphore dbSema;

	private Logger(){
		traces = new ArrayList<Trace>();
		dbSema = new Semaphore(1);
	}


	/* (non-Javadoc)
	 * @see telecom.wi2meTraces.model.ILogger#log(telecom.wi2meTraces.model.entities.Trace)
	 */
	@Override
	public synchronized void log(Trace trace)
	{

		Type type = null;

		if (trace != null)
		{
			count++;

			switch (trace.getStoringType())
			{
				case CELL_SCAN_RESULT:
				case CELL_CONNECTION_EVENT:
				case CELL_CONNECTION_DATA:
				case EXTERNAL_EVENT:
					type = Type.CELL;
					break;
				case WIFI_SCAN_RESULT:
				case WIFI_CONNECTION_EVENT:
				case WIFI_PING:
				case WIFI_CONNECTION_DATA:
				case COMMUNITY_NETWORK_CONNECTION_EVENT:
				case WIFI_SNIFFER_DATA:
				case WIFI_CONNECTION_INFO:
				case BYTES_PER_UID:
					type = Type.WIFI;
				case LOCATION_EVENT:
					type = Type.LOC;
					break;
			}
			this.setChanged();
			this.notifyObservers(new TraceString(type, trace));

			if (!trial) //not TRIAL log everithing
			{
				traces.add(trace);

			} else //is TRIAL
			{
				if (count < ConfigurationManager.MAX_TRACES)
					traces.add(trace);
			}

			if (traces.size() > maxCachedTraces)
			{
				flush();
			}
		}
	}

	public static ILogger getInstance(){
		if (instance == null){
			instance = new Logger();
		}
		return instance;
	}

	/* (non-Javadoc)
	 * @see telecom.wi2meTraces.model.ILogger#flush()
	 */
	@Override
	public synchronized void flush()
	{
		Thread databaseWriting;

		if (dbSema.tryAcquire())
		{
			toRegister = (ArrayList<Trace>) traces.clone();
			traces.clear();

			databaseWriting = new Thread()
			{
				@Override
				public void run()
				{
					TextTraceHelper.getTextTraceHelper().saveAllTraces(toRegister);
					toRegister.clear();
					dbSema.release();
				}
			};
			databaseWriting.start();
		}
	}

	public enum Type{
		CELL,
		WIFI,
		LOC,
	}

	public class TraceString{
		public Type type;
		public Trace content;
		public TraceString(Type type, Trace content){
			this.type = type;
			this.content = content;
		}
	}

}
