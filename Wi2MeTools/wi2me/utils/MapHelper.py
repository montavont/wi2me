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


from __future__ import print_function
import os
from osmxapi.xapiquery import XapiQuery
from osmxapi import OsmXapi
from osmxapi import bbox
import mapnik2 as mapnik
import cairo 
from settings import TEMP_DIR
import numpy as np
from BeautifulSoup import BeautifulSoup
from math import radians, cos, sin, asin, sqrt
from enum import Enum

import math
import urllib2
import svgwrite

from wi2me.utils import SVGHelper

OUTER_BORDER = 0.05
MIN_MAP_DIMENSION = 1024
MAP_DIR = TEMP_DIR + "maps/"
#OSM_SERVER =  "overpass.osm.rambler.ru"
OSM_SERVER =  "overpass-api.de"
F_SEP = "_"

#OSM Static tile size
TILE_SIZE = 256

#OSM STATIC SERVER and stuff
BASE_URL = 'http://tile.openstreetmap.org/'
FILE_EXT = ".png"





#Earth Mensurations
CIRC_EARTH = 40075000
RADIUS_EARTH = 6367000

MAPTYPE_OSM = 1
MAPTYPE_STATIC = 2

class OutputType(Enum):
    OTYPE_SVG = 0
    OTYPE_PNG = 1

#########################################################################
# 			Center Calculation
#########################################################################

MIN_LEVEL = -101
def locateCenter(points):   

	apLat = 0
	apLong = 0
 	retval = [apLat, apLong]

	points = [p for p in points if p[0] > MIN_LEVEL]

	if len(points) > 1:
		weightSum = 0

		for data in points:
			weight, lat, lon = data
			weight = weight - MIN_LEVEL # Positive value...
			weightSum += weight
			apLong += weight * lon
			apLat += weight * lat
		
		apLat /= weightSum
		apLong /= weightSum

		retval = [apLat, apLong]
	elif len(points)==1:
		retval = points[0]

	return retval

#########################################################################
#		Points distances 
#########################################################################
#In Meters
def meterDist( u, v):
	
	lat1, lon1, lat2, lon2 = map(radians, [u[0], u[1], v[0], v[1]])

	dlon = lon2 - lon1 
	dlat = lat2 - lat1 

	a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
	dist = RADIUS_EARTH * 2 * asin(sqrt(a)) 

	return dist

#Max distance in meter from a point
def MaxDistanceFrom( origin, points):
	MaxDist = 0
	for i in range(0, len(points)):
		MaxDist = max(MaxDist, meterDist(origin, points[i]))
	return MaxDist

#Max distance in meter
def MaxDistance( points):
	MaxDist = 0
	for i in range(0, len(points)):
		for j in range(i, len(points)):
			MaxDist = max(MaxDist, meterDist(points[i], points[j]))
	return MaxDist

#Max distance without radial convertion
def MaxNormalDistance( points):
	MaxDist = 0
	for i in range(0, len(points)):
		for j in range(i, len(points)):
			MaxDist = max(MaxDist, ((points[i][0] - points[j][0])**2 + (points[i][1] - points[j][1])**2)**0.5 )

	return MaxDist

#Max distance from a point without radial norming
def MaxNormalDistanceFrom( origin, points):
	MaxDist = 0
	for i in range(0, len(points)):
		MaxDist = max(MaxDist, ((points[i][0] - origin[0])**2 + (points[i][1] - origin[1])**2)**0.5 )

	return MaxDist

#Sum the distance between points one after another to get the length of a path
def SequentialDist(points):
	retval = 0
	if len(points) > 0:
		start = points[0]
		for pt in points[1:]:
			retval += meterDist(start, pt)
			start = pt

	return retval


# Replace the zeros with the next data
# in order to fill for points where GPS was not available
#Does nothing for a list of (0, 0)
def cleanGPS(points):

	lastVal = (0, 0)
	replace = []

	for i, (lat, lon) in enumerate(points):
		if lat * lon != 0:
			lastVal = (lat, lon)
			for j in replace:
				points[j] = lastVal
			replace = []
		else:
			replace.append(i)

	for j in replace:
		points[j] = lastVal

	return points			

	
