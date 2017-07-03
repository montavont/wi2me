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
import numpy as np

from wi2me.utils import MapHelper 
from wi2me.utils import MatrixHelper 
from wi2me.utils import SVGHelper
from wi2me.model import APManager
from wi2me.model.CoverageGraph import coverageGraph

from settings import TEMP_DIR
		
import random
import copy
import math
import sys

import time

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from statsmodels.distributions.empirical_distribution import ECDF


METRIC_NAME = "Physical movement of station represented as graph depending on streets - Coverage set selection"
METRIC_CODE = "2.2.6"

DEFAULT_CONFIG = {
		'VERBOSE':False,
		"PRINT_ROADS":False, #Print the available road names in the file
		"ROADS_FILE":"", #Store the roads you want to limit the correction to in this file, one road name by line
		'SINGLE':1,  #TODO REMOVE WHEN IMPL FINISHED
		'NULL_FILTER':True,
		#"SSID_FILTERS": ['FreeWifi', 'orange', 'Bouygues Telecom Wi-Fi', 'IciWifi gratuit', 'eduroam'],
		"SSID_FILTERS": [],
		#'SSID_PREFIX_FILTERS':['SFR WiFi ', 'SFR_', 'Livebox-', 'NUMERICABLE-', 'Bbox-', 'NEUF_', 'freebox', "DartyBox_"],
		'SSID_PREFIX_FILTERS':[],
		'CHANNEL_FILTERS':[],
		"DETECTION_RSSI_FILTER":-75, #Limit detection points to this minimal rssi value
		"DISPLAY_RSSI":False,
		"DISPLAY_RSSI_LARGE":-85,
		"DISPLAY_APIDS":False,
		}

OUT_FILE_BASENAME = "_coverage_graph"
TEXT_DUMP_EXT = '.txt'
FIGURE_FORMAT = '.svg'

#Determine uncovered meters by counting empty lines in a matrix, with exception of the trailing and starting null lins (corresponding to uncovered positions)
def countUncoveredPositions(graph, apSelection):

	uncovered = 0
	explored = 0

	for road in graph.roads:
		for seg in road.segments:
			if 0 not in seg.matrix.shape:
				noN = np.nonzero(seg.matrix)[0]
				firstNonNull = min(noN)
				lastNonNull = max(noN)
				explored += lastNonNull - firstNonNull + 1
				for line in range(firstNonNull, lastNonNull):
					if len([det.nw.ap.id for cell in seg.matrix[line] if cell != 0 for det in cell if det.nw.ap.id in apSelection]) == 0:
						uncovered += 1
	return uncovered, explored

def computeOverlapping(graph, apSelection):

	overlaps = []

	for road in graph.roads:
		for seg in road.segments:
			if 0 not in seg.matrix.shape:
				for line in np.nonzero(seg.matrix)[0]:
					overlaps.append(len(set([det.nw.ap.id for cell in seg.matrix[line] if cell != 0 for det in cell if det.nw.ap.id in apSelection])))

	return np.mean(overlaps)


#TEMP : TODO move to an helper module
# determins GPS coords from linear abscisse
def pointFromAbsc(a, seg):
	start, stop = seg
	retval = []
	length = MapHelper.meterDist(start, stop)

	ratio = a / length

	for i in range(len(start)):
		retval.append(start[i] + ratio * (stop[i] - start[i]))
	
	return retval


def getHMSMS(ts):
	h, rest = divmod(ts, 3600000)
	m, rest = divmod(rest, 60000)
	s, ms = divmod(rest, 1000)
	return (h, m, s, ms)


#TODO : Compare with maphelper function
def placePointOnRoads(point, graf, roadIndexes):
	correctedPoint = [0, 0]
	minDist = float('inf')
	selectedRoad = -1

	closestRoadStart = 0
	linearAbsc = 0

	for roadIndex in roadIndexes:
		road = graf.roads[roadIndex]
		for segIdx, seg in enumerate(road.segments):
			dist, proj = MapHelper.geoPointLineDist(point, [seg.start, seg.stop], True)
			if dist < minDist:
				minDist = dist
				correctedPoint = proj
				selectedRoad = roadIndex
				closestRoadStart = segIdx
	
	linearAbsc = MapHelper.SequentialDist([graf.roads[selectedRoad].segments[closestRoadStart].start,correctedPoint]) #Distance on the segment
	
	return correctedPoint, selectedRoad, closestRoadStart, linearAbsc



