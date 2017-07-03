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
import numpy as np
from PIL import Image
import math

from wi2me.utils import MapHelper
from wi2me.model import AP, APManager

import sys
import collections

# Create a matrix from a source
# Lines represent the scan attempts' times 
# Columns represent the bssids
# Each cell contains a list of APs seen at one time under the same bssid (They will usually be only one element, but not always)
def getObjectMatrix(source):
	apMgr = APManager.APManager([source])

	width = len(apMgr.networks)
	height = apMgr.scanCount

	retval = np.zeros((height, width), dtype=object)

	for j, nw in enumerate(apMgr.networks):
		for det in nw.detections:
			if retval[det.scanInd, j] != 0 :
				retval[det.scanInd, j] += nw.detections
			else:
				retval[det.scanInd, j] = [det]
	return retval, apMgr


#Get the GPS data for each line
#If a line is empty, add the GPS data of the previous one
def getPoints(matrix):
	Points = []
	for line in matrix:
		#first ap in the first non null cell of each line
		nonNull = np.nonzero(line)[0]
		if len(nonNull) > 0:
			Points.append(line[nonNull][0][0].GPS)
		else:
			Points.append(Points[len(Points) - 1])

	return Points


# Create a distance matrix from a total length in meters and a dict of AP list, which's keys are the distance from start
# WARNING : this produces a matrix with non merged/splitted columns, a bssid has only one column, and does not share it
def createDistanceMatrix(length, scans):
	apIds = [b for b in set([det.nw.ap.id for dets in scans.values() for det in dets])]
	width = len(apIds)
	retval = np.zeros((length, width), dtype=object)	

	dists = [int(math.floor(dist)) for dist in scans]
	# Place the scan points at their discrete positions
	for line in dists:
		for det in scans[dist]:
			col = apIds.index(det.nw.ap.id)
			if retval[line, col] == 0:
				retval[line, col] = []
			retval[line, col].append(det)
	
	# Iterate the filled matrix
	# If an AP was seen in two successive scans, fill the cells inbetween
	#TODO for when connecting segments matrixes : modify this/add another loop to check neighbours and fill accordingly
	if len(dists) > 1:
		for j, col in enumerate(retval.T):
			lastDist = dists[0]
			for dist in dists[1:]:
				if col[lastDist] != 0 and col[dist] != 0:
					for i in range(lastDist + 1, dist):
						retval[i, j] = col[lastDist]
				lastDist = dist
	return retval

#Convert a scan matrix to a distance matrix using the GPS coordinates of access points as well as an optionnal subset of road and an also optionnal distance offset
def convertToDistanceMatrix(matrix, Roads = [], startPoint =[]):

	# Get the GPS coordinates for each lines
	ScanDistances = []
	retMat = None
	lineLength = 1 #How much meters a matrix line stands for

	Points = [p  for p in MapHelper.cleanGPS(getPoints(matrix)) if p != (0, 0)]

	#cleanGPS return an empty list, meaning the source's GPS is not available. Return the input matrix instead
	if len(Points) == 0:
		retMat = matrix
	else:
		if len(Roads) > 0:
			Points = MapHelper.placePointsOnRoads(Points, Roads)

		# Compute the linear ?abscisse? of each line
		for i, point in enumerate(Points):
			if point <= 0:
			 	ScanDistances.append(-1)
			else:
			 	ScanDistances.append(MapHelper.SequentialDist(startPoint + Points[:i]))

		numLines = int(ScanDistances[len(ScanDistances) - 1] / lineLength + 1)
		retMat = np.zeros((numLines, matrix.shape[1]), dtype=object)

		for j, col in enumerate(matrix.T):
			lines = np.nonzero(col)[0]
			if len(lines) > 0:
				lastL = lines[0]
				
				for line in lines:
					scanLine = int(math.floor(ScanDistances[line] / lineLength))
					retMat[scanLine, j] = matrix[line, j]
					#Two scans in a row, fill inbetween
					if line - lastL == 1:
						for u in range(int(math.floor(ScanDistances[line - 1] / lineLength)), scanLine):
							retMat[u, j] = matrix[line, j]

					lastL = line

	return retMat


#Draw a bitmap with blacnk pixels on the non null values of the matrix, and black pixels where something was seen
def saveBlackNWhiteBitmap(matrix, path = None):

	im = Image.new("RGB", matrix.shape[::-1], "white")

	for line, col in zip(*np.nonzero(matrix)):
		im.putpixel((col, line), (0, 0, 0))

	if path:
		im.save(path)

	return im



