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

METRIC_CODE = "2.4.7"
METRIC_NAME = "Map tiling of visitation times"


DEFAULT_CONFIG={
		'SMALL_SIDE_TILES':10,
		'ColorMap':"viridis"
	}



HTML_HEAD = "<!DOCTYPE HTML>\n\
    <html>\n\
	<head>\n\
		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n\
		<title>Your Website</title>\n\
		<style>\n\
.hotspot {\n\
    position: absolute;\n\
    border: 1px solid blue;\n\
}\n\
.hotspot + * {\n\
    pointer-events: none;\n\
    opacity: 0;\n\
}\n\
.hotspot:hover + * {\n\
    opacity: 1.0;\n\
}\n\
.wash {\n\
    position: absolute;\n\
    top: 0;\n\
    left: 0;\n\
    bottom: 0;\n\
    right: 0;\n\
    background-color: rgba(255, 255, 255, 0.6);\n\
}\n\
		</style>\n\
	</head>\n"


def plot(config):

    mapHelper = MapHelper.MapHelper(config['data'])

    box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
    background_path = mapHelper.getBackgroundImage(oType = OutputType.OTYPE_PNG)
    background_img = imread(open(background_path))

    Timestamps = {}
    
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
	        	ts = scan[0].detections[0].timestamp
		        if 0.0 not in GPS : 
			    matpoint = min([(x, y) for x in range(xSize) for y in range(ySize)], key = lambda u : MapHelper.meterDist(GPS,(box_lat_min + float(u[0] + 0.5) / xSize * (box_lat_max - box_lat_min), box_long_min + float(u[1] + 0.5) / ySize * (box_long_max - box_long_min))))

			    if matpoint not in Timestamps:
			        Timestamps[matpoint] = []
    
                            if ts not in Timestamps[matpoint]:
                                Timestamps[matpoint].append(ts)
        
    
    mat = np.zeros((ySize, xSize))
    mat[mat == 0.0] = np.nan
    for (x, y) in Timestamps:
	mat[y, x] = max(Timestamps[x, y]) - min(Timestamps[x, y])

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
    plt.imshow(np.rot90(mat), interpolation='none', cmap=plt.get_cmap(config['ColorMap']), alpha = 0.5, extent = [ 0, xDim, yDim, 0, ])

    day = 3600 * 1000 * 24
    week = day * 7
    month = day * 30
    year = day * 365
    
    barTicks = [0, day, week, week * 2, month, year]
    barTickLabels = ["0", "1 day", "1 week", "2 weeks", "1 month" , "1 year"]
     
    cbar = plt.colorbar(ticks=barTicks)
    cbar.ax.set_yticklabels(barTickLabels)
    
    output_path = config['outdir'] + METRIC_CODE + "_timespan_heatmap.svg"
    fig.savefig(output_path, bbox_inches='tight')

    #Plot the heatmap a second time, for overlaying stuff in a web page
    fig = plt.figure(frameon=False, figsize=(xDim,yDim))

    plt.imshow(background_img, extent = [ 0, xDim, yDim, 0, ])
    plt.imshow(np.rot90(mat), interpolation='none', cmap=plt.get_cmap(config['ColorMap']), alpha = 0.5, extent = [ 0, xDim, yDim, 0, ])

    day = 3600 * 1000 * 24
    week = day * 7
    month = day * 30
    year = day * 365
    
    #barTicks = [0, day, week, week * 2, month, year]
    #barTickLabels = ["0", "1 day", "1 week", "2 weeks", "1 month" , "1 year"]
     
    #cbar = plt.colorbar(ticks=barTicks)
    #cbar.ax.set_yticklabels(barTickLabels)
    
    plt.gca().set_axis_off()
    plt.subplots_adjust(top = 1, bottom = 0,
            left = 0, right = 1, 
            hspace = 0, wspace = 0,
            )
    plt.margins(0,0)
    
    plt.gca().xaxis.set_major_locator(plt.NullLocator())
    plt.gca().yaxis.set_major_locator(plt.NullLocator())

    resource_dirname = "_resources/"
    output_resdir_path = config['outdir'] + METRIC_CODE + resource_dirname
    os.mkdir(output_resdir_path)
    hmap_filename = "timespan_heatmap.svg"
    svg_output_path = output_resdir_path + hmap_filename
    fig.savefig(svg_output_path, bbox_inches='tight', pad_inches = 0)

    pixelsX, pixelsY = fig.get_size_inches()*fig.dpi

    html_output_path = config['outdir'] + METRIC_CODE + "_timespan_heatmap.html"
    html_output = open(html_output_path, 'w')

    html_output.write(HTML_HEAD)
    html_output.write("<body>")
    #html_output.write("<div class=\"imagedd\">\n\
    #            <img src=\"" + METRIC_CODE + resource_dirname + hmap_filename + "\" width = \"" + str(pixelsX) + "\" height = \"" + str(pixelsY) + "\"  usemap=\"#contentmap\" />\n \
    #	    <map name=\"contentmap\"> \n")
    html_output.write("<img src=\"" + METRIC_CODE + resource_dirname + hmap_filename + "\" width = \"" + str(pixelsX) + "\" height = \"" + str(pixelsY) + "\"/>\n")
    for i in range(xSize):
        x1 = int(float(i) / xSize * pixelsX)
        w = int(pixelsX / xSize)
        for j in range(ySize):
            y1 = int(float(j) / ySize * pixelsY)
            h = int(pixelsY / ySize)
            """html_output.write("<area class=\"image\" shape=\"rect\" coords=\"")
            html_output.write(str(x1) + "," + str(y1) + "," + str(x2) + "," + str(y2))
            html_output.write("\" alt=\"" + str((i, j)) + "\" href=\"castro." + str(x1) + " html\" > \n ")
            html_output.write("<div class=\"overlay\"> whaddddqqsdl lqlq lmqlm qlmqm qlmlmqlm qlmlm  " + str((i, j)) + "</div>\n")
            html_output.write("</area>\n")"""
              
            html_output.write("<div class=\"hotspot\" style=\"top: " + str(y1) + "px; left: " + str(x1) + "px; height: " + str(h) + "px; width: " + str(w) + "px;\"></div>\n")
            html_output.write("<div>\n")
            html_output.write("<div class=\"wash\"></div>\n")
            html_output.write("<div style=\"position: absolute; top: 0; left: 0;\">" + str((i, j)) + "</div>\n")
            html_output.write("</div>\n\n")

    #html_output.write("</map> \n\
    html_output.write("\n\
        </div>\n\
        </body>\n\
        </html>")

    #<div class="overlay">  <img src="CKdEQueWEAA9Pyj.png"/>   </div>
