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
from wi2me.utils import MapHelper
from wi2me.utils import SVGHelper

from settings import TEMP_DIR

METRIC_NAME = "Wifi single BSSID detection point display"
METRIC_CODE = "2.4.5"

START_EVENT = "CONNECTION_START"
DHCP_EVENT = "OBTAINING_IPADDR"
CONNECTED_EVENT = "CONNECTED"
DISCONNECTED_EVENT = "DISCONNECTED"

DEFAULT_CONFIG = {
		'SINGLE':1,
		'SIZE_THRESHOLD':15
		}


def plot(config):
	dbMan = config['db']


	mapHelper = MapHelper.MapHelper(dbMan)
	
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
	image_width, image_height = mapHelper.getMapBoxForMinimalDimension()
	
	c, _ = dbMan.getDBCursor()

	MAC_POINTS = {}
	BSSID_TRANSLATOR = {}

	while c:

		#Connection Data Event
		conStartRows = c.execute("SELECT longitude, latitude, bssid, ssid from Trace tr, WifiScanResult res, WifiAP ap where ap.id == res.wifiapid and tr.id == res.wifiapid").fetchall()
		
		for lon, lat, bssid, ssid in conStartRows:
			if MAC_POINTS.has_key(bssid):
				MAC_POINTS[bssid].append([lat, lon]) 
			else:
				MAC_POINTS[bssid] =  [[lat, lon]]

			BSSID_TRANSLATOR[bssid] = ssid

		c, _ = dbMan.getDBCursor()


	finalFile = config['outdir'] + "_" + METRIC_CODE + "_TrajectoryWithWifiAP.svg"
	
	background_path = mapHelper.getBackgroundImage()
	
	second_pass_path = TEMP_DIR + "./layers.svg"


	for mac in MAC_POINTS.keys():
		if len(MAC_POINTS[mac]) > config['SIZE_THRESHOLD']:
			svgHelper = SVGHelper.SVGHelper(second_pass_path, image_width, image_height, mapHelper)
			svgHelper.plotPoints(MAC_POINTS[mac], 13, "blue", "blue")
	
			finalFile = config['outdir'] + METRIC_CODE + "_SingleAPMap_" + BSSID_TRANSLATOR[mac] + "_" + mac + ".svg"
			SVGHelper.addOverlay(background_path, svgHelper.svgFile.tostring() , finalFile)
