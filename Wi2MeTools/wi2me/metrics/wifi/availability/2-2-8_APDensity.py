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
import math

from wi2me.utils import MapHelper
from wi2me.utils import SVGHelper

from settings import TEMP_DIR

METRIC_NAME = "Compute the total number of discovered APs, and compare to the explored Area"
METRIC_CODE = "2.2.8"

DEFAULT_CONFIG = {
                    "PLOT_MAP":False, 
                    "EXPLORATION_RADIUS":100,
            	    "SINGLE":1,
                    "MIN_RSSI":-95,
		}


def dist(pt1, pt2):
    return math.sqrt(pow(pt1[0] - pt2[0], 2) + pow(pt1[1] - pt2[1], 2))


def plot(config):
        Bssids = []

	GPSPoints = [] # GPS Coordinates of traces
        CartesianMeterPoints = [] # cartesian coordinates of traces, in meters


        coverageCartPoints = [] # cartesian coordinates of coveres 1 square meter celles
        coverageGPSPoints = [] # GPS coordinates of said cells, for map representation
        

	mapHelper = MapHelper.MapHelper(config['data'])
	image_width, image_height = mapHelper.getDimensions()
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()

        meters_x = int(MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_max, box_long_min))) + 1
        meters_y = int(MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_min, box_long_max))) + 1

	if image_width * image_height > 0:

		for source in config['data']:
                        points = source.getPoints()
			GPSPoints.append(points)

                        CartesianMeterPoints += [
                                (
                                    MapHelper.meterDist((box_lat_min, pt[1]), pt),
                                    MapHelper.meterDist((pt[0], box_long_min), pt)
                                )
                                for pt in points
                        ]

                        Bssids += [u.bssid for u in source.getApList() if u.detections[0].rssi > config['MIN_RSSI']]
    

                #Sort the Trace points by their first dimension, that should speed up parsing (cf 'break' a few lines below)
                CartesianMeterPoints = sorted(CartesianMeterPoints, key = lambda x : x[0])

                #Much faster than listing all the points in a 50m radius of each GPS pos 
                for x in range(meters_x):
                    for y in range(meters_y):
                        for idx, pt in enumerate(CartesianMeterPoints):
                            if dist((x, y), pt) < config['EXPLORATION_RADIUS']:
                                coverageCartPoints.append((x, y))
                                coverageGPSPoints.append((box_lat_min + float(x) / meters_x * (box_lat_max - box_lat_min), box_long_min + float(y) / meters_y * (box_long_max - box_long_min)))
                                break

                """for pt in CartesianMeterPoints:
                    for x in range(int(math.ceil(pt[0] - config["EXPLORATION_RADIUS"])), int(math.floor(pt[0] + config["EXPLORATION_RADIUS"]))):
                        yrange = int(math.sqrt(pow(config["EXPLORATION_RADIUS"], 2) - math.pow(pt[0] - x, 2)))
                        for y in range(int(pt[1]) - yrange, int(pt[1]) + yrange):
                            if (x, y) not in coverageCartPoints:
                                coverageCartPoints.append((x, y))
                                coverageGPSPoints.append((box_lat_min + float(x) / meters_x * (box_lat_max - box_lat_min), box_long_min + float(y) / meters_y * (box_long_max - box_long_min)))"""


                ApCount = len(set([u[3:14] for u in Bssids]))

                if config['PLOT_MAP']:

			finalFile = config['outdir'] + "_" + METRIC_CODE + "_TrajectoryWithWifiAP.svg"
			background_path = mapHelper.getBackgroundImage()
			second_pass_path = TEMP_DIR + "./layers.svg"
			svgHelper = SVGHelper.SVGHelper(second_pass_path, image_width, image_height, mapHelper)

                        svgHelper.plotPoints(coverageGPSPoints, 2, "black")

			#Main trajectory
			for points in GPSPoints:
				svgHelper.plotLineFromPoints(points, 1, "red")

			#Plot an horizontal line of config['SCALE_METERS'] length
			wMeters = MapHelper.meterDist((box_lat_min, 0), (box_lat_max, 0))
			Startp_lat = box_lat_min + 1 * (box_lat_max - box_lat_min) 
			Startp_long = box_long_min + 1 * (box_long_max - box_long_min) 
			Stopp_long = Startp_long + float(100.0) / wMeters * (box_long_max - box_long_min)
			Stopp_lat = Startp_lat

			svgHelper.plotLineFromPoints([(Startp_lat, Startp_long), (Stopp_lat, Stopp_long)], 4, "black")
			svgHelper.addText("100m", (Startp_lat + 0.01 * (box_lat_max - box_lat_min), Startp_long) , 10, "Arial")

			#Merge both Files 
			finalFile = config['outdir'] + METRIC_CODE + "_CorrectedTrajectoryMapping.svg"
			SVGHelper.addOverlay(background_path, svgHelper.svgFile.tostring() , finalFile)


		outFile = open(config['outdir'] + METRIC_CODE + "_apDensity.txt", "w")
                outFile.write("AP Count " + str(ApCount) + '\n')
                outFile.write("Covered Area    " + str(len(coverageCartPoints)) + '\n')
                outFile.write("AP Density   " + str(float(ApCount) / len(coverageCartPoints)) + '\n')    
                outFile.close()
                
