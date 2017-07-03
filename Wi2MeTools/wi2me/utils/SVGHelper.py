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


import svgwrite
import MapHelper


def addOverlay( background_file, overlay, out_file):
	line = ""
	def_part = "<defs />"
	
	backGround = open(background_file, 'r')
	finalFile = open(out_file, 'wb')
	fileBuff = backGround.read().replace(">", ">\n")

	for line in fileBuff.split('\n'):
		if line.find("</svg>") < 0:
			finalFile.write(line)
			line = backGround.readline()
		else:
			break


	finalFile.write(overlay[overlay.find(def_part) + len(def_part):])
	finalFile.close()

""" delete double  points to lighten final image a tiny bit """
def sanitizePoints(points):
	retval = []

	for point in points:
		add = 1
		for p in retval:
			if p[0] == point[0] and p[1] == point[1]:
				add = 0
				break
		if add > 0:
			retval.append(point)

	return retval


class SVGHelper:
	def __init__(self, path, width, height, mapHelper):
		self.path = path

		self.width = width
		self.height = height

		self.mapHelper = mapHelper

		self.svgFile = svgwrite.Drawing(filename = path, size = (width, height))




	def plotPoints(self, points, width, color, stroke = "black", opacity = 1, convert = True ):

		addedPoints = sanitizePoints(points)
                if convert:
        		points = self.mapHelper.convertPoints(addedPoints) 
		
		for point in points:
			self.svgFile.add(self.svgFile.circle(point, width, stroke = stroke, fill = color, style = "fill-opacity:" + str(opacity)))



	def plotLineFromPoints(self, points, width, color, plotSingles=True):

		addedPoints = sanitizePoints(points)

		#Change coordinates to the ones of the image
		convertedPoints = self.mapHelper.convertPoints(addedPoints)

		if len(convertedPoints) > 1:
			self.svgFile.add(self.svgFile.polyline(convertedPoints, stroke_width = width, stroke = color, fill = 'none'))

		elif len(convertedPoints) == 1 and plotSingles:
			self.svgFile.add(self.svgFile.rect(convertedPoints[0], (width, width), stroke = color, fill = color))

	#plot a sequence of lines normally from bgPoints, using width bgWidth and color bgColor, then highlight said sequence between the hlStart and hlStop params using the hlwidth width and the hlColor color
	def plotAndHighlightLineFromPoints(self, hlStart, hlStop, hlWidth, hlColor, bgPoints, bgWidth, bgColor):

		addedPoints = sanitizePoints(bgPoints)
		convertedPoints = self.mapHelper.convertPoints(addedPoints)
		h_start, h_stop = self.mapHelper.convertPoints([hlStart, hlStop])

		if len(convertedPoints) > 1:
			#Determine the last point before the highlighted zone and the last one inside the highlighted zone
			_, _, startHlInd, _ = MapHelper.placePointOnRoadsWithAbscisse(hlStart, [bgPoints])
			_, _, beforeLastHlInd, _ = MapHelper.placePointOnRoadsWithAbscisse(hlStop, [bgPoints])

			
			if startHlInd > beforeLastHlInd:
				startHlInd, beforeLastHlInd = [beforeLastHlInd, startHlInd]

			self.svgFile.add(self.svgFile.polyline(convertedPoints[:startHlInd] + [h_start], stroke_width = bgWidth, stroke = bgColor, fill = 'none'))
			self.svgFile.add(self.svgFile.polyline([h_start] + convertedPoints[startHlInd + 1:beforeLastHlInd] + [h_stop], stroke_width = hlWidth, stroke = hlColor, fill = 'none'))
			if beforeLastHlInd + 1 < len(convertedPoints):
				self.svgFile.add(self.svgFile.polyline([h_stop] + convertedPoints[beforeLastHlInd + 1:], stroke_width = bgWidth, stroke = bgColor, fill = 'none'))




	def addText(self, text, position, size, font = "Arial", color = "black"):
		self.svgFile.add(self.svgFile.text(text, insert=self.mapHelper.convertPoint(position), style = "font-size:" + str(size) + "px; font-family:" + font + "; fill:" + color))


	def plotRectangle(self, lat_min, lat_max, long_min, long_max, color, opacity = 1):

		[x1, y1], [x2, y2] = self.mapHelper.convertPoints([[lat_min, long_min], [lat_max, long_max]])

		height = abs(x2 - x1)
		width = abs(y2 - y1)
		self.svgFile.add(self.svgFile.rect((min(x1, x2), min(y1, y2)), (width, height), stroke = color, fill = color, style = "fill-opacity:" + str(opacity)))

	def finish(self):
		self.svgFile.save()
