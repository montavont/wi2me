#!/usr/bin/env python
# Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
#
# This file is part of Wi2Me.
#
# Wi2Me is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Wi2Me is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
#

#This tool parses an sqlite wi2me database and outputs it to csv

import sqlite3
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

CSV_SEP = "\t"

class wi2meEvent:
    EXTERNAL = 0
    SCAN_RESULT = "WIFI_SCAN_RESULT"
    CONNECTION_EVENT = 2
    CONNECTION_DATA = 3
    WIFI_DISCONNECTED = 4
    NEW_LOCATION = "LOCATION_EVENT"
    NEW_BATTERY_LEVEL = 6
    WIFI_ON = 7
    STARTING_CONF = 8

EventDescs = {
        wi2meEvent.EXTERNAL : "EXTERNAL_EVENT",
        wi2meEvent.SCAN_RESULT : "WIFI_SCAN_RESULT",
        wi2meEvent.CONNECTION_EVENT : "WIFI_CONNECTION_EVENT",
        wi2meEvent.CONNECTION_DATA : "WIFI_CONNECTION_DATA",
    wi2meEvent.WIFI_DISCONNECTED : "WIFI_DISCONNECTED",
    wi2meEvent.NEW_LOCATION : "NEW_LOCATION",
    wi2meEvent.NEW_BATTERY_LEVEL : "NEW_BATTERY_LEVEL",
    wi2meEvent.WIFI_ON : "WIFI_ON",
    wi2meEvent.STARTING_CONF : "STARTING.CONFIGURATION",
}

class dbParser:
    def __init__(self, path):
        self.path = path
        self.conn = sqlite3.connect(path)

    def queryEvents(self):
        c = self.conn.cursor()
        return c.execute('select * from Trace')

    def parseExternalEvent(self, eventId, pLine):
        retval = ""
        c = self.conn.cursor()
        data = c.execute('select * from ExternalEvent where TraceId = ' + str(eventId)).fetchone()
        _, _, eventType = data

        #if eventType == EventDescs[wi2meEvent.WIFI_DISCONNECTED]:
        #    retval += str(wi2meEvent.WIFI_DISCONNECTED)
        if eventType == EventDescs[wi2meEvent.NEW_LOCATION]:
            retval += str(wi2meEvent.NEW_LOCATION)
            _, _, altitude , longitude, latitude, accuracy, bearing, speed, provider, _, _ = pLine
            retval += CSV_SEP
            retval += str(provider)
            retval += CSV_SEP
            retval += str(altitude)
            retval += CSV_SEP
            retval += str(latitude)
            retval += CSV_SEP
            retval += str(longitude)
            retval += CSV_SEP
            retval += str(speed)
            retval += CSV_SEP
            retval += str(accuracy)
            retval += CSV_SEP
            retval += str(bearing)
        #elif eventType == EventDescs[wi2meEvent.NEW_BATTERY_LEVEL]:
        #    retval += str(wi2meEvent.NEW_BATTERY_LEVEL)
        #    retval += CSV_SEP
        #    batteryLevel = pLine[9]
        #    retval += str(batteryLevel)
        #elif eventType.startswith(EventDescs[wi2meEvent.WIFI_ON]):
        #    retval += str(wi2meEvent.WIFI_ON)
        #elif eventType.startswith(EventDescs[wi2meEvent.STARTING_CONF]):
        #    retval += str(wi2meEvent.STARTING_CONF)
        #    retval += CSV_SEP
        #    retval += eventType
        #else: TODO : CONNECTION MONITOR PARSING

        return retval
    
    def parseScanResult(self, eventId):
        retval = []
        c = self.conn.cursor()
        ap_ids = c.execute('select WifiApId from WifiScanResult where TraceId = ' + str(eventId)).fetchall()
        for ap_id in ap_ids:
            payload = ""
            ap_data = c.execute('select * from WifiAp where Id = ' + str(ap_id[0])).fetchone()
            #_, bssid, ssid, rssi, channel, speed, capabilities = ap_data
            for val in ap_data[1:]:
                payload += str(val)
                payload += CSV_SEP
            payload = payload.rstrip(CSV_SEP)
            retval.append(payload)

        return retval

    def parseEvent(self, eventId, ts, eventType, pLine):
        retval  = []
        c = self.conn.cursor()

        if eventType == EventDescs[wi2meEvent.EXTERNAL]:
            line = ""
            line += str(ts)
            line += CSV_SEP
            line += "100" #Battery....to remove
            line += CSV_SEP
            line += self.parseExternalEvent(eventId, pLine)
            retval.append(line)
        if eventType == EventDescs[wi2meEvent.SCAN_RESULT]:
            for resultPayload in self.parseScanResult(eventId):
                line = ""
                line += str(ts)
                line += CSV_SEP
                line += "100" #Battery....to remove
                line += CSV_SEP
                line += str(wi2meEvent.SCAN_RESULT)
                line += CSV_SEP
                line += resultPayload
                retval.append(line)

        return retval

for path in sys.argv[1:]:
    outF = open(path + ".csv", "w")
    parser = dbParser(path)
    data = parser.queryEvents()
    line = data.fetchone()
    while line is not None:
        eventId, ts, _, _, _, _, _, _, _, _, eventType = line
        eventLines = parser.parseEvent(eventId, ts, eventType, line)

        if len(eventLines) > 0:
            for l in eventLines:
                outF.write(l)
                outF.write("\n")

        line = data.fetchone()

    outF.close()