def geoPointLineDist(p, seg, testSegmentEnds=False):
	"""
	Minimum Distance between a Point and a Line
	Written by Paul Bourke,    October 1988
	http://astronomy.swin.edu.au/~pbourke/geometry/pointline/
	Slightly modified for wi2me integration
	"""

	x3,y3 = p
	(x1,y1),(x2,y2) = seg

	dx21 = (x2-x1)
	dy21 = (y2-y1)
	
	lensq21 = dx21*dx21 + dy21*dy21
	if lensq21 == 0:
	    #20080821 raise ValueError, "zero length line segment"
	    dy = y3-y1 
	    dx = x3-x1 
	    return (sqrt( dx*dx + dy*dy ), (x1, y1))  # return point to point distance

	u = (x3-x1)*dx21 + (y3-y1)*dy21
	u = u / float(lensq21)


	x = x1+ u * dx21
	y = y1+ u * dy21    

	if testSegmentEnds:
	    if u < 0:
	        x,y = x1,y1
	    elif u >1:
	        x,y = x2,y2
	

	dx30 = x3-x
	dy30 = y3-y

	return (np.sqrt( dx30*dx30 + dy30*dy30 ), (x,y))


#Given a point and a list of roads (each road is a list of points), compute the point's distance to each road (minimal distance from the point to each segment of the road) and select the minimal one.
#Return value is a threeplet : 
#	- original point's projection on the closest road's closest segment
#	- Index of the selected road
#	- Index of the last road point before inserting the point
#	- Optional : Linear abcsisse of the point's projection on the road (or 0 if disabled)
def placePointOnRoadsWithAbscisse(point, roads, computeDist = False):
	correctedPoint = [0, 0]
	minDist = float('inf')
	selectedRoad = -1

	closestRoadStart = 0
	linearAbsc = 0

	#for road in roads:
	#for roadIndex in range(len(roads)):
	for roadIndex, road in enumerate(roads):
		roadStart = road[0]
		for ptIdx, roadPoint in enumerate(road[1:]):
			dist, proj = geoPointLineDist(point, [roadStart, roadPoint], True)
			if dist < minDist:
				minDist = dist
				correctedPoint = proj
				selectedRoad = roadIndex
				#closestRoadStart = ptIdx + 1 # TKE CHECKL 
				closestRoadStart = ptIdx # TKE CHECKL 

			roadStart =  roadPoint
	if computeDist:
		#linearAbsc = SequentialDist(roads[selectedRoad][:closestRoadStart] + [correctedPoint]) #Distance on the road
		linearAbsc = SequentialDist([roads[selectedRoad][closestRoadStart],correctedPoint]) #Distance on the segmeng
	
	return correctedPoint, selectedRoad, closestRoadStart, linearAbsc


def placePointOnRoads(point, roads):
	return placePointOnRoadsWithAbscisse(point, roads)[:2]