def plotExplorationMap(graf, svg_path, w, h, hlpr):

	def getKnowledge(segment):
		#res are the keys of segment.scanResults : i.e the source indexes
		return len(set(res for res in segment.scanResults))


	background_path = hlpr.getBackgroundImage()
	second_pass_path = TEMP_DIR + "./layers.svg"


	MaxPasses = max([getKnowledge(seg) for road in graf.roads for seg in road.segments])
	artist = SVGHelper.SVGHelper(second_pass_path, w, h, hlpr)

	baseLineS = 2
	lineGrowth = 1
	MaxLine = 10

	for road in graf.roads:
		for seg in road.segments:
			klvl = float(getKnowledge(seg)) / MaxPasses
			#lineSize = int(klvl * MaxLine)
			lineSize = getKnowledge(seg) * lineGrowth + baseLineS
			if klvl > 0:
				color = "rgb("
				color += str(int(klvl * 255))
				color += ","
				color += "0"
				color += ","
				color += str(int((1 - klvl) * 255))
				color += ")"
				artist.plotLineFromPoints([seg.start, seg.stop], lineSize, color, plotSingles=True)

	artist.finish()

	SVGHelper.addOverlay(background_path, artist.svgFile.tostring() , svg_path)


def plotApBoundingBoxCDF(path, pDetections):
    apCells = {}
    boxSides = []
    ssids = {}

    for det in pDetections:
        if 0.0 not in det.GPS:
            if det.nw.ap.id not in apCells:
                apCells[det.nw.ap.id] = []
                ssids[det.nw.ap.id] = []
            apCells[det.nw.ap.id].append(det.GPS)
            ssids[det.nw.ap.id].append(det.nw.ssid)

    for apId in apCells:
        maxLat = max([u[0] for u in apCells[apId]])
        minLat = min([u[0] for u in apCells[apId]])
        maxLon = max([u[1] for u in apCells[apId]])
        minLon = min([u[1] for u in apCells[apId]])

        side1 = MapHelper.meterDist((maxLat, maxLon), (minLat, maxLon))
        side2 = MapHelper.meterDist((maxLat, maxLon), (maxLat, minLon))

        boxSides.append(side1)
        boxSides.append(side2)

    fig = plt.figure()

    cdf = ECDF(boxSides)
    plt.step(cdf.x, cdf.y)

    fig.savefig(path, bbox_inches = "tight")
    plt.close()


def plotApNearBoundingCircleCDF(path, pDetections, pointInCircleRatio = 0.8):
    apCells = {}
    radiuses = []
    ssids = {}

    for det in pDetections:
        if 0.0 not in det.GPS:
            if det.nw.ap.id not in apCells:
                apCells[det.nw.ap.id] = []
                ssids[det.nw.ap.id] = []
            apCells[det.nw.ap.id].append(det.GPS)
            ssids[det.nw.ap.id].append(det.nw.ssid)

    for apId in apCells:
        points = apCells[apId]
        acceptablePointIgnored = (1- pointInCircleRatio) * len(points)
        center_x = sum([u[0] for u in points]) / float(len(points))
        center_y = sum([u[1] for u in points]) / float(len(points))

        radius = 0

        while len(points) > acceptablePointIgnored:
            radius += 1
            points = [p for p in points if MapHelper.meterDist((center_x, center_y), p) > radius]

        if radius > 1000:
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
            print apCells[apId]
            print radius
            print set(ssids[apId])
            print "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

        radiuses.append(radius)

    fig = plt.figure()

    cdf = ECDF(radiuses)
    plt.step(cdf.x, cdf.y)

    fig.savefig(path, bbox_inches = "tight")
    plt.close()




class allApFilter:
	def __init__(self):
		self.name = "ALL"

	def filterAp(self, ap):
		return True

class ssidApFilter:
	def __init__(self, ssid):
		self.name = "SSID_MATCHER_" + ssid
		self.ssid = ssid.lower()

	def filterAp(self, ap):
		return len([nw for nw in ap.networks if self.ssid == nw.ssid.lower()]) > 0

class ssidApPrefixFilter:
	def __init__(self, prefix):
		self.name = "SSID_PREFIX_" + prefix
		self.prefix = prefix.lower()

	def filterAp(self, ap):
		return len([nw for nw in ap.networks if nw.ssid.lower().startswith(self.prefix)]) > 0