def plotAnimatedMatrix(mat, path):


	try:
		from bokeh.plotting import *
		from bokeh.objects import HoverTool, ColumnDataSource

		from bokeh.charts import TimeSeries

		from collections import OrderedDict
		import pandas as pd
	except ImportError, e:
		print "Missing libraries to plot animated matrixes"
		print e
		return


	reset_output()
	output_file(path, title="Matrix is rocknroll")

	figure()

	points = np.nonzero(mat)

	xname = []
	xind = []
	yname = []
	ssidName = []	
	apWidth = []
	origins = []
	colors = []


	for pos, col in zip(*points):
	        yname.append(pos)
	        xind.append(col)

		#add the list of ssids for the AP
		ssidName.append("")
		#for ssid in set([nw.ssid for nw in mat[pos, col].networks]):
		for ssid in set([det.nw.ssid for det in mat[pos, col]]):
			if len(ssid) > 0:
				ssidName[len(ssidName) - 1] += ssid
				ssidName[len(ssidName) - 1] += ", "
		
		#List of broadcasted bssids
		xname.append("")
		#for sbssid in set([nw.bssid for nw in mat[pos, col].networks]):
		for sbssid in set([det.nw.bssid for det in mat[pos, col]]):
			if len(sbssid) > 0:
				xname[len(xname) - 1] += sbssid
				xname[len(xname) - 1] += ", "


		#Source from wich the APs were observed
		
		sTypes = {sType:0 for sType in set([det.source for det in mat[pos, col]])}
		for sType in [det.source for det in mat[pos, col]]:
			sTypes[sType] += 1

		origins.append("")
		for sType in sorted(sTypes):
			origins[-1] += sType + " : " + str(sTypes[sType]) + ","

		#Check if additionnal color info was provided at the AP level, otherwise paint it black !
		color = 0
		for attrs in [det.nw.attrs for det in mat[pos, col] if det.nw != None and "color" in det.nw.attrs]:
			color = attrs["color"]
			
		if color != 0:
			colors.append(color)
		else:
			colors.append('black')
	
		apWidth.append(len(mat[pos, col]))
	
	source = ColumnDataSource(
	    data=dict(
		xpoints = points[0],
		ypoints = points[1],
	        xname=xname,
	        xind=xind,
		yname=yname,
		ssidName=ssidName,
		apWidth=apWidth,
		origins=origins,
		)
	)
	
	rect('ypoints', 'xpoints', width=1, height=1,
	        color=colors, fill_alpha=0.5, name="CoverageMatrix",
		x_axis_location="above", source=source,
		tools="hover,pan,wheel_zoom,box_zoom,reset,previewsave",
		plot_width=1200, plot_height=800
	)
	        
	xax, yax = axis()
	xax.axis_label = 'APs'
	yax.axis_label = 'Coverage'
	
	grid().grid_line_color = None
	
	curplot().title = "Coverage Matrix"
	
	hover = curplot().select(dict(type=HoverTool))
	hover.tooltips = OrderedDict([
	    ('BSSID', '@xname'),
	    ('SSIDs', '@ssidName'),
	    ('Detection index', '@yname'),
	    ('AP index', '@xind'),
	    ('Number of Bssids', '@apWidth'),
	    ('Observed from', '@origins'),
	])
	
	show()





######################################################################################################################
#
# 	Matrix Operation Function
#			
#		1 input matrix - 1 ouput matrix
######################################################################################################################

MSEC = 1000
MAX_TR = 100


