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

package telecom.wi2meCore.controller.services.persistance;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.services.ControllerServices;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetworkService;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetwork;
import telecom.wi2meCore.model.TraceManager;
import telecom.wi2meCore.model.entities.*;
import telecom.wi2meCore.model.entities.ConnectionInfo.ConnectionState;
import telecom.wi2meCore.model.wifiCommands.WifiBytesTransferedReceiver;
/**
 * Service to manage the storage of the trace captured.
 * @author XXX
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper implements ITraceDatabase {

	public enum TraceType{
		WIFI_SCAN_RESULT,
		WIFI_CONNECTION_EVENT,
		COMMUNITY_NETWORK_CONNECTION_EVENT,
		WIFI_CONNECTION_DATA,
		WIFI_SNIFFER_DATA,
		WIFI_CONNECTION_INFO,
		BYTES_PER_UID,
		CELL_SCAN_RESULT,
		CELL_CONNECTION_EVENT,
		CELL_CONNECTION_DATA, 
		EXTERNAL_EVENT, 
		WIFI_PING
	}

	private static Set<Integer> uidTotal;
	/*--- CONSTANTS FOR DATABASE ---*/
	public static PackageManager packageManager;
	private static final String KEY_NAME = "id";	
	private static final String UIDTABLENAME = "Uid";
	private static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "TraceLog";
	private static final long INVALID_ID = -1;
	private static final String INEXISTENT_COM_NET = "Inexistent community network for user to add.";
	/*--- CONSTANTS FOR DATABASE ---*/

	/*--- STATEMENTS TO CREATE TABLES ---*/
	private static final String TRACE_TABLE_CREATE =
			"CREATE TABLE " + Trace.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					Trace.TIMESTAMP + " TIMESTAMP, "+
					Trace.ALTITUDE + " REAL, " +
					Trace.LONGITUDE + " REAL, " +
					Trace.LATITUDE + " REAL, " +
					Trace.ACCURACY + " REAL, " +
					Trace.BEARING + " REAL, " +
					Trace.SPEED + " REAL, " +
					Trace.PROVIDER + " REAL, " +
					Trace.BATT_LEVEL + " INTEGER," +
					Trace.TYPE + " TEXT);";    

	private static final String WIFI_AP_TABLE_CREATE =
			"CREATE TABLE " + WifiAP.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiAP.BSSID + " TEXT, " +
					WifiAP.SSID + " TEXT, " +
					WifiAP.LEVEL + " INTEGER, " +
					WifiAP.CHANNEL + " INTEGER, " +
					WifiAP.LINK_SPEED + " INTEGER, " +
					WifiAP.CAPABILITIES + " TEXT);";

	private static final String UID_TABLE_CREATE =
			"CREATE TABLE " + UIDTABLENAME + " (" + 
					KEY_NAME + " INTEGER PRIMARY KEY, Uid INTEGER, UidName TEXT);";

	private static final String CELL_TABLE_CREATE =
			"CREATE TABLE " + Cell.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					Cell.OPERATOR_NAME + " TEXT, " +
					Cell.OPERATOR + " TEXT, " +
					Cell.NET_TYPE + " TEXT, " +
					Cell.PHONE_TYPE + " TEXT, " +
					Cell.CID + " INTEGER, " +
					Cell.LAC + " INTEGER, " +
					Cell.LEVEL_DBM + " INTEGER, " +
					Cell.CURRENT + " INTEGER);";

	private static final String WIFI_SCAN_RESULT_TABLE_CREATE =
			"CREATE TABLE " + WifiScanResult.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiScanResult.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE);";

	private static final String WIFI_CONNECTION_EVENT_TABLE_CREATE =
			"CREATE TABLE " + WifiConnectionEvent.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiConnectionEvent.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE, " +
					WifiConnectionEvent.EVENT + " TEXT);";

	private static final String COMMUNITY_NETWORK_CONNECTION_EVENT_TABLE_CREATE =
			"CREATE TABLE " + CommunityNetworkConnectionEvent.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					CommunityNetworkConnectionEvent.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE, " +
					CommunityNetworkConnectionEvent.USERNAME + " TEXT, " +
					CommunityNetworkConnectionEvent.EVENT + " TEXT);";

	private static final String WIFI_CONNECTION_DATA_TABLE_CREATE =
			"CREATE TABLE " + WifiConnectionData.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiConnectionData.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE, " +
					ConnectionData.TYPE + " TEXT, " +
					ConnectionData.IP + " TEXT, " +
					ConnectionData.BYTES_TRANSFERRED + " INTEGER, " +
					ConnectionData.TOTAL_BYTES + " INTEGER, " +
					ConnectionData.TX_PACKETS + " INTEGER, " +
					ConnectionData.RX_PACKETS + " INTEGER, " +
					ConnectionData.RETRIES + " INTEGER);";

	private static final String WIFI_SNIFFER_DATA_TABLE_CREATE =
			"CREATE TABLE " + WifiSnifferData.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiSnifferData.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE, " +
					SnifferData.TX_BYTES + " INTEGER, " +
					SnifferData.RX_BYTES + " INTEGER, " +
					SnifferData.TX_PACKETS + " INTEGER, " +
					SnifferData.RX_PACKETS + " INTEGER, " +
					SnifferData.RETRIES + " INTEGER, " +
					WifiSnifferData.SNIFF_SEQUENCE + " INTEGER);";

	private static final String BYTES_PER_UID_TABLE_CREATE =
			"CREATE TABLE " + BytesperUid.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					BytesperUid.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					BytesperUid.SNIFF_SEQUENCE + " INTEGER, " +
					BytesperUid.UID + " INTEGER, " +
					//BytesperUid.UID_NAME + " TEXT, " +
					BytesperUid.TXBYTES + " INTEGER, " +
					BytesperUid.RXBYTES + " INTEGER);";

	private static final String WIFI_CONNECTIONINFO_TABLE_CREATE =
			"CREATE TABLE " + WifiConnectionInfo.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiConnectionInfo.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiConnectionInfo.SNIFF_SEQUENCE + " INTEGER, " +
					ConnectionInfo.PROTOCOL + " TEXT, " +
					ConnectionInfo.LOCADD + " TEXT, " +
					ConnectionInfo.REMADD + " TEXT, " +
					ConnectionInfo.LOCPORT + " INTEGER, " +
					ConnectionInfo.REMPORT + " INTEGER, " +    
					ConnectionInfo.STATE + " TEXT, " +
					ConnectionInfo.UID + " INTEGER);";


	private static final String WIFI_PING_TABLE_CREATE =
			"CREATE TABLE " + WifiPing.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					WifiPing.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiAP.WIFI_AP_REFERENCE + " INTEGER REFERENCES " + WifiAP.TABLE_NAME + " ON DELETE CASCADE, " +
					WifiPing.IP + " TEXT, " +
					WifiPing.SENT + " INTEGER, " +
					WifiPing.RECEIVED + " INTEGER, " +
					WifiPing.RTT_MIN + " REAL, " +
					WifiPing.RTT_MAX + " REAL, " +
					WifiPing.RTT_AVG + " REAL, " +
					WifiPing.RTT_MDEV + " REAL);";

	private static final String CELL_CONNECTION_EVENT_TABLE_CREATE =
			"CREATE TABLE " + CellularConnectionEvent.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					CellularConnectionEvent.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					Cell.CELL_REFERENCE + " INTEGER REFERENCES " + Cell.TABLE_NAME + " ON DELETE CASCADE, " +
					CellularConnectionEvent.EVENT + " TEXT);";

	private static final String CELL_CONNECTION_DATA_TABLE_CREATE =
			"CREATE TABLE " + CellularConnectionData.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					CellularConnectionData.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					Cell.CELL_REFERENCE + " INTEGER REFERENCES " + Cell.TABLE_NAME + " ON DELETE CASCADE, " +
					ConnectionData.TYPE + " TEXT, " +
					ConnectionData.IP + " TEXT, " +
					ConnectionData.BYTES_TRANSFERRED + " INTEGER, " +
					ConnectionData.TOTAL_BYTES + " INTEGER, " +
					ConnectionData.TX_PACKETS + " INTEGER, " +
					ConnectionData.RX_PACKETS + " INTEGER, " +
					ConnectionData.RETRIES + " INTEGER);";

	private static final String CELL_SCAN_RESULT_TABLE_CREATE =
			"CREATE TABLE " + CellularScanResult.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					CellularScanResult.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					Cell.CELL_REFERENCE + " INTEGER REFERENCES " + Cell.TABLE_NAME + " ON DELETE CASCADE);";

	private static final String EXTERNAL_EVENT_TABLE_CREATE =
			"CREATE TABLE " + ExternalEvent.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					ExternalEvent.TRACE_REFERENCE + " INTEGER REFERENCES "+ Trace.TABLE_NAME +" ON DELETE CASCADE, " +
					WifiConnectionEvent.EVENT + " TEXT);";

	private static final String COMMUNITY_NETWORK_TABLE_CREATE =
			"CREATE TABLE " + ICommunityNetwork.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					ICommunityNetwork.NAME + " TEXT UNIQUE, " +
					ICommunityNetwork.MATCH_SSID_REG_EX + " TEXT, " +
					ICommunityNetwork.JS_CONNECTION_PLUGIN + " TEXT);";

	private static final String USER_TABLE_CREATE =
			"CREATE TABLE " + User.TABLE_NAME + " (" +
					KEY_NAME + " INTEGER PRIMARY KEY, " +
					User.COM_NET_REFERENCE + " INTEGER REFERENCES "+ ICommunityNetwork.TABLE_NAME +" ON DELETE CASCADE, " +
					User.NAME + " TEXT, " +
					User.PASS + " TEXT);";
	/*--- STATEMENTS TO CREATE TABLES ---*/

	private static final String SELECT_ALL_TRACES = "Select * from " + Trace.TABLE_NAME;

	private static final String generateDeletionTriggerForWifiAP(String tableName){
		return "CREATE TRIGGER on_delete" + tableName + "_deleteWifiAP AFTER DELETE ON " + tableName + " BEGIN "+
				"DELETE FROM "+ WifiAP.TABLE_NAME +" WHERE " + KEY_NAME + " = old." + WifiAP.WIFI_AP_REFERENCE + ";" +
				"END;";
	}

	private static final String generateDeletionTriggerForCell(String tableName){
		return "CREATE TRIGGER on_delete" + tableName + "_deleteCell AFTER DELETE ON " + tableName + " BEGIN "+
				"DELETE FROM "+ Cell.TABLE_NAME +" WHERE " + KEY_NAME + " = old." + Cell.CELL_REFERENCE + ";" +
				"END;";
	}

	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		packageManager = context.getPackageManager();
		if (uidTotal == null) uidTotal = new HashSet<Integer>();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.setLockingEnabled(false);
		/* Tables creation */
		createTables(db);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {		
		super.onOpen(db);
		if (!db.isReadOnly())
		{
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
		dropTables();
		onCreate(db);
	}


	public void resetTables(){
		dropTables();
		SQLiteDatabase db = getWritableDatabase();
		createTables(db);
		db.close();
	}

	private static String getDBFileName(String packageName){
		return Environment.getDataDirectory() + "/data/" + packageName + "/databases/" + DATABASE_NAME;
	}

	public static boolean resetDatabase(String packageName){
		WifiBytesTransferedReceiver.resetSniffSequence();
		if (uidTotal != null) uidTotal.clear();
		String databaseFileName = getDBFileName(packageName);
		if (isInitialized())
			dbh.close();
		File file = new File(databaseFileName);
		if (file.exists()){
			if (file.delete()){
				dbh = null;
			}
		}
		return false;
	}

	private synchronized void dropTables(){
		SQLiteDatabase db = getWritableDatabase();
		Log.d(getClass().getSimpleName(), "++ " + "SQL About to drop tables");
		//Use a transaction because if something fails, we don't want to have the database in an invalid state
		db.beginTransaction();
		try{   
			db.execSQL("DROP TABLE " + User.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + ICommunityNetwork.TABLE_NAME + ";");

			db.execSQL("DROP TABLE " + ExternalEvent.TABLE_NAME + ";");

			db.execSQL("DROP TABLE " + CellularScanResult.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + CellularConnectionData.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + CellularConnectionEvent.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + Cell.TABLE_NAME + ";");

			db.execSQL("DROP TABLE " + WifiPing.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiConnectionData.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiSnifferData.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiConnectionInfo.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + BytesperUid.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + CommunityNetworkConnectionEvent.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiConnectionEvent.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiScanResult.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + WifiAP.TABLE_NAME + ";");
			db.execSQL("DROP TABLE " + UIDTABLENAME + ";");

			db.execSQL("DROP TABLE " + Trace.TABLE_NAME + ";");        	
			//If everything went fine, commit the changes
			db.setTransactionSuccessful();

			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables dropped OK");
		}
		catch(SQLException e){
			Log.e("SQL Insert", e.getMessage(), e);
			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables drop FAILED");
		}
		catch(RuntimeException e){
			Log.e("SQL Insert - Runtime", e.getMessage(), e);
			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables drop FAILED");
		}
		finally{
			//End the transaction. If something went wrong, changes are rolled back
			db.endTransaction();
			db.close();
		}

	}

	private synchronized void createTables(SQLiteDatabase db){
		Log.d(getClass().getSimpleName(), "++ " + "SQL About to create tables");

		//Use a transaction because if something fails, we don't want to have the database in an invalid state
		db.beginTransaction();
		try{ 
			db.execSQL(TRACE_TABLE_CREATE);

			db.execSQL(WIFI_AP_TABLE_CREATE);		
			db.execSQL(WIFI_SCAN_RESULT_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(WifiScanResult.TABLE_NAME));
			db.execSQL(WIFI_CONNECTION_EVENT_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(WifiConnectionEvent.TABLE_NAME));
			db.execSQL(COMMUNITY_NETWORK_CONNECTION_EVENT_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(CommunityNetworkConnectionEvent.TABLE_NAME));
			db.execSQL(WIFI_CONNECTION_DATA_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(WifiConnectionData.TABLE_NAME));
			db.execSQL(WIFI_SNIFFER_DATA_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(WifiSnifferData.TABLE_NAME));
			db.execSQL(WIFI_CONNECTIONINFO_TABLE_CREATE);
			db.execSQL(BYTES_PER_UID_TABLE_CREATE);
			db.execSQL(WIFI_PING_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForWifiAP(WifiPing.TABLE_NAME));
			db.execSQL(UID_TABLE_CREATE);

			db.execSQL(CELL_TABLE_CREATE);
			db.execSQL(CELL_CONNECTION_EVENT_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForCell(CellularConnectionEvent.TABLE_NAME));
			db.execSQL(CELL_CONNECTION_DATA_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForCell(CellularConnectionData.TABLE_NAME));
			db.execSQL(CELL_SCAN_RESULT_TABLE_CREATE);
			db.execSQL(generateDeletionTriggerForCell(CellularScanResult.TABLE_NAME));

			db.execSQL(EXTERNAL_EVENT_TABLE_CREATE);

			db.execSQL(COMMUNITY_NETWORK_TABLE_CREATE);
			db.execSQL(USER_TABLE_CREATE);

			//If everything went fine, commit the changes
			db.setTransactionSuccessful();

			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables created OK");
		}
		catch(SQLException e){
			Log.e("SQL Insert", e.getMessage(), e);
			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables create FAILED");
		}
		catch(RuntimeException e){
			Log.e("SQL Insert - Runtime", e.getMessage(), e);
			Log.d(getClass().getSimpleName(), "++ " + "SQL Tables create FAILED");
		}
		finally{
			//End the transaction. If something went wrong, changes are rolled back
			db.endTransaction();
		}
	}

	public void closeDatabase(){
		//getWritableDatabase().close();
		close();
	}

	public static DatabaseHelper dbh = null;

	public static boolean isInitialized(){
		return dbh != null;
	}

	/**
	 * Initializes the only instance of the DatabaseHelper. This method must be run before any other to create the database and its tables.
	 * @param context The context in which the database will run
	 */
	public static void initialize(Context context){
		if (dbh == null){
			dbh = new DatabaseHelper(context);
			dbh.getWritableDatabase().close(); //make sure tables are created
		}
	}

	/**
	 * Method to obtain the DatabaseHelper (as it is implemented as a Singleton). The "initialize()" method must be run first.
	 * @return The only instance of the database helper
	 */
	public static ITraceDatabase getDatabaseHelper(){
		if (dbh == null)
			throw new RuntimeException("Database never initialized. Please initialize first.");
		return dbh;
	}

	private long saveTrace(SQLiteDatabase db, Trace trace, String type) throws SQLException{
		ContentValues values = new ContentValues();
		values.put(Trace.TIMESTAMP, trace.getTimestamp());
		values.put(Trace.ALTITUDE, trace.getAltitude());
		values.put(Trace.LONGITUDE, trace.getLongitude());
		values.put(Trace.LATITUDE, trace.getLatitude());
		values.put(Trace.ACCURACY, trace.getAccuracy());
		values.put(Trace.SPEED, trace.getSpeed());
		values.put(Trace.BEARING, trace.getBearing());
		values.put(Trace.PROVIDER, trace.getProvider());
		values.put(Trace.BATT_LEVEL, trace.getBatteryLevel());    	
		values.put(Trace.TYPE, type);
		return db.insertOrThrow(Trace.TABLE_NAME, "", values);
	}

	private long saveWifiAP(SQLiteDatabase db, WifiAP ap) throws SQLException{
		ContentValues values = new ContentValues();
		values.put(WifiAP.BSSID, ap.getBSSID());
		values.put(WifiAP.SSID, ap.getSsid());    	
		values.put(WifiAP.LEVEL, ap.getLevel());
		values.put(WifiAP.CHANNEL, ap.getChannel());
		values.put(WifiAP.LINK_SPEED, ap.getLinkSpeed());
		values.put(WifiAP.CAPABILITIES, ap.getCapabilities());
		return db.insertOrThrow(WifiAP.TABLE_NAME, "", values);
	}

	private long saveUid(SQLiteDatabase db, int uid) throws SQLException{
		ContentValues values = new ContentValues();
		String uidName = packageManager.getNameForUid(uid);		
		//Log.e("UidName", uidName);
		values.put("Uid", uid);
		values.put("UidName", uidName);    	
		return db.insertOrThrow(UIDTABLENAME, "", values);
	}

	private long saveCell(SQLiteDatabase db, Cell cell) throws SQLException{
		ContentValues values = new ContentValues();
		values.put(Cell.OPERATOR_NAME, cell.getOperatorName());
		values.put(Cell.OPERATOR, cell.getOperator());
		values.put(Cell.NET_TYPE, cell.getNetworkType());
		values.put(Cell.PHONE_TYPE, cell.getPhoneType());
		values.put(Cell.CID, cell.getCid());
		values.put(Cell.LAC, cell.getLac());
		values.put(Cell.LEVEL_DBM, cell.getLeveldBm());
		values.put(Cell.CURRENT, cell.isCurrent());
		return db.insertOrThrow(Cell.TABLE_NAME, "", values);
	}

	/**
	 * All save methods are synchronized
	 * @param trace
	 * @param type
	 * @return
	 */

	/*
	private synchronized long saveTraceByType(Trace trace, TraceType type){
		long traceId = 0;
		SQLiteDatabase db = getWritableDatabase();
		//Use a transaction because if something fails, we don't want to have the database in an invalid state
		db.beginTransaction();
		try{   
			//First we save the trace in its table
			traceId = saveTrace(db, trace, type.toString());

			saveType(db, trace, type, traceId);

			//If everything went fine, commit the changes
			db.setTransactionSuccessful();
		}
		catch(SQLException e){
			Log.e("SQL Insert", "++ "+e.getMessage(), e);
		}
		catch(RuntimeException e){
			Log.e("SQL Insert - Runtime", "++ "+e.getMessage(), e);
		}
		finally{
			//End the transaction. If something went wrong, changes are rolled back
			db.endTransaction();
			db.close();
		}
		return traceId;
	}*/

	@Override
	public void saveAllTraces(List<Trace> traces)
	{
		Log.d("SQL Insert", "++ To save "+traces.size()+" traces");
		long traceId = 0;
		SQLiteDatabase db = getWritableDatabase();
		//Use a transaction because if something fails, we don't want to have the database in an invalid state
		db.beginTransaction();
		try{   
			for (Trace t : traces){
				//First we save the trace in its table
				traceId = saveTrace(db, t, t.getStoringType().toString());

				saveType(db, t, t.getStoringType(), traceId);        		
			}

			//If everything went fine, commit the changes
			db.setTransactionSuccessful();
		}
		catch(SQLException e){
			Log.e("SQL Insert", e.getMessage(), e);
		}
		catch(RuntimeException e){
			Log.e("SQL Insert - Runtime", e.getMessage(), e);
		}
		finally{
			//End the transaction. If something went wrong, changes are rolled back
			db.endTransaction();
			db.close();
		}
	}

	private void saveType(SQLiteDatabase db, Trace trace, TraceType type, Long traceId) {
		long wifiAPId;
		long cellId;
		ContentValues values;
		switch (type){
		case WIFI_SCAN_RESULT:
			WifiScanResult wScanResult = (WifiScanResult) trace;
			//Now we save the scan results in the corresponding table, using the Id of the trace        	
			for (WifiAP ap : wScanResult.getResults()){
				wifiAPId = saveWifiAP(db, ap);
				values = new ContentValues();
				values.put(WifiScanResult.TRACE_REFERENCE, traceId);
				values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
				db.insertOrThrow(WifiScanResult.TABLE_NAME, "", values);
			}
			break;
		case WIFI_CONNECTION_EVENT:
			WifiConnectionEvent wConnectionEvent = (WifiConnectionEvent) trace;
			//We save the wifi AP we are trying to connect to
			wifiAPId = saveWifiAP(db, wConnectionEvent.getConnectionTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(WifiConnectionEvent.TRACE_REFERENCE, traceId);
			values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
			values.put(WifiConnectionEvent.EVENT, wConnectionEvent.getEvent());
			db.insertOrThrow(WifiConnectionEvent.TABLE_NAME, "", values);
			break;
		case COMMUNITY_NETWORK_CONNECTION_EVENT:
			CommunityNetworkConnectionEvent cnConnectionEvent = (CommunityNetworkConnectionEvent) trace;
			//We save the wifi AP we are connected to
			wifiAPId = saveWifiAP(db, cnConnectionEvent.getConnectedTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(CommunityNetworkConnectionEvent.TRACE_REFERENCE, traceId);
			values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
			values.put(CommunityNetworkConnectionEvent.EVENT, cnConnectionEvent.getEvent());
			values.put(CommunityNetworkConnectionEvent.USERNAME, cnConnectionEvent.getUsername());
			db.insertOrThrow(CommunityNetworkConnectionEvent.TABLE_NAME, "", values);
			break;
		case WIFI_CONNECTION_DATA:
			WifiConnectionData wcd = (WifiConnectionData) trace;
			//We save the wifi AP we are connected to
			wifiAPId = saveWifiAP(db, wcd.getConnectedTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(WifiConnectionData.TRACE_REFERENCE, traceId);
			values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
			values.put(ConnectionData.TYPE, wcd.getConnectionData().getType());
			values.put(ConnectionData.IP, wcd.getConnectionData().getIp());
			values.put(ConnectionData.BYTES_TRANSFERRED, wcd.getConnectionData().getBytesTransferred());
			values.put(ConnectionData.TOTAL_BYTES, wcd.getConnectionData().getTotalBytes());
			values.put(ConnectionData.TX_PACKETS, wcd.getConnectionData().getTxPackets());
			values.put(ConnectionData.RX_PACKETS, wcd.getConnectionData().getRxPackets());
			values.put(ConnectionData.RETRIES, wcd.getConnectionData().getRetries());
			//Log.w("DatabaseHelper", "++ " + " TX " + wcd.getConnectionData().getTxPackets() + " RX " + wcd.getConnectionData().getRxPackets() + " RET " + wcd.getConnectionData().getRetries());

			db.insertOrThrow(WifiConnectionData.TABLE_NAME, "", values);
			break;
		case WIFI_CONNECTION_INFO:
			WifiConnectionInfo wci = (WifiConnectionInfo) trace;
			int uid = wci.getConnectionInfo().getUid();
			if (!uidTotal.contains(uid) && (uid != -1)) {
				saveUid(db,uid);
				uidTotal.add(uid);
				//Log.e("uidTotal", String.valueOf(uidTotal));
			}
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(WifiConnectionInfo.TRACE_REFERENCE, traceId);
			values.put(WifiConnectionInfo.SNIFF_SEQUENCE, wci.getSniffSequence());
			values.put(ConnectionInfo.PROTOCOL, wci.getConnectionInfo().getProtocol());
			values.put(ConnectionInfo.LOCADD, wci.getConnectionInfo().getLocalAdd());
			values.put(ConnectionInfo.REMADD, wci.getConnectionInfo().getRemoteAdd());
			values.put(ConnectionInfo.LOCPORT, wci.getConnectionInfo().getLocalPort());
			values.put(ConnectionInfo.REMPORT, wci.getConnectionInfo().getRemotePort());
			values.put(ConnectionInfo.STATE, wci.getConnectionInfo().getConnectionState().toString());
			values.put(ConnectionInfo.UID, uid);

			db.insertOrThrow(WifiConnectionInfo.TABLE_NAME, "", values);
			break;
		case WIFI_SNIFFER_DATA:
			WifiSnifferData wsd = (WifiSnifferData) trace;
			//We save the wifi AP we are connected to
			wifiAPId = saveWifiAP(db, wsd.getConnectedTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(WifiSnifferData.TRACE_REFERENCE, traceId);
			values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
			values.put(SnifferData.RX_BYTES, wsd.getSnifferData().getRxBytes());
			values.put(SnifferData.TX_BYTES, wsd.getSnifferData().getTxBytes());
			values.put(SnifferData.TX_PACKETS, wsd.getSnifferData().getTxPackets());
			values.put(SnifferData.RX_PACKETS, wsd.getSnifferData().getRxPackets());
			values.put(SnifferData.RETRIES, wsd.getSnifferData().getRetries());
			values.put(WifiSnifferData.SNIFF_SEQUENCE, wsd.getSniffSequence());
			db.insertOrThrow(WifiSnifferData.TABLE_NAME, "", values);
			break;
		case BYTES_PER_UID:
			BytesperUid bpu = (BytesperUid) trace;
			values = new ContentValues();
			values.put(BytesperUid.TRACE_REFERENCE, traceId);
			values.put(BytesperUid.SNIFF_SEQUENCE, bpu.getSniffSequence());
			values.put(BytesperUid.UID, bpu.getUid());
			//values.put(BytesperUid.UID_NAME, bpu.getUidName());
			values.put(BytesperUid.TXBYTES, bpu.getTxBytes());
			values.put(BytesperUid.RXBYTES, bpu.getRxBytes());
			db.insertOrThrow(BytesperUid.TABLE_NAME, "", values);
			break;
		case WIFI_PING:
			WifiPing ping = (WifiPing) trace;
			//We save the wifi AP we are trying to connect to
			wifiAPId = saveWifiAP(db, ping.getConnectionTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(WifiPing.TRACE_REFERENCE, traceId);
			values.put(WifiAP.WIFI_AP_REFERENCE, wifiAPId);
			values.put(WifiPing.IP, ping.getPingedIp());
			values.put(WifiPing.SENT, ping.getPacketsSent());
			values.put(WifiPing.RECEIVED, ping.getPacketsReceived());
			values.put(WifiPing.RTT_MIN, ping.getRttMin());
			values.put(WifiPing.RTT_MAX, ping.getRttMax());
			values.put(WifiPing.RTT_AVG, ping.getRttAvg());
			values.put(WifiPing.RTT_MDEV, ping.getRttMdev());
			db.insertOrThrow(WifiPing.TABLE_NAME, "", values);
			break;
		case CELL_CONNECTION_EVENT:
			CellularConnectionEvent cConnectionEvent = (CellularConnectionEvent) trace;
			//We save the cell we are trying to connect to
			cellId = saveCell(db, cConnectionEvent.getConnectionTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(CellularConnectionEvent.TRACE_REFERENCE, traceId);
			values.put(Cell.CELL_REFERENCE, cellId);
			values.put(CellularConnectionEvent.EVENT, cConnectionEvent.getEvent());
			db.insertOrThrow(CellularConnectionEvent.TABLE_NAME, "", values);
			break;
		case CELL_CONNECTION_DATA:
			CellularConnectionData ccd = (CellularConnectionData) trace;
			//We save the cell we are connected to
			cellId = saveCell(db, ccd.getConnectedTo());
			//We save the rest (only thing left)
			values = new ContentValues();
			values.put(CellularConnectionData.TRACE_REFERENCE, traceId);
			values.put(Cell.CELL_REFERENCE, cellId);
			values.put(ConnectionData.TYPE, ccd.getConnectionData().getType());
			values.put(ConnectionData.IP, ccd.getConnectionData().getIp());
			values.put(ConnectionData.BYTES_TRANSFERRED, ccd.getConnectionData().getBytesTransferred());
			values.put(ConnectionData.TOTAL_BYTES, ccd.getConnectionData().getTotalBytes());
			values.put(ConnectionData.TX_PACKETS, ccd.getConnectionData().getTxPackets());
			values.put(ConnectionData.RX_PACKETS, ccd.getConnectionData().getRxPackets());
			values.put(ConnectionData.RETRIES, ccd.getConnectionData().getRetries());
			db.insertOrThrow(CellularConnectionData.TABLE_NAME, "", values);
			break;
		case CELL_SCAN_RESULT:
			CellularScanResult cScanResult = (CellularScanResult) trace;
			//Now we save the scan results in the corresponding table, using the Id of the trace        	
			for (Cell c : cScanResult.getResults()){
				cellId = saveCell(db, c);
				values = new ContentValues();
				values.put(CellularScanResult.TRACE_REFERENCE, traceId);
				values.put(Cell.CELL_REFERENCE, cellId);
				db.insertOrThrow(CellularScanResult.TABLE_NAME, "", values);
			}
			break;
		case EXTERNAL_EVENT:
			ExternalEvent externalEvent = (ExternalEvent) trace;
			values = new ContentValues();
			values.put(ExternalEvent.TRACE_REFERENCE, traceId);
			values.put(ExternalEvent.EVENT, externalEvent.getEvent());
			db.insertOrThrow(ExternalEvent.TABLE_NAME, "", values);
			break;
		}
	}



	private WifiAP getWifiAPById(Long wifiAPId) {
		Cursor c = dbh.getReadableDatabase().query(WifiAP.TABLE_NAME, null, KEY_NAME + " =? ", new String[]{ wifiAPId.toString() }, null, null, null);
		c.moveToFirst();
		WifiAP ret = WifiAP.getNewWifiAP(c.getString(c.getColumnIndex(WifiAP.BSSID)), c.getString(c.getColumnIndex(WifiAP.SSID)), 
				c.getInt(c.getColumnIndex(WifiAP.LEVEL)), c.getInt(c.getColumnIndex(WifiAP.CHANNEL)), 
				c.getString(c.getColumnIndex(WifiAP.CAPABILITIES)), c.getInt(c.getColumnIndex(WifiAP.LINK_SPEED)));
		c.close();
		return ret;
	}

	private Cell getCellById(Long cellId) {
		Cursor c = dbh.getReadableDatabase().query(Cell.TABLE_NAME, null, KEY_NAME + " =? ", new String[]{ cellId.toString() }, null, null, null);
		c.moveToFirst();
		Cell ret = Cell.getNewCell(c.getString(c.getColumnIndex(Cell.OPERATOR_NAME)), c.getString(c.getColumnIndex(Cell.OPERATOR)),
				c.getString(c.getColumnIndex(Cell.NET_TYPE)), c.getString(c.getColumnIndex(Cell.PHONE_TYPE)), 
				c.getInt(c.getColumnIndex(Cell.CID)), c.getInt(c.getColumnIndex(Cell.LAC)), 
				c.getInt(c.getColumnIndex(Cell.LEVEL_DBM)), intToBoolean(c.getInt(c.getColumnIndex(Cell.CURRENT))));
		c.close();
		return ret;
	}

	private boolean intToBoolean(int value) {
		return value != 0;
	}

	private class TraceIterator implements ITraceIterator{

		private DatabaseHelper dbh;
		private Cursor cursor;

		public TraceIterator(DatabaseHelper databaseHelper) {
			this.dbh = databaseHelper;
			this.cursor = dbh.getReadableDatabase().rawQuery(SELECT_ALL_TRACES, null);
			cursor.moveToFirst();
		}
		@Override
		public boolean hasNext() {
			return !cursor.isAfterLast();
		}
		@Override
		public Trace next() {
			Trace ret = null;
			Cursor c;			
			Long wifiAPId;
			WifiAP wifiAP;
			Long cellId;
			Cell cell;
			Long traceId = cursor.getLong(cursor.getColumnIndex(KEY_NAME));
			Trace mainTrace = TraceManager.getNewTrace(cursor.getLong(cursor.getColumnIndex(Trace.TIMESTAMP)), cursor.getDouble(cursor.getColumnIndex(Trace.ALTITUDE)), cursor.getDouble(cursor.getColumnIndex(Trace.LONGITUDE)), cursor.getDouble(cursor.getColumnIndex(Trace.LATITUDE)),
					cursor.getDouble(cursor.getColumnIndex(Trace.ACCURACY)), cursor.getFloat(cursor.getColumnIndex(Trace.SPEED)), cursor.getFloat(cursor.getColumnIndex(Trace.BEARING)), cursor.getString(cursor.getColumnIndex(Trace.PROVIDER)), cursor.getInt(cursor.getColumnIndex(Trace.BATT_LEVEL)));
			TraceType type = TraceType.valueOf(cursor.getString(cursor.getColumnIndex(Trace.TYPE)));

			switch (type){
			case WIFI_SCAN_RESULT:				
				List<WifiAP> results = new ArrayList<WifiAP>();
				//We get the WifiScan results
				c = dbh.getReadableDatabase().query(WifiScanResult.TABLE_NAME, null, WifiScanResult.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				//We iterate all the WifiScan results, and for each one we get the WifiAP
				if (c.moveToFirst()){
					do {
						wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
						wifiAP = getWifiAPById(wifiAPId);
						results.add(wifiAP);
					}while(c.moveToNext());
				}
				c.close();

				//Instantiate the Trace to return
				ret = WifiScanResult.getNewWifiScanResult(mainTrace, results);
				break;
			case WIFI_CONNECTION_EVENT:
				//We get the WifiAP and the event
				c = dbh.getReadableDatabase().query(WifiConnectionEvent.TABLE_NAME, null, WifiConnectionEvent.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the WifiAP the event is referencing
				wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
				wifiAP = getWifiAPById(wifiAPId);

				//We get the event now
				String event = c.getString(c.getColumnIndex(WifiConnectionEvent.EVENT));
				c.close();

				//Instantiate the Trace to return
				ret = WifiConnectionEvent.getNewWifiConnectionEvent(mainTrace, event, wifiAP);
				break;
			case COMMUNITY_NETWORK_CONNECTION_EVENT:
				//We get the WifiAP and the event
				c = dbh.getReadableDatabase().query(CommunityNetworkConnectionEvent.TABLE_NAME, null, CommunityNetworkConnectionEvent.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the WifiAP the event is referencing
				wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
				wifiAP = getWifiAPById(wifiAPId);

				//We get the event and username now
				String eventCN = c.getString(c.getColumnIndex(CommunityNetworkConnectionEvent.EVENT));
				String username = c.getString(c.getColumnIndex(CommunityNetworkConnectionEvent.USERNAME));
				c.close();

				//Instantiate the Trace to return
				ret = CommunityNetworkConnectionEvent.getNewCommunityNetworkConnectionEvent(mainTrace, eventCN, wifiAP, username);
				break;
			case WIFI_CONNECTION_INFO:
				//We get the WifiAP, ip and data
				c = dbh.getReadableDatabase().query(WifiConnectionInfo.TABLE_NAME, null, WifiConnectionInfo.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();

				//We get the rest
				String protocol = c.getString(c.getColumnIndex(ConnectionInfo.PROTOCOL));
				String localAdd = c.getString(c.getColumnIndex(ConnectionInfo.LOCADD));
				String remoteAdd = c.getString(c.getColumnIndex(ConnectionInfo.REMADD));
				int localPort = c.getInt(c.getColumnIndex(ConnectionInfo.LOCPORT));
				int remotePort = c.getInt(c.getColumnIndex(ConnectionInfo.REMPORT));
				ConnectionState connectionState	= ConnectionState.valueOf(c.getString(c.getColumnIndex(ConnectionInfo.STATE)));
				c.close();
				long infoSniffSequence = c.getLong(c.getColumnIndex(WifiConnectionInfo.SNIFF_SEQUENCE));
				int conUid = c.getInt(c.getColumnIndex(ConnectionInfo.UID));

				//Instantiate the Trace to return
				ret = WifiConnectionInfo.getNewWifiConnectionInfo(mainTrace, infoSniffSequence, ConnectionInfo.getNewConnectionInfo(protocol, localAdd, remoteAdd, localPort, remotePort, connectionState, conUid));
				break;
			case WIFI_SNIFFER_DATA:
				//We get the WifiAP, ip and data
				c = dbh.getReadableDatabase().query(WifiSnifferData.TABLE_NAME, null, WifiSnifferData.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the WifiAP 
				wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
				wifiAP = getWifiAPById(wifiAPId);

				//We get the rest
				int txBytes = c.getInt(c.getColumnIndex(SnifferData.TX_BYTES));
				int rxBytes = c.getInt(c.getColumnIndex(SnifferData.RX_BYTES)); 
				int rxPackets = c.getInt(c.getColumnIndex(SnifferData.TX_PACKETS));
				int txPackets = c.getInt(c.getColumnIndex(SnifferData.RX_PACKETS));
				int retriesSniffer = c.getInt(c.getColumnIndex(SnifferData.RETRIES));
				String Sniffip = c.getString(c.getColumnIndex(SnifferData.IP));
				long sniffSequence = c.getLong(c.getColumnIndex(WifiSnifferData.SNIFF_SEQUENCE));

				//Log.w("TraceIterator", "++ " + " TX " + tx + " RX " + rx + " RET " + retries);

				c.close();

				//Instantiate the Trace to return
				ret = WifiSnifferData.getNewWifiSnifferData(mainTrace, wifiAP, SnifferData.getNewSnifferData(txBytes, rxBytes, txPackets, rxPackets, retriesSniffer, Sniffip), sniffSequence);
				break;
			case BYTES_PER_UID:
				//We get the WifiAP, ip and data
				c = dbh.getReadableDatabase().query(BytesperUid.TABLE_NAME, null, BytesperUid.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the rest
				long bytesSniffSequence = c.getLong(c.getColumnIndex(BytesperUid.SNIFF_SEQUENCE));
				int uid = c.getInt(c.getColumnIndex(BytesperUid.UID));
				//String uidName = c.getString(c.getColumnIndex(BytesperUid.UID_NAME));
				long uidTxBytes = c.getLong(c.getColumnIndex(BytesperUid.TXBYTES));
				long uidRxBytes = c.getLong(c.getColumnIndex(BytesperUid.RXBYTES)); 

				c.close();

				//Instantiate the Trace to return
				ret = BytesperUid.getNewBytesperUid(mainTrace, bytesSniffSequence, uid, uidTxBytes, uidRxBytes);
				break;
			case WIFI_CONNECTION_DATA:
				//We get the WifiAP, ip and data
				c = dbh.getReadableDatabase().query(WifiConnectionData.TABLE_NAME, null, WifiConnectionData.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the WifiAP 
				wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
				wifiAP = getWifiAPById(wifiAPId);

				//We get the rest
				String typeConnection = c.getString(c.getColumnIndex(ConnectionData.TYPE));
				String ip = c.getString(c.getColumnIndex(ConnectionData.IP));
				int bytesTransferred = c.getInt(c.getColumnIndex(ConnectionData.BYTES_TRANSFERRED));
				int totalBytes = c.getInt(c.getColumnIndex(ConnectionData.TOTAL_BYTES));
				int tx = c.getInt(c.getColumnIndex(ConnectionData.TX_PACKETS));
				int rx = c.getInt(c.getColumnIndex(ConnectionData.RX_PACKETS));
				int retries = c.getInt(c.getColumnIndex(ConnectionData.RETRIES));

				//Log.w("TraceIterator", "++ " + " TX " + tx + " RX " + rx + " RET " + retries);

				c.close();

				//Instantiate the Trace to return
				ret = WifiConnectionData.getNewWifiConnectionData(mainTrace, wifiAP, ConnectionData.getNewConnectionData(ip, bytesTransferred, totalBytes, typeConnection, tx, rx, retries));
				break;
			case WIFI_PING:
				//We get the WifiAP 
				c = dbh.getReadableDatabase().query(WifiPing.TABLE_NAME, null, WifiPing.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the WifiAP the event is referencing
				wifiAPId = c.getLong(c.getColumnIndex(WifiAP.WIFI_AP_REFERENCE));
				wifiAP = getWifiAPById(wifiAPId);

				String pingedIp = c.getString(c.getColumnIndex(WifiPing.IP));
				int sent = c.getInt(c.getColumnIndex(WifiPing.SENT));
				int received = c.getInt(c.getColumnIndex(WifiPing.RECEIVED));
				float rttMin = c.getFloat(c.getColumnIndex(WifiPing.RTT_MIN));
				float rttMax = c.getFloat(c.getColumnIndex(WifiPing.RTT_MAX));
				float rttAvg = c.getFloat(c.getColumnIndex(WifiPing.RTT_AVG));
				float rttMdev = c.getFloat(c.getColumnIndex(WifiPing.RTT_MDEV));
				c.close();

				//Instantiate the Trace to return
				ret = WifiPing.getNewWifiPing(mainTrace, pingedIp, sent, received, rttMin, rttMax, rttAvg, rttMdev, wifiAP);
				break;
			case CELL_CONNECTION_EVENT:
				//We get the cell and the event
				c = dbh.getReadableDatabase().query(CellularConnectionEvent.TABLE_NAME, null, CellularConnectionEvent.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the cell the event is referencing
				cellId = c.getLong(c.getColumnIndex(Cell.CELL_REFERENCE));
				cell = getCellById(cellId);

				//We get the event now
				String eventCell = c.getString(c.getColumnIndex(CellularConnectionEvent.EVENT));
				c.close();

				//Instantiate the Trace to return
				ret = CellularConnectionEvent.getNewCellularConnectionEvent(mainTrace, eventCell, cell);
				break;
			case CELL_CONNECTION_DATA:
				c = dbh.getReadableDatabase().query(CellularConnectionData.TABLE_NAME, null, CellularConnectionData.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();
				//We get the Cell 
				cellId = c.getLong(c.getColumnIndex(Cell.CELL_REFERENCE));
				cell = getCellById(cellId);

				//We get the data and ip
				String typeConnectionCell = c.getString(c.getColumnIndex(ConnectionData.TYPE));
				String ipCell = c.getString(c.getColumnIndex(ConnectionData.IP));
				int bytesTransferredCell = c.getInt(c.getColumnIndex(ConnectionData.BYTES_TRANSFERRED));
				int totalBytesCell = c.getInt(c.getColumnIndex(ConnectionData.TOTAL_BYTES));
				int txCell = c.getInt(c.getColumnIndex(ConnectionData.TX_PACKETS));
				int rxCell = c.getInt(c.getColumnIndex(ConnectionData.RX_PACKETS));
				int retriesCell = c.getInt(c.getColumnIndex(ConnectionData.RETRIES));

				c.close();

				//Instantiate the Trace to return
				ret = CellularConnectionData.getNewCellularConnectionData(mainTrace, cell, ConnectionData.getNewConnectionData(ipCell, bytesTransferredCell, totalBytesCell, typeConnectionCell, txCell, rxCell, retriesCell));
				break;
			case CELL_SCAN_RESULT:				
				List<Cell> resultsCell = new ArrayList<Cell>();
				//We get the WifiScan results
				c = dbh.getReadableDatabase().query(CellularScanResult.TABLE_NAME, null, CellularScanResult.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				//We iterate all the WifiScan results, and for each one we get the WifiAP
				if (c.moveToFirst()){
					do {
						cellId = c.getLong(c.getColumnIndex(Cell.CELL_REFERENCE));
						cell = getCellById(cellId);
						resultsCell.add(cell);
					}while(c.moveToNext());
				}
				c.close();

				//Instantiate the Trace to return
				ret = CellularScanResult.getNewCellularScanResult(mainTrace, resultsCell);
				break;
			case EXTERNAL_EVENT:
				c = dbh.getReadableDatabase().query(ExternalEvent.TABLE_NAME, null, ExternalEvent.TRACE_REFERENCE + " =? ", new String[] { traceId.toString() }, null, null, null);
				c.moveToFirst();

				//We get the event now
				String extEvent = c.getString(c.getColumnIndex(ExternalEvent.EVENT));
				c.close();

				//Instantiate the Trace to return
				ret = ExternalEvent.getNewExternalEvent(mainTrace, extEvent);
				break;
			}
			//Move cursor to next Trace
			cursor.moveToNext();

			//return the result
			return ret;			
		}

		@Override
		public void close() {
			cursor.close();
		}

	}

	/*
	@Override
	public long saveWifiScanResult(WifiScanResult result){
		return saveTraceByType(result, TraceType.WIFI_SCAN_RESULT);
	}

	@Override
	public long saveWifiConnectionEvent(WifiConnectionEvent wifiConnectionEvent) {
		return saveTraceByType(wifiConnectionEvent, TraceType.WIFI_CONNECTION_EVENT);
	}

	@Override
	public long saveCommunityNetworkConnectionEvent(
			CommunityNetworkConnectionEvent communityNetworkConnectionEvent) {
		return saveTraceByType(communityNetworkConnectionEvent, TraceType.COMMUNITY_NETWORK_CONNECTION_EVENT);
	}

	@Override
	public long saveWifiConnectionData(WifiConnectionData wifiConnectionData) {
		return saveTraceByType(wifiConnectionData, TraceType.WIFI_CONNECTION_DATA);
	}

	@Override
	public long saveWifiSnifferData(WifiSnifferData wifiSnifferData) {
		return saveTraceByType(wifiSnifferData, TraceType.WIFI_SNIFFER_DATA);
	}

	@Override
	public long saveWifiConnectionInfo(WifiConnectionInfo wifiConnectionInfo){
		return saveTraceByType(wifiConnectionInfo, TraceType.WIFI_CONNECTION_INFO);
	}

	@Override
	public long saveBytesperUid(BytesperUid bytesperUid) {
		return saveTraceByType(bytesperUid, TraceType.BYTES_PER_UID);
	}

	@Override
	public long saveWifiPing(WifiPing wifiPing) {
		return saveTraceByType(wifiPing, TraceType.WIFI_PING);
	}

	@Override
	public long saveCellularConnectionEvent(CellularConnectionEvent cellularConnectionEvent) {
		return saveTraceByType(cellularConnectionEvent, TraceType.CELL_CONNECTION_EVENT);
	}

	@Override
	public long saveCellularConnectionData(CellularConnectionData cellularConnectionData) {
		return saveTraceByType(cellularConnectionData, TraceType.CELL_CONNECTION_DATA);
	}

	@Override
	public long saveCellularScanResult(CellularScanResult cellularScanResult) {
		return saveTraceByType(cellularScanResult, TraceType.CELL_SCAN_RESULT);
	}


	@Override
	public long saveWifiExternalEvent(ExternalEvent externalEvent) {
		return saveTraceByType(externalEvent, TraceType.EXTERNAL_EVENT);
	}

	private long getCommunityNetworkIdByName(String name){
		long ret = INVALID_ID;
		Cursor c = dbh.getReadableDatabase().query(ICommunityNetwork.TABLE_NAME, null, ICommunityNetwork.NAME + " =? ", new String[] { name }, null, null, null);
		if (c.moveToFirst()){
			ret = c.getLong(c.getColumnIndex(KEY_NAME));
		}
		c.close();
		return ret;
	}*/


	public static boolean databaseFileExists(String packageName){
		String databaseFileName = getDBFileName(packageName);
		File file = new File(databaseFileName);
		return file.exists();
	}
}
