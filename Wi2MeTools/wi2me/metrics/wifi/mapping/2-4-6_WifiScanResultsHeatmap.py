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
from wi2me.utils.MapHelper import OutputType
from scipy.misc import imread

import numpy as np
import matplotlib.pyplot as plt

METRIC_CODE = "2.4.6"
METRIC_NAME = "Wifi Scanresult Heatmap"

DEFAULT_CONFIG={
		'SMALL_SIDE_TILES':60,
		'ColorMap':"viridis"
	}


def plot(config):

	mapHelper = MapHelper.MapHelper(config['data'])
	
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()

	background_path = mapHelper.getBackgroundImage(oType = OutputType.OTYPE_PNG)
        background_img = imread(open(background_path))
	
	ScanResultSizes = {}
	SourceRegions = {}
	BestRssis = {}
	
	yDist = MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_min, box_long_max))	
	xDist = MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_max, box_long_min))	

        if yDist > 0 and xDist > 0:
		if xDist < yDist:
			ySize = int(yDist / xDist * config['SMALL_SIDE_TILES'])
			xSize = config['SMALL_SIDE_TILES']
		else:
			ySize = config['SMALL_SIDE_TILES']
			xSize = int(xDist / yDist * config['SMALL_SIDE_TILES'])

		for srcInd, source in enumerate(config['data']):
			for scan in source.getScanResults():
				if len(scan) > 0:
					GPS = scan[0].detections[0].GPS
					if 0.0 not in GPS : 
						matpoint = min([(x, y) for x in range(xSize) for y in range(ySize)], key = lambda u : MapHelper.meterDist(GPS,(box_lat_min + float(u[0]) / xSize * (box_lat_max - box_lat_min), box_long_min + float(u[1]) / ySize * (box_long_max - box_long_min))))

						if matpoint not in ScanResultSizes:
							ScanResultSizes[matpoint] = []
						ScanResultSizes[matpoint].append(len(scan))
					
						if matpoint not in SourceRegions:
							SourceRegions[matpoint] = []
						if source.path not in SourceRegions[matpoint]:
							SourceRegions[matpoint].append(source.path)

		#Scanresult HEatmap
		mat = np.zeros((ySize, xSize))
		mat[mat == 0.0] = np.nan
		for (x, y) in ScanResultSizes:
			#if x < xSize and y < ySize and x > 0 and y > 0:
			mat[y, x] = np.mean(ScanResultSizes[x, y])
				
		fig, ax = plt.subplots()

                plt.imshow(background_img, extent = [ 0, ySize, xSize, 0, ])

		plt.imshow(np.rot90(mat), interpolation='bicubic', cmap=plt.get_cmap(config['ColorMap']))
		plt.colorbar()	
		plt.grid()

		plt.tight_layout()
		plt.savefig(config['outdir'] + METRIC_CODE + "_WifiScanResultHeatMap.svg")


		#Trace visit Heatmap
		mat = np.zeros((ySize, xSize))
		mat[mat == 0.0] = np.nan
		for (x, y) in SourceRegions:
			mat[y, x] = len(set(SourceRegions[x, y]))
				
		fig, ax = plt.subplots()

                plt.imshow(background_img, extent = [ 0, ySize, xSize, 0, ])
		plt.imshow(np.rot90(mat), interpolation='bicubic', cmap=plt.get_cmap(config['ColorMap']))
		plt.colorbar()	
		plt.grid()

		plt.tight_layout()

		plt.savefig(config['outdir'] + METRIC_CODE + "_SourceExploration.svg")