# Compare bssids in order to merge the ones belonging to the same APs
# Uses the 4 middle bits of the mac address, if common, merge
def bssGrouping(matrix):

	Cols2Merge = []

	for j, col in enumerate(matrix.T):
		bssid = col[np.where(col!=0)][0].networks[0].bssid
		appended = False
		for nj, nCol in enumerate(Cols2Merge):
			#check if our middle bytes are the same for each list of Cols2Merge

			if bssid[3:len(bssid)-3] in [nw.bssid[3:len(nw.bssid)-3] for colInd in nCol for ap in matrix.T[colInd][np.where(matrix.T[colInd]!=0)] for nw in ap.networks]:
				Cols2Merge[nj].append(j)
				appended = True
				break	
		if not appended:
			Cols2Merge.append([j])

	retval = np.zeros((matrix.shape[0], len(Cols2Merge)), dtype=object)
	for nj, nCol in enumerate(Cols2Merge):
		for col in [matrix.T[j] for j in nCol]:
			for i in np.where(col!=0)[0]:
				if retval[i, nj] == 0:
					retval[i, nj] = col[i]
				else:
					for nw in col[i].networks:
						try:
							nwInd = [n.bssid for n in retval[i, nj].networks].index(nw.bssid)
							retval[i, nj].networks[nwInd].detections += nw.detections
						except ValueError:
							retval[i, nj].networks.append(nw)

	return retval

 
def findSsid(col, bssid, pos):
	retval = ""	
	ssids = []
	
	for j in np.nonzero(col)[0]:
		for ap in col[j]:
			if ap.bssid == bssid and len(ap.ssid) > 0:
				ssids.append((ap.ssid, abs(j - pos)))

	if len(ssids) > 0:
		retval = min(ssids, key = lambda x : x[1])[0]

	return retval

def rewriteSSIDs(matrix):
	for j, col in enumerate(matrix.T):
		for i in np.nonzero(col)[0]:
			for v, ap in enumerate(col[i]):
				if len(ap.ssid) == 0:
					matrix[i, j][v].ssid = findSsid(col, ap.bssid, i)

	return matrix


