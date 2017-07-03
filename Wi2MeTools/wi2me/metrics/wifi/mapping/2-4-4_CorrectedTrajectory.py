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

import os

from wi2me.utils import MapHelper
from wi2me.utils import SVGHelper

from settings import TEMP_DIR

METRIC_NAME = "Correct GPS points by placing them on existing roads"
METRIC_CODE = "2.4.4"

DEFAULT_CONFIG = {"PLOT_ORIGINAL":True, 
			"CORRECTED_COLOR":"green", 
			"ORIGINAL_COLOR":"red", 
			"PRINT_ROADS":False, #Print the available road names in the file
			"ROADS_FILE":"", #Store the roads you want to limit the correction to, one road name by line
			'SCALE':True,
			'SCALE_METERS':100,
			'SCALE_CAPTION':"100m",
			'SCALE_COLOR':"black",
			'SCALE_POS_X':0.05,
			'SCALE_POS_Y':0.05,
		}

def plot(config):
	GPSPoints = []

	tracePoints = []
	
	mapHelper = MapHelper.MapHelper(config['data'])
	image_width, image_height = mapHelper.getDimensions()
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
	if image_width * image_height > 0:

		for source in config['data']:
			GPSPoints.append(source.getPoints())

		finalFile = config['outdir'] + "_" + METRIC_CODE + "_TrajectoryWithWifiAP.svg"
		background_path = mapHelper.getBackgroundImage()
		second_pass_path = TEMP_DIR + "./layers.svg"
		svgHelper = SVGHelper.SVGHelper(second_pass_path, image_width, image_height, mapHelper)

		#Do we have a road subset ?
		roadSel = []
		if len(config['ROADS_FILE']) > 0 and os.path.isfile(config['ROADS_FILE']):
			roadF = open(config['ROADS_FILE'])
			for r in roadF.readlines():
				roadSel.append(r.rstrip('\n'))

		#Main trajectory
		for points in GPSPoints:
			svgHelper.plotLineFromPoints(MapHelper.placePointsOnRoads(points, mapHelper.getRoadsFromOSM(roadSel = roadSel, printNames=config['PRINT_ROADS']))[0], 3, config["CORRECTED_COLOR"])
			if config["PLOT_ORIGINAL"]:
				svgHelper.plotLineFromPoints(points, 1, config["ORIGINAL_COLOR"])

		#Plot an horizontal line of config['SCALE_METERS'] length
		if config['SCALE']:
			wMeters = MapHelper.meterDist((box_lat_min, 0), (box_lat_max, 0))
			Startp_lat = box_lat_min + config['SCALE_POS_Y'] * (box_lat_max - box_lat_min) 
			Startp_long = box_long_min + config['SCALE_POS_X'] * (box_long_max - box_long_min) 
			Stopp_long = Startp_long + float(config['SCALE_METERS']) / wMeters * (box_long_max - box_long_min)
			Stopp_lat = Startp_lat

			svgHelper.plotLineFromPoints([(Startp_lat, Startp_long), (Stopp_lat, Stopp_long)], 4, config['SCALE_COLOR'])
			svgHelper.addText(config['SCALE_CAPTION'], (Startp_lat + 0.01 * (box_lat_max - box_lat_min), Startp_long) , 10, "Arial")


		#Merge both Files 
		finalFile = config['outdir'] + METRIC_CODE + "_CorrectedTrajectoryMapping.svg"
		SVGHelper.addOverlay(background_path, svgHelper.svgFile.tostring() , finalFile)