def placePointsOnRoads(points, roads, backNForthSize = 10, sanitize=True):
	retval = [] #converted points by orthogonaly projected them on the closest road
	retRoads = [] #Also return the roads we selected, in order to reuse them
	RoadSequence = []

	if sanitize:
		points = SVGHelper.sanitizePoints(points)

	for p in points:
		placedPoint, selRoad = placePointOnRoads(p, roads)
		retval.append(placedPoint)
		RoadSequence.append(selRoad)



	#Parse Trajectory and look if we went back and forth at some point. This means the point was badly placed at a street intersection, so we replace it with a projection on the road we left and went back to.
	currentR = 0
	index = 0
	while index < len(RoadSequence) - backNForthSize:
		r = RoadSequence[index]
		if r != currentR and currentR in RoadSequence[index:]:
			nextRind = RoadSequence.index(currentR, index)
			#Went back and forth to the same street...
			if nextRind - index <= backNForthSize:
				for i in range(index, nextRind + 1):
					retval[i] = placePointOnRoads(points[i], [roads[currentR]])[0]
					RoadSequence[i] = currentR

				index = nextRind - 1
		currentR = r
		index += 1




	#Look road transition and check if an intersection between the exist. if so insert a point to avoid crossing a building. If no or more than one intersection exist, do nothing.
	#DISABLED - Results not satisfying, and creates points...
	"""Insertions = {}
	lastRoad = RoadSequence[0]
	for index in range(1, len(RoadSequence)):
		roadIndex = RoadSequence[index]
		if lastRoad == roadIndex:
			continue
		intersect = filter(lambda x: x in roads[roadIndex], roads[lastRoad])
		if len(intersect) == 1:
			Insertions[index] = intersect[0]
		lastRoad = roadIndex

	print ("____")
	print(len(retval))
	print ("____")
	for i in sorted(Insertions.keys(), reverse=True):
		print (i)
		retval.insert(i, Insertions[i])

	print ("____")
	print(len(retval))"""

	if len(roads) > 0:
		for r in set(RoadSequence):
			retRoads.append(roads[r])

	return retval, retRoads


############################################################################################
# OpenStreetMap Static API helper functions 
############################################################################################
def lonToTile(lon, zoom):
	fRetval = (lon + 180) / 360 * pow(2, zoom)
	retval = int(fRetval)

	return retval

def latToTile(lat, zoom):
	fRetval = (1 - math.log(math.tan(lat * math.pi/180) + 1 / math.cos(lat* math.pi/180)) / math.pi) /2 * pow(2, zoom)
	retval = int(fRetval)
	return retval

def getStaticUrlForPoint(lat, lon, zoom):
	X = lonToTile(lon, zoom)
	Y = latToTile(lat, zoom)
	target = BASE_URL + str(zoom) + "/" + str(X) + "/"  + str(Y) + FILE_EXT

	return target 