# Use the ssid sets of an AP to create a column for physical AP
# These are the more detailed steps
# 
#	for each column : 
#		- Find the different mac addresses present in the column
#		- For each mac address, find wich SSIDs it broadcasts
#	
#		If no ssid is broadcasted (only nulls), do not change the column and append it to the final matrix
#		
#		Else, let us consider the list of lists of SSIDs [ [SSID1_OF_MACADDRESS_1, SSID2_OF_MACADDRESS_1, SSID2_OF_MACADDRESS_1], [SSID1_OF_MACADDRESS_2, SSID2_OF_MACADDRESS_2, SSID2_OF_MACADDRESS_2], [SSID1_OF_MACADDRESS_3, SSID2_OF_MACADDRESS_3], ...] 
#			- Look for SSIDs that are present in more than one list, remove them 
#			- Let us look at the longest of those lists,
#				if there is only one, we have our discriminator set, probably containing the private SSIDs
#				if we have a draw, look at how much of the matrix columns contain the BSSID. The Ssid list wich is share by the lower number of columns will be our discriminator set
#
#			- Create one more column (of the final matricx) for each SSID of our 		
#			- Now for each line of our column
#				- If no AP with an SSID from the discriminator set is in this cell, discard the whole cell !
#						(it is probably bad coverage anyway)
#				- Else, cool, we then put this cell in the column we created for the SSID of the discriminator set we detected
#			-Append these new columns to the final matrix, and off we go to the next Column
#
#	The function below works slightly differently as it first determines the discriminator sets for each column, and then iterates again to fill them. This allow us to allocate a matrix of the appropriate size and to avoid regular calls to np.concatenate that deteriorate parformances.
def bssidSsidSplitting(matrix, extras = {}):

	discarded = []

	finalSize = 0
	discSets = []

	if 'addAdjacent' in extras : 
		addAdjacents = extras['addAdjacent']
	else:
		addAdjacents = 0
	

	#Fill SSID data when it was blank
	matrix = rewriteSSIDs(matrix)	


	#First pass, determine the final matrix size
	for col_ind, col in enumerate(matrix.T):

		ssidCounts = {}
		discriminatorSet = []

		for aps in col[np.nonzero(col)]:
			for ap in aps:
 				if len(ap.ssid) > 0:
					if ap.bssid in ssidCounts:
						ssidCounts[ap.bssid].append(ap.ssid)
					else:
						ssidCounts[ap.bssid] = [ap.ssid]


		# Regroup the SSIDs useed by the APs operating in this bssid range...
		ssidSets = [set(ssidCounts[u]) for u in ssidCounts]

		if len(ssidSets) > 0:

			#Now delete ssids that were found on more than one mac address:
			uniqueSsidSets = []
			doubleChecker = {}
			for ssids in ssidSets:
				for ssid in ssids:
					if ssid not in doubleChecker:
						doubleChecker[ssid] = 0
					doubleChecker[ssid] += 1
			for ssids in ssidSets:
				values = []
				for ssid in ssids:
					if doubleChecker[ssid] == 1:
						values.append(ssid)
				uniqueSsidSets.append(values)

			# And select the one that changes the most
			discriminatorLen = len(max(uniqueSsidSets, key = lambda x: len(x)))
			candidates = [ssidSet for ssidSet in uniqueSsidSets if len(ssidSet) == discriminatorLen]

			# Check for unique max, this will not happen often, usually CN SSIDs were removed by the previous step
			# For these remaining cases, we can indulge in heavier processing
			if len(candidates) > 1:
				#More than one candidate 
				canCounts = {}
				#Check for other APs with such an ssid
				for i, cand in enumerate(candidates):
					canCounts[i] = []
					for ssid in cand:
						canCounts[i].append(len(set([j for j, column in enumerate(matrix.T) for aps in column if aps > 0 for ap in aps if ssid == ap.ssid])))
				disIndex = min(canCounts, key = lambda x: canCounts[x])
				discriminatorSet = candidates[disIndex]
			else:
				#only one candidate, we good !
				discriminatorSet = candidates[0]

			#convert back to array
			discriminatorSet = [ u for u in discriminatorSet]
			
			finalSize += len(discriminatorSet)
			
		else:	
			finalSize += 1

		discSets.append(discriminatorSet)
	

	finalSize = np.sum([max(1, len(u)) for u in discSets])
	
	retval = np.zeros((matrix.shape[0], finalSize), dtype=object)
	currentCol = 0

	#Second pass, fill in the columns !
	for col_ind, col in enumerate(matrix.T):
			discriminatorSet = discSets[col_ind]

			if len(discriminatorSet) > 0:
				
				concatValue = np.zeros((matrix.shape[0], len(discriminatorSet)), dtype=object)
				nonAvailables = np.zeros((matrix.shape[0], 1), dtype=object)
			
				for i, aps in enumerate(col):
					targetCol = -1
					if aps != 0:
						#Check wich ssid we will use to select the column
						for ap in aps:
							if ap.ssid in discriminatorSet:
								targetCol = discriminatorSet.index(ap.ssid)
								break
						
						#Looks like we missed the discriminator ap in this scan, use the last column
						if targetCol < 0:
							nonAvailables[i, 0] = aps
						else:
							#Append the ap to the appropriate column
							for ap in aps:
								if concatValue[i, targetCol] != 0:
									concatValue[i, targetCol].append(ap)
								else:
									concatValue[i, targetCol] = [ap]


				# Optionnal : go over the discarded cells, if one of them is adjacent (close from less than addAdjacent) to a non null cell of the columns, add it
				if addAdjacents > 0:
					addedSome = True
					while addedSome:
						addedSome = False
						#iterate the non allocated cells
						for i in np.nonzero(nonAvailables.T[0])[0]:
							for j0, aCol in enumerate(concatValue.T):
								nonz0 = np.nonzero(aCol)[0]
								if len(nonz0) > 0 and min([abs(i0 - i) for i0 in nonz0]) <= addAdjacents:
									addedSome = True
									concatValue[i, j0] = []
									for ap in nonAvailables[i, 0]:
										concatValue[i, j0].append(ap)
									nonAvailables[i, 0] = 0
									break

				if len(np.nonzero(nonAvailables)[0]) > 0:
					discarded.append(col_ind)

				for i in range(concatValue.shape[1]):
					retval[:,currentCol] = concatValue.T[i]
					currentCol += 1

			else:
				retval[:,currentCol] = col
				currentCol += 1

	return retval

