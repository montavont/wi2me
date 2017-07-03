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

METRIC_CODE = "2.4.2"
METRIC_NAME = "Wifi Scanresult Tiling"


DEFAULT_CONFIG={'SMALL_SIDE_TILES':10,
		'NUM_LEVELS':5,
		'LEVEL_COLORS':[ "rgb(0,200,0)", "rgb(0,150,0)", "rgb(0,100,0)", "rgb(0,75,0)", "rgb(0,50,0)" ],
		"BEST_RSSI":False,
	}


def plot(config):

	mapHelper = MapHelper.MapHelper(config['data'])
	
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
	image_width, image_height = mapHelper.getDimensions()
	
	background_path = mapHelper.getBackgroundImage()
	
	##################################################################################################
	#		ScanResult size depending on tiling
	##################################################################################################
	
	second_pass_path = TEMP_DIR + "./layers.svg"
	svgHelper = SVGHelper.SVGHelper(second_pass_path, image_width, image_height, mapHelper)
	
	ScanResultSizes = {}
	BestRssis = {}
	
	for [lat_min, lat_max, long_min, long_max] in mapHelper.getMapTiling(config['SMALL_SIDE_TILES']):
		scans = []
		rssis = []
		for source in config['data']:
			for scan in source.getScanResults(gpsRange = [lat_min, lat_max, long_min, long_max]):
				scans.append(len(scan))
				if len(scan) > 0:
					rssis.append(max(s.detections[0].rssi for s in scan))

		if len(scans) > 0 and len(rssis) > 0:
			mean = 0.0
			for s in scans:
				mean += s
	
			mean /= len(scans)
			ScanResultSizes[lat_min, long_min] = round(mean, 2)
			BestRssis[lat_min, long_min] = max(rssis)

	if len(ScanResultSizes.values()) > 0 :

		level_size = (max(ScanResultSizes.values()) - min(ScanResultSizes.values())) / (config['NUM_LEVELS'] - 1)
		level_size = max(level_size, 1)
		
		for [lat_min, lat_max, long_min, long_max] in mapHelper.getMapTiling(config['SMALL_SIDE_TILES']):
			square_side = long_max -long_min
			if ScanResultSizes.has_key((lat_min, long_min)):
				meanApFound = ScanResultSizes[lat_min, long_min]
		
				colorIndex = int((meanApFound - min(ScanResultSizes.values())) / level_size)
				tileText = str(meanApFound)
				if config['BEST_RSSI']:
					tileText += "	"
					tileText += str(BestRssis[lat_min, long_min])

				svgHelper.plotRectangle(lat_min, lat_max, long_min, long_max, config['LEVEL_COLORS'][colorIndex], 0.5)
				svgHelper.addText(tileText, (lat_min + square_side / 2, long_min + square_side / 2) , 5)
		
		###Merge both Files !!!
		finalFile = config['outdir'] + METRIC_CODE + "_WifiScanResultSizeTiling.svg"
		SVGHelper.addOverlay(background_path, svgHelper.svgFile.tostring() , finalFile)