############################################################################################
# Actual MapHelper object
############################################################################################
class MapHelper:
	def __init__(self, sources):
		self.sources = sources
		self.minimalDimension = MIN_MAP_DIMENSION
		self.box_lat_max, self.box_lat_min, self.box_long_max, self.box_long_min = self.getMapBox()
		self.image_width, self.image_height  = self.getDimensions()
		self.osm_data_path = ""

		if not os.path.exists(MAP_DIR):
			os.makedirs(MAP_DIR)

	#########################################################################
	#		Database Box Delimitation
	#########################################################################

	def getMapBox(self):
		retval = [0, 0, 0, 0]
		
		#Area Size calculation
		lat_min, lat_max, long_min, long_max = self.sources[0].getExtremeCoordinates()


		for source in self.sources[1:]:
			loop_lat_min, loop_lat_max, loop_long_min, loop_long_max = source.getExtremeCoordinates()
			long_min = min( loop_long_min, long_min)
			long_max = max( loop_long_max, long_max)
			lat_min = min( loop_lat_min, lat_min)
			lat_max = max( loop_lat_max, lat_max)

		#Discard cases with a null dimension (non localized sources)
		if long_max > long_min and lat_max > lat_min:
		
			lat_diff = (lat_max - lat_min) * OUTER_BORDER
			long_diff = (long_max - long_min) * OUTER_BORDER

			box_lat_min = lat_min - lat_diff
			box_lat_max = lat_max + lat_diff
			box_long_min = long_min - long_diff
			box_long_max = long_max + long_diff
			retval = [box_lat_max, box_lat_min, box_long_max, box_long_min]


		return retval

	def getDimensions(self):
		retval = (0, 0)

		#Discard cases with a null dimension (non localized sources)
		if self.box_long_max > self.box_long_min and self.box_lat_max > self.box_lat_min:
			long_lat_ratio = (self.box_long_max - self.box_long_min)/(self.box_lat_max - self.box_lat_min)

			if long_lat_ratio < 1:
				image_width = self.minimalDimension
				image_height = int( image_width / long_lat_ratio )
			else:
				image_height = self.minimalDimension
				image_width = int( image_height *  long_lat_ratio )
			retval = (image_width, image_height)

		return retval
	

	def getMapTiling(self, numTiles):
		square_side = 0
		retval = []

		#Discard cases with a null dimension (non localized sources)
		if self.box_long_max > self.box_long_min and self.box_lat_max > self.box_lat_min:
			if self.box_long_max - self.box_long_min > self.box_lat_max - self.box_lat_min:
				square_side = (self.box_lat_max - self.box_lat_min)/ numTiles
			else:
				square_side = (self.box_long_max - self.box_long_min)/ numTiles
		
			x = self.box_lat_min
			while x < self.box_lat_max:
				y = self.box_long_min
				while y < self.box_long_max:
					retval.append([x, x + square_side, y, y + square_side])
					y += square_side
				x += square_side

		return retval

	def getTilingIndexForPoint(self, point, tiling):
		retval = -1
		for i in range(len(tiling)):
			tile = tiling[i]
			if tile[0] <= point [0] < tile[2] and tile[1] <= point [1] < tile[3]:
				retval = i
				break
		return retval

	#########################################################################
	#		Point coordinate translation (GPS to pix, GPS to meters from (min, min) corner)
	#########################################################################
	def convertPoint(self, point):
                u = 0
                v = 0
		if self.box_lat_max != self.box_lat_min and self.box_long_max != self.box_long_min:
     	            ## North Hemisphere
        	    v = (self.box_lat_max - point[0]) / (self.box_lat_max - self.box_lat_min) * self.image_height
		    u = (point[1] - self.box_long_min) / (self.box_long_max - self.box_long_min) * self.image_width

		return [u, v]


	def convertPoints(self, points):
		retval = []
		for p in points:
			retval.append(self.convertPoint(p))

		return retval

	def convertPointToMeters(self, point):
                u = 0
                v = 0
		if self.box_lat_max != self.box_lat_min and self.box_long_max != self.box_long_min:
                    u = meterDist((self.box_lat_min, self.box_long_min), (self.box_lat_min, point[1]))
                    v = meterDist((self.box_lat_min, self.box_long_min), (point[0], self.box_long_min))

		return [u, v]

	def convertPointsToMeters(self, points):
		retval = []
		for p in points:
			retval.append(self.convertPointToMeters(p))

		return retval


	#########################################################################
	#			Openstreetmap Info Querying
	#########################################################################

	def getMap(self):
		osm_data_path = MAP_DIR  + str(self.box_lat_max)  + F_SEP +  str(self.box_lat_min)  + F_SEP +  str(self.box_long_max)  + F_SEP +  str(self.box_long_min)  +  ".osm"

		if not os.path.isfile(osm_data_path):
			#xapi = OsmXapi(api=OSM_SERVER, base="cgi", debug = True)
			xapi = OsmXapi(api=OSM_SERVER, base="api", debug = True)

			map_box = bbox.Bbox(latn=self.box_lat_max, lats=self.box_lat_min, lone=self.box_long_max, lonw=self.box_long_min)

			map_data = XapiQuery()
			map_data.bbox(map_box)

			raw = xapi.anyGet(map_data, raw=True)

			osmFile = open(osm_data_path, "w")
			print(raw, file=osmFile)
			osmFile.flush()
			osmFile.close()

		self.osm_data_path = osm_data_path
		return osm_data_path

	#########################################################################
	#			Mapnik data rendering
	#########################################################################


	def makePolygonStyle(self, Filter, Color, Opacity):
		filter = mapnik.Filter(Filter)
		rule = mapnik.Rule()
		symbolizer = mapnik.PolygonSymbolizer()
		symbolizer.fill = Color
		symbolizer.fill_opacity = Opacity
		rule.symbols.append(symbolizer)
		rule.filter = filter
		return rule

	def makeLineStyle(self, Filter, Color, Opacity, Width):
		filter = mapnik.Filter(Filter)
		rule = mapnik.Rule()

		lineSymbolizer = mapnik.LineSymbolizer()
		stroke = mapnik.Stroke(Color, Opacity)
		stroke.line_cap = mapnik.line_cap.ROUND_CAP
		stroke.line_join = mapnik.line_join.ROUND_JOIN
		stroke.width = Width
		lineSymbolizer.stroke = stroke
		rule.symbols.append(lineSymbolizer)
		rule.filter = filter
		return rule

	def getBackgroundImage(self, mType=MAPTYPE_OSM, oType=OutputType.OTYPE_SVG):


		background_path = MAP_DIR  + str(self.box_lat_max)  + F_SEP +  str(self.box_lat_min)  + F_SEP +  str(self.box_long_max)  + F_SEP +  str(self.box_long_min)  + F_SEP +  str(mType) 
		box_lat_max, box_lat_min, box_long_max, box_long_min = self.getMapBox()
	
		if mType == MAPTYPE_OSM:	
			m = mapnik.Map(*self.getDimensions())
			bbox=(mapnik.Envelope(box_long_max, box_lat_max, box_long_min, box_lat_min))
			m.zoom_to_box(bbox)

			mapStyle = mapnik.Style()
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'residential'", mapnik.Color(208, 231, 176), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'construction'", mapnik.Color(100, 100, 100), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'grass'", mapnik.Color('green'), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'commercial'", mapnik.Color(208, 231, 176), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'basin'", mapnik.Color('blue'), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[landuse] = 'village_green'", mapnik.Color(0, 55, 0), 0.3))
			mapStyle.rules.append(self.makePolygonStyle("[building]", mapnik.Color(50, 50, 50), 0.3))
			
			mapStyle.rules.append(self.makeLineStyle("[highway]", mapnik.Color('grey'), 1, 1))
			mapStyle.rules.append(self.makeLineStyle("[railway]", mapnik.Color(120, 120, 120), 1, 1))
			
			
			streetFilter = mapnik.Filter("[highway]")
			RoadNameRule = mapnik.Rule()
			mapTextSymbolizer = mapnik.TextSymbolizer(mapnik.Expression('[name]'), 'DejaVu Sans Book', 8, mapnik.Color('black'))
			mapTextSymbolizer.halo_fill = mapnik.Color('white')
			mapTextSymbolizer.halo_radius = 1
			mapTextSymbolizer.label_placement = mapnik.label_placement.LINE_PLACEMENT
			RoadNameRule.symbols.append(mapTextSymbolizer)
			RoadNameRule.filter = streetFilter
			mapStyle.rules.append(RoadNameRule)
			
			
			m.append_style('Map Style',mapStyle)
			
			mapLayer = mapnik.Layer('highways')
			mapSource = mapnik.CreateDatasource({'type':'osm', 'file':self.getMap()})
			mapLayer.datasource = mapSource
			mapLayer.styles.append('Map Style')
			m.layers.append(mapLayer)
			
			
                        if oType == OutputType.OTYPE_SVG:
                            background_path += ".svg"
        		    svgfile = open(background_path, 'wb')
	        	    svg_surface = cairo.SVGSurface(svgfile.name, m.width, m.height)
		            mapnik.render(m, svg_surface)
			    svg_surface.finish()
        		    svgfile.close()
                        elif oType == OutputType.OTYPE_PNG:
                            im = mapnik.Image(m.width, m.height)
		            mapnik.render(m, im)
                            background_path += ".png"
                            im.save(background_path,'png32:z=1')

		elif mType == MAPTYPE_STATIC:
			
			if not os.path.isfile(background_path):
				outpngFiles = {}

				
				#ratio between l and L 
				latSize = meterDist([box_lat_min, 0], [box_lat_max, 0])
				lonSize = meterDist([0, box_long_min], [0, box_long_max])

				#ratio = float(max(lonSize, latSize)) / min(lonSize, latSize)
				ratio = float(lonSize / latSize)
		
				xTiles = 0
				yTiles = 0
				

				if ratio > 1 :	
					yTiles = MIN_MAP_DIMENSION / TILE_SIZE
					xTiles = int(math.ceil(yTiles * ratio))
				else:
					xTiles = MIN_MAP_DIMENSION / TILE_SIZE
					yTiles = int(math.ceil(xTiles / ratio))
			
				#Determinate the appropriate OSM zoom in order to represent our smallest side with 1024 pixels
				#TODO replace longitude			
				#zoom = int(math.log(MIN_MAP_DIMENSION * CIRC_EARTH * cos(math.radians(box_lat_max)) / meterDist([box_long_max, box_lat_max], [box_long_min, box_lat_max]), 2) - 8)

				#From OSM doc, should be this one 
				#zoom = int(math.log(MIN_MAP_DIMENSION * CIRC_EARTH * cos(math.radians(box_lat_max)) / max(latSize, lonSize), 2) - 8)
				#Ok for trace 1.5.3_outdoor
				#zoom = int(math.log(MIN_MAP_DIMENSION * CIRC_EARTH * cos(math.radians(box_lat_max)) / max(latSize, lonSize), 2) - 8) + 1 ### DRUUUUGS !

				#Ok for trace GuiguiLoop S3 but offset...
				zoom = int(math.log(MIN_MAP_DIMENSION * CIRC_EARTH * cos(math.radians(box_lat_max)) / max(latSize, lonSize), 2) - 8) + 3 ### DRUUUUGS !
	
				
				lonDiff = box_long_max - box_long_min
				latDiff = box_lat_max - box_lat_min
				
				tileLonSize = lonDiff / xTiles
				tileLatSize = latDiff / yTiles
		
				tilesDir = MAP_DIR  + str(self.box_lat_max)  + F_SEP +  str(self.box_lat_min)  + F_SEP +  str(self.box_long_max)  + F_SEP +  str(self.box_long_min)  + F_SEP +  str(mType) +  "_TILES"  + os.path.sep
				if not os.path.exists(tilesDir):
					os.makedirs(tilesDir)
				
				"""for x in range(xTiles):
					for y in range(yTiles):
						outPath = tilesDir + "tile_z" + str(y) + "_" + str(x) + ".png"
						if not os.path.exists(outPath):
							print (str(box_lat_min + (0.5 + y) * tileLatSize) + "	" + str(box_long_min + (0.5 + x) * tileLonSize))
							target = getStaticUrlForPoint(box_lat_min + (0.5 + y) * tileLatSize,box_long_min + (0.5 + x) * tileLonSize, zoom)
							print (target)
							out = open(outPath, 'w')
							out.write(urllib2.urlopen(target).read())
							out.close()
				
						outpngFiles[x, y] = outPath"""


				X0 = lonToTile(box_long_min + 0.5 * tileLonSize, zoom)
				Y0 = latToTile(box_lat_min + 0.5 * tileLatSize, zoom)

				for x in range(xTiles):
					for y in range(yTiles):
						outPath = tilesDir + "tile_z" + str(y) + "_" + str(x) + ".png"
						if not os.path.exists(outPath):
							target = BASE_URL + str(zoom) + "/" + str(X0 + x) + "/"  + str(Y0 - y) + FILE_EXT
							out = open(outPath, 'w')
							out.write(urllib2.urlopen(target).read())
							out.close()
				
						outpngFiles[x, y] = outPath

				
				
				svgFile = svgwrite.Drawing("out.svg", size = (xTiles * TILE_SIZE, yTiles * TILE_SIZE))
				
				for (x, y) in outpngFiles:
				
					f = open(outpngFiles[x, y], "rb")
					svgFile.add(svgwrite.image.Image("data:image/png;base64," +  f.read().encode("base64").replace('\n','')  , insert=[x * TILE_SIZE, (yTiles - y - 1) * TILE_SIZE], size=[TILE_SIZE, TILE_SIZE]))
					#svgFile.add(svgwrite.image.Image("data:image/png;base64," +  f.read().encode("base64").replace('\n','')  , insert=[x * TILE_SIZE, y * TILE_SIZE], size=[TILE_SIZE, TILE_SIZE]))
					f.close()
				
				finalFile = open(background_path, 'w')
				finalFile.write(svgFile.tostring())
				finalFile.close()
					

		return background_path

	# Extract the GPS coordinates of the roads
	# roadSel parameter to pass a list of target road names
	# printNames optionnal parameter to print all the detected roads
	def getRoadsFromOSM(self, roadSel=[], printNames = False):
		self.getMap()
		soup = BeautifulSoup(open(self.osm_data_path))
		
		Roads = []
		RoadNames = []
		
		Coordinates = {}
	
	
		for point in soup.osm.findAll('node'):
			Coordinates[point['id']] = (float(point['lat']), float(point['lon']))

		for way in soup.osm.findAll(lambda nd : nd.name=="way" and nd.findAll(k='highway')):
			name = ""
			road = []

			nodes = way.findAll('nd')

			#Get the road Name	
			for u in nodes[-1].findAll('tag'):
				if u['k'] == 'name':
					name =  u['v']
			
			if len(name) > 0:
				RoadNames.append(name)

			if len(roadSel) == 0 or name in roadSel:
				for node in nodes:
					road.append(Coordinates[node['ref']])
	
				Roads.append(road)

		if printNames:
			for n in set(RoadNames):
				print (n)

		return Roads

	# Extract the GPS coordinates of the roads intersections
	# Return a dict with the GPS coordinates of intersection as keys, 
	# and values consisting in a list of tuples : [RoadIndex, IntersectionPointIndex (in the road)]
	# TODO roadSel parameter similar to getRoadsFromOSM
	def getIntersectionsFromOSM(self):
		self.getMap()
		soup = BeautifulSoup(open(self.osm_data_path))
	
		retval = {}	
		tmpRetVal = {}
		#correspondingRefs = []
		Roads = []
		RoadRefs = []		
		Coordinates = {}
		
		for point in soup.osm.findAll('node'):
			Coordinates[point['id']] = (float(point['lat']), float(point['lon']))

		for way in soup.osm.findAll(lambda node : node.name=="way" and node.findAll(k='highway')):
			name = ""
			roadPoints = []
			
			nodes = way.findAll('nd')
			for node in nodes:
				roadPoints.append(node['ref'])

			RoadRefs.append(roadPoints)


		#TOoo much imbriqued iterations...matrixes ?

		#Iterate the ids of points crossed by each road
		#THIS
		"""for roadIdx, roadRef in enumerate(RoadRefs):
			appendVal = []
			#And compare them to the list of ids by other roads
			for roadJdx, otherRef in enumerate(RoadRefs):
				if roadIdx != roadJdx:
					#If a common ref is found, keep the id of the other road, and the point of intersection
					for segIdx, seg in enumerate(roadRef):
						if seg in otherRef:
							#appendVal.append([[roadJj, Coordinates[ref]])
							coords = Coordinates[seg]
							if coords not in tmpRetVal:
								tmpRetVal[coords] = []

							tmpRetVal[coords].append([roadIdx, segIdx]) """
		#End THIS
		#REPLACED WITH THAT
		for roadIdx, roadRef in enumerate(RoadRefs):
			for segIdx, seg in enumerate(roadRef):
				coords = Coordinates[seg]
				if coords not in tmpRetVal:
					tmpRetVal[coords] = []

				tmpRetVal[coords].append([roadIdx, segIdx]) 
	
		delVals = []	
		for k in tmpRetVal.keys():
			if len(tmpRetVal[k]) <2:
				del(tmpRetVal[k])
		#END THAT
		

			#correspondingRefs.append(appendVal)

		#Finally, reformat as a dict position:[list of road indexes]
		#for refs in correspondingRefs:
		#	for r, coords in refs:
		#		if coords not in tmpRetVal:
		#			tmpRetVal[coords] = []
		#		tmpRetVal[coords].append(r)

		#Delete double refs
		for u in tmpRetVal:
			retval[u] = []
			for v in tmpRetVal[u]:
				if v[0] not in [w[0] for w in retval[u]] and v[1] not in [w[1] for w in retval[u]]:
					retval[u].append(v)

		return retval