#Use the time offsets to create a column for each bssid
def bssidTimeSplitting(matrix, extras = {}):
	retval = np.zeros((matrix.shape[0], 0), dtype=object)
	reused = 0

	if 'threshold' in extras:
		threshold = extras['threshold']
	else:
		threshold = MAX_TR * MSEC
	
	for col in matrix.T:
		timeOffsets = []

		concatValue = np.zeros((matrix.shape[0], 0), dtype=object)

		for l, aps in enumerate(col):
			if aps != 0:
				for ap in aps:
					if ap.uptime > 0:
						timeOffsets.append([ap, math.fabs(ap.uptime - ap.timestamp), l])

		#We did find some time offsets, this column can be splitted by time
		if len(timeOffsets) > 0:

			sortedOffsets = sorted(timeOffsets, key=lambda x: x[1])
			lastOffset = - threshold - 1
			for ap, off, l in sortedOffsets:
				if off - lastOffset > threshold:
					newCol = np.zeros((matrix.shape[0], 1), dtype=object)
					newCol[l, 0] = [ap]
					concatValue = np.concatenate((concatValue, newCol), axis = 1)
				else:
					if concatValue[l, concatValue.shape[1] - 1] != 0:
						concatValue[l, concatValue.shape[1] - 1].append(ap)
					else:
						concatValue[l, concatValue.shape[1] - 1] = [ap]

				lastOffset = off
		#No uptimes were found, append the column as is
		else:
			concatValue = np.zeros((matrix.shape[0], 1), dtype=object)
			concatValue[:,0] = col


		if concatValue.shape[1] > 1:
			reused += 1
		retval = np.concatenate((retval, concatValue), axis = 1)
	
	return retval


"""
	Esthetical function to sort the columns by order of first non null occurence
"""
def orderMatrix(matrix):
	retval = matrix
	lines, cols = matrix.shape

	startLine = 0

	#Don't waste time with empty matrixes
	if len(np.nonzero(matrix)[0]) > 0:

		while len(np.where(retval[startLine] == 0)[0]) == 0 and startLine < lines:
			startLine += 1

		firstNull =  np.where(retval[startLine] == 0)[0][0]
		for i in range(startLine, lines):
			for j in range(firstNull, cols):
				if retval[i, j] != 0 :
					#swap columns
					newOrder = range(cols)
					newOrder[firstNull] = j
					newOrder[j] = firstNull
					retval = retval[:, newOrder]
					firstNull += 1

	return retval



######################################################################################################################
#
# 	Matrix Merging Functions
#
######################################################################################################################


MERGE_COLOR_FIRST = "green"
MERGE_COLOR_SECOND = "blue"
MERGE_COLOR_INTER = "red"


#Merge Two cells by appending their networks and detections
def cellUnionMerge(cell1, cell2, colorize = False):
	retval = 0

	if cell1 == 0 or len(cell1) == 0:
		color = MERGE_COLOR_SECOND
		retval = cell2
	elif cell2 == 0 or len(cell2) == 0:
		retval = cell1
		color = MERGE_COLOR_FIRST
	else:
		retval = cell1 + cell2
		color = MERGE_COLOR_INTER

	if colorize and retval != 0:
		for det in retval : 
			det.nw.attrs['color'] = color

	return retval


#Merge two columns if both are non null, return np.zeros(height) otherwise
def COLU_LII_MergeColorize(col1, col2):
	retval = col1
	nonz1 = np.where(col1!=0)[0]
	nonz2 = np.where(col2!=0)[0]


	#One of the columns is empty, return zeros
	if len(nonz2) * len(nonz1) == 0:
		retval = np.zeros(col1.shape, dtype=object)

	#Both nice columns, merge cell by cell
	else:
		for i, cell in enumerate(col2):
			retval[i] = cellUnionMerge(retval[i], cell, True)
	return retval



