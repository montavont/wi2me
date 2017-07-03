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

import math

from wi2me.utils import MapHelper
from wi2me.utils import MatrixHelper
import numpy as np
import weakref

class coverageGraph:
	def __init__(self, roadGPSs, mapHelper, inter = None):

		self.roads = []
		self.mapHelper = mapHelper

		for roadIdx, road in enumerate(roadGPSs):
			gr = graphRoad()
			self.roads.append(gr)
			startP = road[0]
			for segIdx, stopP in enumerate(road[1:]):
				seg = graphRoadSegment(startP, stopP)
				gr.segments.append(seg)
				if segIdx > 0:
					seg.pSeg.append(weakref.proxy(self.roads[roadIdx].segments[segIdx - 1]))
					seg.pSeg[-1].nSeg.append(weakref.proxy(seg))
				startP = stopP

		#Get a list of road intersection and fill the segment information accordingly
		if inter is None :
			inter = mapHelper.getIntersectionsFromOSM()
		for point  in inter:
			# We get a list of points, but need to operate on a list of segment
			# Thus if the point is out of range (a list of points will have one more element), reduce it by one, it will operate on the last segment
			for idx, (roadIdx, segIdx) in enumerate(inter[point]):
				if segIdx == len(self.roads[roadIdx].segments):
					inter[point][idx] = [roadIdx, segIdx - 1]

			for roadIdx, segIdx in inter[point]:
				#addedValue = [[roadJdx, segJdx] for roadJdx, segJdx in inter[point] if segJdx != segIdx]
				addedValue = [weakref.proxy(self.roads[roadJdx].segments[segJdx]) for roadJdx, segJdx in inter[point] if segJdx != segIdx]

				if segIdx < len(self.roads[roadIdx].segments) and point == self.roads[roadIdx].segments[segIdx].start:
					self.roads[roadIdx].segments[segIdx].pSeg += addedValue
				elif point == self.roads[roadIdx].segments[segIdx].stop:
					self.roads[roadIdx].segments[segIdx].nSeg += addedValue

	#Generate matrixes on a Graf wide scale : triggers matrix generation on each road, then iterates all the segments to fill lines at the beginning and end of each matrix, when an AP was seen in two consecutive segments
	def generateMatrixes(self, func):

		for road in self.roads:
			road.generateMatrixes(func)


		for seg in [s for road in self.roads for s in road.segments]:
			#Plotting points linking up to the next segments
			currentNonZ = np.nonzero(seg.matrix)[0]
			for neighSeg in seg.nSeg:
				nextNonZ = np.nonzero(neighSeg.matrix)[0]
					
				if len(currentNonZ) > 0 and len(nextNonZ) > 0:
					currentLastPos = max(currentNonZ)
					nextFirstPos = min(nextNonZ)
					for j, cell in enumerate(seg.matrix[currentLastPos]):
						if cell != 0 and len(cell) > 0:
							det = cell[0]
							for nJ, nCell in enumerate(neighSeg.matrix[nextFirstPos]):
								if nCell != 0 and len(nCell) > 0:
									nDet = nCell[0]
									if det.nw.ap.id == nDet.nw.ap.id :
										for l in range(currentLastPos, seg.matrix.shape[0]):
											if seg.matrix[l, j] == 0:
												seg.matrix[l, j] = []
											seg.matrix[l, j] += cell

										for l in range(0, nextFirstPos):
											if neighSeg.matrix[l, nJ] == 0:
												neighSeg.matrix[l, nJ] = []
											neighSeg.matrix[l, nJ] += nCell
										break

class graphRoad:
	def __init__(self):
		self.segments = []

	def generateMatrixes(self, func):
		for seg in self.segments:
			seg.generateMatrix(func)



class graphRoadSegment:
	instCounter = 0

	def __init__(self, start, stop):
		self.id = graphRoadSegment.instCounter
		graphRoadSegment.instCounter += 1

		self.start = start
		self.stop = stop
	
		self.exploreedSections = []		
	
		self.mLength = int(math.floor(MapHelper.meterDist(self.start, self.stop) + 1))

		#Links to other segments as tuples : (roadIndex, segmentIndex)
		self.pSeg = [] #Last segments (whose stops is our start)
		self.nSeg = [] #Next segments (whose start is our stop)

		#Data representation
		self.scanResults = {}
		self.matrix = np.zeros((0,0))

		#Keep track of the parts of the road we do not hava data for.
		#TODO : do that in generate Matrix ?
		#Like for each sourceInd (traceScans) count meters at a scan or between two successive scans


	def generateMatrix(self, func):
		if len(self.scanResults) > 0:
			matrixes  = []	
			for traceScans in self.scanResults.values():
				matrixes.append(MatrixHelper.createDistanceMatrix(self.mLength, traceScans))

			self.matrix = MatrixHelper.mergeMatrixesByColumns(matrixes, func)
			#self.scanResults = {} #TODO, darn, need to be kept to use different 'func's in a row....