class channelApFilter:
	def __init__(self, channel):
		self.name = "CHANNEL_" + str(channel)
		self.channel = channel

	def filterAp(self, ap):
		return self.channel in [nw.channel for nw in ap.networks]
	


class DetectionFilter:
	def __init__(self, level):
		self.level = level

	def filterDet(self, det):
		return det.rssi >= self.level


#Beware, this function exploded beyond the limits of madness...
def plot(config):
	
	mapHelper = MapHelper.MapHelper(config['data'])

		
	# SPEEDING UP NEEDED HERE
	roads = mapHelper.getRoadsFromOSM(roadSel = [], printNames=config['PRINT_ROADS'])
	intersections = mapHelper.getIntersectionsFromOSM()

	if config['NULL_FILTER']:
		apFilters = [allApFilter()]
	else:
		apFilters = []

	for ssid in config['SSID_FILTERS']:
		apFilters.append(ssidApFilter(ssid))
	
	for pref in config['SSID_PREFIX_FILTERS']:
		apFilters.append(ssidApPrefixFilter(pref))

	for chan in config['CHANNEL_FILTERS']:
		apFilters.append(channelApFilter(chan))

	for apFilter in apFilters:
		print "processing filter " + apFilter.name

		
		apMgr = APManager.APManager(config['data'])

		# SPEEDING UP NEEDED HERE
		apMgr.bssGrouping()
		apMgr.ssidSplitting()
		

		if apFilter.name != "ALL":
			apMgr.applyAPFilter(apFilter.filterAp)

		if config["DETECTION_RSSI_FILTER"] > -200:
			detFilter = DetectionFilter(config["DETECTION_RSSI_FILTER"])
			apMgr.applyDetectionFilter(detFilter.filterDet)

                
                plotApBoundingBoxCDF(config['outdir'] + METRIC_CODE + OUT_FILE_BASENAME + "_" + apFilter.name + "_allAps_boundingBoxCDF.svg", [det for ap in apMgr.aps for nw in ap.networks if len(nw.ssid) > 0 for det in nw.detections])
                plotApNearBoundingCircleCDF(config['outdir'] + METRIC_CODE + OUT_FILE_BASENAME + "_" + apFilter.name + "_allAps_nearBoundingCircelCDF.svg", [det for ap in apMgr.aps for nw in ap.networks if len(nw.ssid) > 0for det in nw.detections])
                qsdlqsdlqsdqs

		walkedDist = np.sum([source.getTravelledDistance() for source in config['data']])
		expTime = "%i:%i:%i.%i" % getHMSMS(np.sum([source.getEndTime() - source.getStartTime() for source in config['data']]))
		
		image_width, image_height = mapHelper.getDimensions()

		#Do we have a road subset ?
		roadSel = []
		if len(config['ROADS_FILE']) > 0 and os.path.isfile(config['ROADS_FILE']):
			roadF = open(config['ROADS_FILE'])
			for r in roadF.readlines():
				roadSel.append(r.rstrip('\n'))
		# SPEEDING UP NEEDED HERE
		Graf = coverageGraph(roads, mapHelper, inter = intersections)
		
		# Gps ccordinate translator
		# This will let us call MapHelper.placePointOnRoadsWithAbscisse only once per physical points, and reuse its result for the network detections that occured in the same place
		ProjectorTranslator = {}
		#Also, get separates roads for each source in order to fasten up initial projections
		#Messas up road IDS
		#roadsBySource = []
		#for source in config['data']:
		#	roadsBySource.append(MapHelper.MapHelper([source]).getRoadsFromOSM())


		#Divide map in 'mapDiv' tiles on each dimension
		mapDiv = 6
		separatedRoads = {}
		overlappedRoads = {}


		box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
		latStep = abs(box_lat_max - box_lat_min) / mapDiv
		longStep = abs(box_long_max - box_long_min) / mapDiv

		def pointToTile(point, minP, steps):
			retval = (int(math.floor( (point[0] - minP[0]) / steps[0])), int(math.floor( (point[1] - minP[1]) / steps[1])))

			return retval


		for roadInd, road in enumerate(Graf.roads):
			for seg in road.segments:
				startT = pointToTile(seg.start, (box_lat_min, box_long_min), (latStep, longStep))
				stopT = pointToTile(seg.stop, (box_lat_min, box_long_min), (latStep, longStep))
				for i in range(startT[0], stopT[0] + 1):
					for j in range(startT[1], stopT[1] + 1 ):
						#These are OSM points, so can be out of our map range.
						if i in range(mapDiv) and j in range(mapDiv):
							if (i, j) not in separatedRoads:
								separatedRoads[i, j] = []
							separatedRoads[i, j].append(roadInd)

		if len(separatedRoads) > 0:
			for i in range(mapDiv):
				for j in range(mapDiv):
					if (i, j) not in separatedRoads:
						separatedRoads[i, j] = separatedRoads[min([(u, v) for u, v in separatedRoads], key = lambda x : math.pow(x[0] - i, 2) + math.pow(x[1] - j, 2))]

		###TEST OVERLAPPED ROADS 
		for i in range(mapDiv):
			for j in range(mapDiv):
				val = []
				if (0,0) in separatedRoads:
					val += separatedRoads[i, j]
				if i > 0:
					val += separatedRoads[i - 1, j]
					if j > 0:
						val += separatedRoads[i - 1, j - 1]
						val += separatedRoads[i, j - 1]

					if j < mapDiv - 1:
						val += separatedRoads[i - 1, j + 1]
						val += separatedRoads[i, j + 1]

				if i < mapDiv -1:
					val += separatedRoads[i + 1, j]
					if j > 0:
						val += separatedRoads[i + 1, j - 1]

					if j < mapDiv - 1:
						val += separatedRoads[i + 1, j + 1]
				overlappedRoads[i, j] = set(val)				

		#Parse data from each source and fill graphRoadSegments
		for apind, ap in enumerate(apMgr.aps):
			for nw in ap.networks:
				for det in nw.detections:
					if 0.0 not in det.GPS:
						if det.GPS not in ProjectorTranslator:
							ProjectorTranslator[det.GPS] = placePointOnRoads(det.GPS, Graf, overlappedRoads[pointToTile(det.GPS, (box_lat_min, box_long_min), (latStep, longStep))])
						(lat, lon), roadId, segIndex, absc = ProjectorTranslator[det.GPS]
		
						if det.sourceInd not in Graf.roads[roadId].segments[segIndex].scanResults:
							Graf.roads[roadId].segments[segIndex].scanResults[det.sourceInd] = {}

						if absc not in Graf.roads[roadId].segments[segIndex].scanResults[det.sourceInd]:
							Graf.roads[roadId].segments[segIndex].scanResults[det.sourceInd][absc] = []

						Graf.roads[roadId].segments[segIndex].scanResults[det.sourceInd][absc].append(det)
			sys.stdout.write(str(apind) + "/" + str(len(apMgr.aps)) + '\r')
			
		sys.stdout.write('\n')

		func = MatrixHelper.COLU_LII_MergeColorize
		Graf.generateMatrixes(func)
			
		plotExplorationMap(Graf, config['outdir'] + METRIC_CODE + OUT_FILE_BASENAME + "_" + apFilter.name + "_progressMap.svg", image_width, image_height, mapHelper)

		for algo in [MatrixHelper.multiCoverageGreedy, MatrixHelper.multiCoverageContinuous]:

			out_path = config['outdir'] + METRIC_CODE + OUT_FILE_BASENAME + "_" + apFilter.name + "_" + algo.__name__
			svgHelper = SVGHelper.SVGHelper(out_path + FIGURE_FORMAT, image_width, image_height, mapHelper)

			for road in roads:
				svgHelper.plotLineFromPoints(road, 1, "black")
			
				
			multiCov, uncovered = algo(Graf)
			#[seg.matrix for road in Graf.roads for seg in road.segments])
			print "COVERED with	" + str(len(multiCov)) + " APs"  + "(" + str(uncovered) + ")"


			bssidFile = open(out_path + "_bssids", 'w')
			aapIds = [u[0] for u in multiCov]	
			for ap in apMgr.aps:
				if ap.id in aapIds:
					for nw in ap.networks:
						#if len(config['SSID_FILTERS']) == 0 or nw.ssid.lower() in [fssid.lower() for fssid in config['SSID_FILTERS']]:
						if len(config['SSID_FILTERS']) == 0 or apFilter.filterAp(ap):
							bssidFile.write(nw.bssid)
							bssidFile.write('\n')
			bssidFile.close()

                        plotApBoundingBoxCDF(out_path + "_boundingBoxCDF.svg", [det for ap in apMgr.aps if ap.id in aapIds for nw in ap.networks for det in nw.detections])
                        plotApNearBoundingCircleCDF(out_path + "_nearBoundingCircleCDF.svg", [det for ap in apMgr.aps if ap.id in aapIds for nw in ap.networks for det in nw.detections])

			apColors = {}
			for apId, covVal in reversed(multiCov):
				color = "rgb(" + str(random.randint(64, 196)) + "," + str(random.randint(64, 196)) + "," + str(random.randint(64, 196)) + ")"
				#Avoid duplicate color
				while color in apColors.values():
					color = "rgb(" + str(random.randint(64, 196)) + "," + str(random.randint(64, 196)) + "," + str(random.randint(64, 196)) + ")"
		
				apColors[apId] = color


			
			# Write down the id of the Access point
			# if an ssidFilter was provided, write the two last bytes of the mac address of the selected network instead
			if config['DISPLAY_APIDS']:
				for ap in apMgr.aps:
					if ap.id in aapIds:
						prettyId = str(ap.id)
						if len(config['SSID_FILTER']) > 0:
							prettyId = ""
							for nw in ap.networks:
								if nw.ssid.lower() in [u.lower() for u in config['SSID_FILTER']]:
									prettyId += nw.bssid[12:]
									prettyId += " "
						# Print the id at the position returned by the maphelper LocateFunction
						# use 1 as a weight for each point
						printPoint = MapHelper.locateCenter([[1, det.GPS[0], det.GPS[1]] for nw in ap.networks for det in nw.detections if 0.0 not in det.GPS])
						svgHelper.addText(prettyId, printPoint, 24, "Arial", color = apColors[ap.id])
						


			#No use in redoing that, keep this copy to reuse in the incoming loop
			colorToPlotInit = {}
			for color in apColors.values():
				colorToPlotInit[color] = []

			#Enumerate Roads
			for roadIdx, road in enumerate(Graf.roads):
				#enumerate road segments
				for segIx, seg in enumerate(road.segments):
					colorToPlot = copy.deepcopy(colorToPlotInit)
					rssiToPlot = copy.deepcopy(colorToPlotInit)
					rssiScanWritten = []
					linkagePoints = copy.deepcopy(colorToPlotInit)


					#Plotting segment's internal points
					#enumerate segment's matrix by column
					for col in seg.matrix.T:
						#enumerate column
						for absc in np.nonzero(col)[0]:
							cell = col[absc]
							#Look for aps in our bssidSet (thus in bssidColor)
							for det in cell:
								if det.nw.ap.id in apColors:
									apKey = apColors[det.nw.ap.id]
									if len(colorToPlot[apKey]) == 0 or absc not in colorToPlot[apKey][-1]:
										#Append to the last list, if consecutive to a formerly found list of points, 
										if len(colorToPlot[apKey]) > 0 and colorToPlot[apKey][-1][-1] == absc - 1: 
											colorToPlot[apKey][-1].append(absc) 
											rssiToPlot[apKey][-1].append([det.rssi, det.scanInd])
										#Or add as a new list if a discontinuity was detected
										else:
											colorToPlot[apKey].append([absc])
											rssiToPlot[apKey].append([[det.rssi, det.scanInd]])

					#Great, all aps from our subset present on this segment were detected, let us plot them
					for color in colorToPlot:
						for listInd, apList in enumerate(colorToPlot[color]):
							#svgHelper.plotLineFromPoints([pointFromAbsc(absc, [seg.start, seg.stop]) for absc in apList], 5, color, plotSingles=True)
							abscPoints = [pointFromAbsc(absc, [seg.start, seg.stop]) for absc in apList]
							svgHelper.plotLineFromPoints(abscPoints, 5, color, plotSingles=True)
							if config['DISPLAY_RSSI']:
								for abscInd, abPoint in enumerate(abscPoints):
									rssi, rssiScInd = rssiToPlot[color][listInd][abscInd]
									size = 12
									if rssi >= config['DISPLAY_RSSI_LARGE']:
										size = 18
									if rssiScInd not in rssiScanWritten:
										svgHelper.addText(str(rssi), abPoint, size, "Arial")
										rssiScanWritten.append(rssiScInd)
								

					# TODO TODO : All this should be matrix filling instead of juste representation.

					#TODO : add N steps linkage trough empty matrixes (small roads were no datapoint was taken)
					#Plotting points linking up to the next segments
					#for nexRoad, nexSeg in seg.nSeg:
					for neighSeg in seg.nSeg:
						#Check if a bssid of our set is common to the last point of this segment, and the first point of the next segment
						currentNonZ = np.nonzero(seg.matrix)[0]
						nextNonZ = np.nonzero(neighSeg.matrix)[0]
							
						if len(currentNonZ) > 0 and len(nextNonZ) > 0:
							currentLastPos = max(currentNonZ)
							nextFirstPos = min(nextNonZ)
							for apId in set([det.nw.ap.id for dets in seg.matrix[currentLastPos] if dets> 0 for det in dets if det.nw.ap.id in apColors.keys()]):
								if apId in set([det.nw.ap.id for dets in neighSeg.matrix[nextFirstPos] if dets> 0 for det in dets]):
									apKey = apColors[apId]
									linkagePoints[apKey].append([pointFromAbsc(currentLastPos, [seg.start, seg.stop]), seg.stop])
									linkagePoints[apKey].append([seg.stop, pointFromAbsc(nextFirstPos, [neighSeg.start, neighSeg.stop])])

					#Plotting points from to the last segments 
					# TODO refacto, duplicated code
					# TODO needed ? This should symetrical ?
					#for preRoad, preSeg in seg.pSeg:
					for neighSeg in seg.pSeg:
						#Check if a bssid of our set is common to the last point of this segment, and the first point of the pret segment
						currentNonZ = np.nonzero(seg.matrix)[0]
						preNonZ = np.nonzero(neighSeg.matrix)[0]
							
						if len(currentNonZ) > 0 and len(preNonZ) > 0:
							currentFirstPos = min(currentNonZ)
							preLastPos = min(preNonZ)
							for apId in set([det.nw.ap.id for dets in  seg.matrix[currentFirstPos] if dets> 0 for det in dets if det.nw.ap.id in apColors.keys()]):
								if apId in set([det.nw.ap.id for dets in neighSeg.matrix[preLastPos] if dets> 0 for det in dets]):
									apKey = apColors[apId]
									linkagePoints[apKey].append([pointFromAbsc(currentFirstPos, [seg.start, seg.stop]), seg.start])
									linkagePoints[apKey].append([seg.start, pointFromAbsc(preLastPos, [neighSeg.start, neighSeg.stop])])


					#Great, all linkage points from our subset leaving this segment were detected, let us plot them
					for color in linkagePoints:
						for ptList in linkagePoints[color]:
							svgHelper.plotLineFromPoints(ptList, 5, color, plotSingles=True)
			
			svgHelper.finish()
	

			uncovered, explored = countUncoveredPositions(Graf, [u[0] for u in multiCov])

			out = open(out_path + TEXT_DUMP_EXT, 'w')
			out.write("Number of sources		" + str(len(config['data'])) + "\n")
			out.write("Coverage Algorithm	" + algo.__name__ + "\n")
			out.write("Columns Merging Function	" + func.__name__ + "\n")
			#out.write("Total Matrixes	" + str(np.sum([len(road.segments) for road in Graf.roads]) ) + "\n")
			out.write("Total Matrixes	" + str(len([seg for road in Graf.roads for seg in road.segments if 0 not in seg.matrix.shape])) + "\n")
			out.write("TotalLines	" + str(np.sum([seg.matrix.shape[0] for road in Graf.roads for seg in road.segments])) + "\n")
			out.write("Total Duration	" + expTime + "\n")

			#out.write("Coverage(%)	" + str(float(explored - uncovered) / explored * 100) + " %\n")# + str((NumLines - uncovered) * 100.0 / NumLines)  + "\n")
			out.write("Coverage(%)	NOTIMPL DUE TO APFILTER%\n")# + str((NumLines - uncovered) * 100.0 / NumLines)  + "\n")
			#out.write("Uncovered Meters	" + str(uncovered) + "\n")
			out.write("Uncovered Meters	NOTIMPL DUE TO APFILTER\n")
			out.write("Explored Meters	" + str(explored) + "\n")
			out.write("Walked Meters	" + str(walkedDist) + "\n")
			out.write("Available APs	" + str(len(apMgr.aps)) + "\n")
			out.write("Used APs	" + str(len(multiCov)) + "\n")
			if len(apMgr.aps) > 0:
				out.write("Used APs(%)	" + str(len(multiCov) * 100.0 / len(apMgr.aps)) + "\n")
			out.write("MeanOverlapping	" + str( computeOverlapping(Graf, [u[0] for u in multiCov])) +"\n")# + str(plotOverlapping(matrix, coveragePath)) + "\n")

			out.close()