# Merge a list of matrixes into one using the specified function
# Function passed as the func parameter needs to operate on two matrix column 
# WARNING : These columns do not have to be of the same size, but the returned size is expected to be the one on the first parameter
def mergeMatrixesByColumns(mats, func, color = ""):

	#sort matrixes by number of lines, start with the biggest
	mats = sorted(mats, key=lambda mat: mat.shape[0])[::-1]

	retmat = mats[0]

	#Don't bother if only one matrix
	if len(mats) > 1:
		firstMatAPs = [col[np.where(col!=0)][0][0].nw.ap.id for col in mats[0].T if len(np.where(col!=0)[0]) > 0]
		otherMatsAPs = [col[np.where(col!=0)][0][0].nw.ap.id for mat in mats[1:] for col in mat.T if len(np.where(col!=0)[0]) > 0 and col[np.where(col!=0)][0][0].nw.ap.id not in firstMatAPs ]

		retmat = np.column_stack([retmat, np.zeros((retmat.shape[0], len(otherMatsAPs)), dtype=object)])
		currentCol = len(firstMatAPs)
	
		#Keep a list of merged columns from the first matrix. The ones not in this list at the end of the merge, will need to be merged with an empty column.
		mergedCols = []	

		#COlorize the first matrix
		#if len(color) > 0:
		#	for (i, j) in zip(*np.nonzero(retmat)):
		#		for det in retmat[i, j]:
		#			det.nw.attrs['color'] = color

		for mat in mats[1:]:
			for col in mat.T:
				# get the bssids in this column
				#NOTE : we assume we only merge matrixes with a single AP id per column
				if len(np.where(col!=0)[0]) > 0:
					apId = col[np.where(col!=0)][0][0].nw.ap.id
					
					#Bssid is already present in retmat
					if apId in firstMatAPs:
						targetCol = firstMatAPs.index(apId)
						newCol = func(retmat.T[targetCol], col)
						retmat[:,targetCol] = newCol
						mergedCols.append(targetCol)
					#No existing column was found, append a new column, merging each cell with an empty list
					else:
						if currentCol < retmat.shape[1]:
							newCol = func(np.zeros((retmat.shape[0]), dtype=object), col)
							if len(np.nonzero(newCol)[0]) > 0:
								#retmat[:, currentCol] = newCol[0]
								retmat[:, currentCol] = newCol
								firstMatAPs.append(apId)
								currentCol += 1
						
						else:
							print "ERROR MERGING MATRIXES"
		
		#Merge the untouched columns with an empty one.
		for j in range(mats[0].shape[1]):
			if j not in mergedCols:
				newCol = func(retmat.T[j], np.zeros((retmat.shape[0], 1), dtype=object))
				retmat[:,j] = newCol


		retmat = cleanEmptyColumns(retmat)
	
	return retmat

def cleanEmptyColumns(matrix):

	forDeletion = []

	for j, col in enumerate(matrix.T):
		if len(np.where(col!=0)[0]) == 0:
			forDeletion.append(j)

	for j in reversed(sorted(forDeletion)):
		matrix = np.delete(matrix, j, axis = 1)

	return matrix



######################################################################################################################
#
# 	AP Selection Algorithms
#
######################################################################################################################

# Start at time t0, select the AP with the longest coverage starting from time t0 and use it while it is seen.
# When it is lost at time t1, select it the same way and repeat
def coverageContinuous(mat):
	selected = []
	line = 0
	NumLines = mat.shape[0]
	uncovered = 0.0
	while line < NumLines:
		maxCov = 0
		bestSuccessor = -1

		#Iterate Columns
		for i in range(len(mat[line])):
			lastLine = line
			while lastLine < NumLines and mat[lastLine, i] != 0:
				lastLine += 1

			if lastLine - line > maxCov : 
				maxCov = lastLine - line
				bestSuccessor = i

		#Skip empty lines
		if bestSuccessor == -1:
			uncovered += 1
			line += 1
		else:
			selected.append([bestSuccessor, maxCov])
			line = line + maxCov

	return selected, uncovered


# Select the Column with the best (non continuous) coverage, delete the covered lines  and the chosen column from the matrix and start again
def coverageGreedy(mat):
	selected = []
	uncovered = 0.0
	oLines, oCols = mat.shape

	#Index of remaining columns, since we will delete them
	remCols = np.array(range(oCols))

	#Delete empty lines
	emptyForDeletion = []
	for i in range(mat.shape[0]):
		#if np.sum(mat[i]) == 0:
		if np.count_nonzero(mat[i]) == 0:
			emptyForDeletion.append(i)

	uncovered += len(emptyForDeletion)
	mat = np.delete(mat, emptyForDeletion, axis=0)

	#we detroy the matrix line by line
	while mat.shape[0] > 0:

		coveredForDeletion = []
	
		bestCol = remCols[0]	
		#Maximize coverage by minimizing the zero count
		bestColCov = len(np.nonzero(mat[:,remCols[0]])[0])

		#Iterate the remaining columns to get the one with the biggest coverage:
		for col in remCols:
			cov = len(np.nonzero(mat[:, col])[0])
			if cov > bestColCov:
				bestColCov = cov
				bestCol = col

		selected.append([bestCol, bestColCov])

		#Delete covered lines
		for i in reversed(range(mat.shape[0])):
			if mat[i, bestCol] != 0:
				coveredForDeletion.append(i)

		mat = np.delete(mat, coveredForDeletion, axis=0)

		#Clear the selected value to speed up process, since it is now a null column
		remCols = np.delete(remCols, np.where(remCols==bestCol)[0], axis=0)

	return selected, uncovered


