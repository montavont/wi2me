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
from wi2me.utils.MapHelper import  OutputType

import numpy as np
import matplotlib.pyplot as plt
from scipy.misc import imread
from BeautifulSoup import BeautifulSoup

METRIC_CODE = "2.4.666"
METRIC_NAME = "Stuff that should be deleted"


DEFAULT_CONFIG={
		'SMALL_SIDE_TILES':10,
		'ColorMap':"viridis",
                'SINGLE':1, 
	}
def getAllNodes(path):
	soup = BeautifulSoup(open(path))
	
	Coordinates = {}

	for point in soup.osm.findAll('node'):
		Coordinates[point['id']] = (
                            float(point['lon']),
                            float(point['lat'])
                            )

        
        return Coordinates


def getBuildingsFromOSM(path):
        retval = []
	Coordinates = getAllNodes(path)

	soup = BeautifulSoup(open(path))


	for bul in soup.osm.findAll(lambda node : node.name=="way" and node.findAll(k='building')):
		points = []
		
		nodes = bul.findAll('nd')
		for node in nodes:
			points.append(Coordinates[node['ref']])

		retval.append(points)

	return retval

def getBuildingsWallSizes(buils):
   
    WallSizes = []
 
    for b in buils:
        minX = min([u[0] for u in b])
        minY = min([u[1] for u in b])
        maxX = max([u[0] for u in b])
        maxY = max([u[1] for u in b])

        WallSizes.append(MapHelper.meterDist((minX, minY), (maxX, minY)))
        WallSizes.append(MapHelper.meterDist((minX, minY), (minX, maxY)))

    return WallSizes

def getWallSizeMatrix(buildings, pMapHelper, tiles = 10):
   
    wSizes = np.zeros((tiles, tiles))
    
    for x in range(1, tiles + 1):
        for y in range(1, tiles + 1):
            tileBuildings = []
            tlat = pMapHelper.box_lat_min + float(y) / tiles * (pMapHelper.box_lat_max - pMapHelper.box_lat_min)
            tlon = pMapHelper.box_long_min + float(x) / tiles * (pMapHelper.box_long_max - pMapHelper.box_long_min)
            for bInd, b in enumerate(buildings):
                if max([u[0] for u in b]) < tlon and max([u[1] for u in b]) < tlat:
                    tileBuildings.append(buildings.pop(bInd))
    
            #wSizes[x - 1, y - 1] = np.mean(getBuildingsWallSizes(tileBuildings))
            wSizes[tiles - y, x - 1] = len(tileBuildings)

    wSizes[np.where(wSizes == 0.0)] = np.nan
    print wSizes

    return wSizes

def plot(config):
    source = config['data'][0]
    mapHelper = MapHelper.MapHelper([source])

    box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
    background_path = mapHelper.getBackgroundImage(oType = OutputType.OTYPE_PNG)
    background_img = imread(open(background_path))

    buildings = getBuildingsFromOSM(mapHelper.osm_data_path)
    
    yDist = MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_min, box_long_max))	
    xDist = MapHelper.meterDist((box_lat_min, box_long_min), (box_lat_max, box_long_min))	

    if yDist > 0 and xDist > 0:
        if xDist < yDist:
	    ySize = int(yDist / xDist * config['SMALL_SIDE_TILES'])
	    xSize = config['SMALL_SIDE_TILES']
	else:
	    ySize = config['SMALL_SIDE_TILES']
	    xSize = int(xDist / yDist * config['SMALL_SIDE_TILES'])

    mat = getWallSizeMatrix(buildings, mapHelper)

    xDim = 0
    yDim = 0
    if xDist > yDist:
        yDim = 10
        xDim = 10 * yDist / xDist
    else:
        xDim = 10
        yDim = 10 * xDist / yDist


    #Plot the heatmap in a classical fashion
    fig = plt.figure(figsize=(xDim,yDim))

    plt.imshow(background_img, extent = [ 0, xDim, yDim, 0, ])
    #plt.imshow(np.rot90(mat), interpolation='none', cmap=plt.get_cmap(config['ColorMap']), alpha = 0.5, extent = [ 0, xDim, yDim, 0, ])
    plt.imshow(mat, interpolation='none', cmap=plt.get_cmap(config['ColorMap']), alpha = 0.5, extent = [ 0, xDim, yDim, 0, ])
    cbar = plt.colorbar()

    output_path = config['outdir'] + METRIC_CODE + "_stuff_heatmap.svg"
    fig.savefig(output_path, bbox_inches='tight')