# Select the access point with the best (non continuous) coverage over all the passed matrixes, delete the covered lines and the chosen column from the matrixes and start again. 
# This process is sped up by : 
# - Computing the non null lines, giving a max value of the contribution for a given AP [1]
# - Using this maximal value to interrupt AP iteration, if our coverage is already better than the remaining max coverages [2]
# - Caching a computed value of the coverage for an AP in a matrix in the coverages dict : [3]. These cached values are invalidated when the matrix is modified, or when the AP is selected [4]
# - Deleting the covered lines, columns or matrixes on the fly [5]
def multiCoverageGreedy(Graf):

	mats = [seg.matrix for road in Graf.roads for seg in road.segments if 0 not in seg.matrix.shape]

	selected = []
	uncovered = 0.0

	#Delete empty lines
	for mat in mats:
		emptyForDeletion = []
		for i in range(mat.shape[0]):
			#if np.sum(mat[i]) == 0:
			if np.count_nonzero(mat[i]) == 0:
				emptyForDeletion.append(i)

		uncovered += len(emptyForDeletion)
		mat = np.delete(mat, emptyForDeletion, axis=0)


	#Get a set of all APs Ids, then iterate them
	matrixAPPotentials = []
	APMaxCoverages = {}
	for mat in mats:
		for line in mat:
			for cell in line:
				if cell != 0:
					for apId in set([det.nw.ap.id for det in cell]):
						if apId not in APMaxCoverages:
							APMaxCoverages[apId] = 0
						APMaxCoverages[apId] += 1

	# [1]
	# Keep a list of tuples,
	# - The first value is the ap Id
	# - The second value is the non null lines, giving us the maximal coverage we can expect from this AP
	# We sort them from higher to lower maximal coverage in order to be able to interrupt our loop later
	matrixAPPotentials = sorted([(aid, APMaxCoverages[aid]) for aid in APMaxCoverages], key = lambda x :x [1])[::-1]

	coverages = {}
	for apId, _ in matrixAPPotentials:
		coverages[apId] = [-1 for mat in mats]
	
	while len(mats) > 0:

		bestMats = []
		bestCov = 0
		bestAP = -1

		currentPotential = np.inf # Start at + infinite

		apIndex = 0
		#[2] if current cov is better than it, we can stop iterating.
		while currentPotential > bestCov and apIndex < len(matrixAPPotentials):
			apId, potential = matrixAPPotentials[apIndex]

			sys.stdout.write("[GREEDY] selecting ap	" + str(apIndex) + "/" + str(len(matrixAPPotentials)) + "	" + str(potential) + "/" + str(bestCov) + "\r")

			sys.stdout.flush()

			covMats = []
			cov = 0

			#When replaced with real APs, we'll need to consider a list of bssid, (or will we ?)

			for matIdx, mat in enumerate(mats):
				# TODO : keep coverages in memory (check idx ok (cf deletion))
				mCov = coverages[apId][matIdx] #[3]
				if mCov == -1:
					mCov = 0
					for col in mat.T:
						covered = [cell for cell in col if cell != 0 and len([det for det in cell if det.nw.ap.id == apId]) > 0]
						if len(covered) > 0:
							mCov += len(covered)

					coverages[apId][matIdx] = mCov #[3]
										
				if mCov > 0:
					covMats.append(matIdx)
					cov += mCov

			if cov > bestCov:
				bestAP = apId
				bestCov = cov
				bestMats = covMats

			currentPotential = potential
			apIndex += 1

		

		#Remove the selected AP
		for apId, pot in matrixAPPotentials:
			if apId == bestAP:
				matrixAPPotentials.remove((apId, pot)) #Only once, this is a set

		selected.append([bestAP, bestCov])

		# Delete lines covered by selected bssid in matrixes [5]
		for i in bestMats:
			mat = mats[i]
			coveredForDeletion = []
			for y in range(mat.shape[0]):
				#check intersection with bestBssidSet
				#TODO : use AP id, bssid set is not proper to an AP
				if len([cell for cell in mat[y] if cell != 0 for det in cell if det.nw.ap.id == bestAP]) > 0:
					coveredForDeletion.append(y)

			if len(coveredForDeletion) > 0:
				for aid in coverages: #[4]
					coverages[aid][i] = -1
				mats[i] = np.delete(mats[i], coveredForDeletion, axis=0)

		#Remove empty columns [5]
		for i in bestMats:
			mat = mats[i]
			emptyForDel = []
			for x, col in enumerate(mat.T):
				if len(np.nonzero(col)[0]) == 0:
					emptyForDel.append(x)
			mats[i] = np.delete(mats[i], [emptyForDel], axis=1)

		# Remove fully covered matrixes (of now null shapes) [5]
		matForRemoval = []
		for i in bestMats:
			mat = mats[i]
			if 0 in mat.shape:
				matForRemoval.append(i)

		for i in sorted(matForRemoval)[::-1]:
			del(mats[i])
			for aid in coverages: #[4]
				del coverages[aid][i]
		
	sys.stdout.write('\n')
	return selected, uncovered




# Start at the first line of the first matrix and compute the access point available at this position that takes us the furthest
# The getApPotential local function lets us do that on a specified matrix at the specified offset.
# If the end of the matrix is reached and there are linked matrix, we recursively mesure the potential of this ap on them, adding them to its potential.
# The first Segment(matrix) of the algorithm is not chosen, however, once it is covered, we continue by recursively covering its neighbours
def multiCoverageContinuous(Graf):
	DEPTH =  2 # LIMIT THIS, until looping back is fixed 

	#retvals
	selected = []
	uncoveredLines = 0

	segs = collections.OrderedDict()



	for road in Graf.roads:
		for seg in road.segments:
			if 0 not in seg.matrix.shape:
				segs[seg.id] = seg

	if len(segs) > 0:

		initialLen = len(segs)

		currentSeg = segs.keys()[0]
		currentPos = 0
		

		def getApPotential(l_apId, l_seg, l_offset, l_depth, branches = []):
			l_matrix = l_seg.matrix
			l_retval = 0
				
			while l_offset < l_matrix.shape[0] and l_apId in [det.nw.ap.id for dets in l_matrix[l_offset] if dets != 0 for det in dets]:
				l_retval += 1
				l_offset += 1
			

			if l_depth > 0 and  l_offset == l_matrix.shape[0]:
				for l_neighbour in l_seg.nSeg:
					if l_neighbour.id in segs and l_neighbour.id not in branches:
						#TODO : if ok remove DEPTH !
						#l_retval += getApPotential(l_apId, l_neighbour, 0, l_depth - 1,  branches + [l_neighbour.id])
						l_retval += getApPotential(l_apId, l_neighbour, 0, l_depth,  branches + [l_neighbour.id])

			return l_retval

		while len(segs) > 0:

			sys.stdout.write("[CONTINUOUS] selecting ap	" + str(len(segs)) + "/" + str(initialLen) + "	" + str(len(selected)) + "\r")
			#reached the end of the current matrix
			if currentPos >= segs[currentSeg].matrix.shape[0]:
				segs.pop(currentSeg)
				if len(segs) > 0:
					currentSeg = segs.keys()[0]
				currentPos = 0

			#Ended up in an empty line, skip
			elif len(np.nonzero(segs[currentSeg].matrix[currentPos])[0]) == 0:
				currentPos += 1
				uncoveredLines += 1
			else:	
				potentials = [(det.nw.ap.id, getApPotential(det.nw.ap.id, segs[currentSeg], currentPos, DEPTH)) for dets in segs[currentSeg].matrix[currentPos] if dets > 0 for det in dets]
				bestApId, bestApCoverage = max (potentials, key = lambda x : x[1])
				selected.append((bestApId, bestApCoverage))

				if bestApCoverage + currentPos < segs[currentSeg].matrix.shape[0]:
					currentPos += bestApCoverage
				else:
					#Place the neighbours of the last segment at the beginning of our ordered dict
					if len(segs[currentSeg].nSeg) > 0:
						newSegs = collections.OrderedDict()
						#Place the neighbour first
						for nSeg in segs[currentSeg].nSeg:
							if nSeg.id in segs:
								newSegs[nSeg.id] = segs[nSeg.id]
						#Add the other ones
						for oSeg in segs:
							if oSeg not in newSegs and oSeg != currentSeg:
								newSegs[oSeg] = segs[oSeg]
						segs = newSegs
					else:
						segs.pop(currentSeg)

					if len(segs) > 0:
						currentSeg = segs.keys()[0]
					currentPos = 0

		sys.stdout.write('\n')
	return selected, uncoveredLines
