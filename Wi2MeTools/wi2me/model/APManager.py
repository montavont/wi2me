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
import weakref
from AP import AP, Network, Detection

class APManager:

	def __init__(self, sources):
		self.aps = []
		self._nws = {}
		self.networks = []
		self.scanCount = 0
		self.sources = sources


		for ind, source in enumerate(sources):
			results = source.getScanResults()
			self.scanCount += len(results)
			for scan in results:
				for res in scan:
					self._handleResult(res, ind)

		#for nws in self._nws.values():
		#	self.networks += nws
		self.networks = self._nws.values()
		del(self._nws)

		#for nets in self.networks:
		#	net = nets[0]
		#	print "	" + net.bssid
		#	for det in net.detections:
		#		print "		" + str(det.GPS) + "	" + det.nw.ssid


	#Used to parse scan results in CTOR, will assume in a very silly fashion that bssids characterize an unique network
	def _handleResult(self, network, sourceInd):
		bssid = network.bssid
		ssid = network.ssid
		
		for i in range(len(network.detections)):
			network.detections[i].sourceInd = sourceInd

		if (bssid, ssid) not in self._nws:
			self._nws[bssid, ssid] = network
			self._nws[bssid, ssid].detections[0].nw = weakref.proxy(self._nws[bssid, ssid])
		else:
			det = network.detections[0]
			det.nw = weakref.proxy(self._nws[bssid, ssid])
			self._nws[bssid, ssid].detections.append(det)

	def getBssidMatrixes(self):
		retval = []

		for srcInd, source in enumerate(self.sources):
			positions = source.getScanResults()

			height = source.countScans()
			#width = len(set([ap.bssid for res in positions for ap in res]))
			bssidSet = set([ap.bssid for res in positions for ap in res])
			width = len([nw.bssid for nw in self.networks if nw.bssid in bssidSet])
			matrix = np.zeros((height, width), dtype=object)
			currentCol = 0

			for j, nw in enumerate(self.networks):
				relevantNet = False
				for det in nw.detections:
					if det.sourceInd == srcInd:
						relevantNet = True
						if matrix[det.scanInd, currentCol] != 0 :
							matrix[det.scanInd, currentCol].append(det)
						else:
							matrix[det.scanInd, currentCol] = [det]
				if relevantNet:
					currentCol += 1

			retval.append(matrix)
		return retval

	def getAPMatrixes(self):
		retval = []
		if len(self.aps) == 0:
			self.bssGrouping()
			self.ssidSplitting()


		for srcInd, source in enumerate(self.sources):
			positions = source.getScanResults()

			#Determine the matrix size
			apIds = []
			for j, ap in enumerate(self.aps):
				for nw in ap.networks:
					apIds.append(nw.ap.id)

			width = len(set(apIds))
			height = source.countScans()
			matrix = np.zeros((height, width), dtype=object)
			currentCol = 0


			#Fill the matrix
			for j, ap in enumerate(self.aps):
				relevantAp = False
				for nw in ap.networks:
					for det in nw.detections:
						if det.sourceInd == srcInd:
							relevantAp = True
							if matrix[det.scanInd, currentCol] != 0 :
								matrix[det.scanInd, currentCol].append(det)
							else:
								matrix[det.scanInd, currentCol] = [det]
				if relevantAp:
					currentCol += 1

			retval.append(matrix)
		return retval

	#TODO : Make faster !!
	def bssGrouping(self):

		for net in self.networks:
			appended = False
			#check if our middle bytes are the same for each list of Cols2Merge
			for ap in self.aps:
				for apNet in ap.networks:
					if not appended:
						#If bssid is already in this AP, merge the detections to the existing one
						if net.bssid == apNet.bssid:
							for det in net.detections:
								det.nw = weakref.proxy(apNet)
							apNet.detections += net.detections
							appended = True
							break
						#If bssids look alike, add this bssid to the ap
						elif net.bssid[3:len(net.bssid)-3] == apNet.bssid[3:len(apNet.bssid)-3]:
							net.ap = weakref.proxy(ap)
							ap.networks.append(net)
							appended = True
							break
				if appended:
					break
	

			if not appended:
				newAp = AP([net])
				net.ap = weakref.proxy(newAp)
				self.aps.append(newAp)
		
		


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
	def ssidSplitting(self, addAdjacents = 0):

		newAps = []

		for ap in self.aps:

			ssidCounts = {}
			discriminatorSet = []

			for nw in ap.networks:
 				if len(nw.ssid) > 0:
					if nw.bssid in ssidCounts:
						ssidCounts[nw.bssid].append(nw.ssid)
					else:
						ssidCounts[nw.bssid] = [nw.ssid]

			# Regroup the SSIDs useed by the APs operating in this bssid range...
			ssidSets = [set(ssidCounts[u]) for u in ssidCounts]

			if len(ssidSets) > 1:

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
				# Look if this ssid is also used by other APs
				if len(candidates) > 1:
					#More than one candidate 
					canCounts = {}
					#Check for other APs with such an ssid
					for i, cand in enumerate(candidates):
						canCounts[i] = []
						for ssid in cand:
							canCounts[i].append(len(set([j for j, column in enumerate(self.aps) for nw in ap.networks if ssid == nw.ssid])))
					disIndex = min(canCounts, key = lambda x: canCounts[x])
					discriminatorSet = candidates[disIndex]
				else:
					#only one candidate, we good !
					discriminatorSet = candidates[0]

				#convert back to array
				discriminatorSet = [ u for u in discriminatorSet]

				if len(discriminatorSet) > 1:
				
					#separatedAPs = {ssid:AP() for ssid in discriminatorSet} #The APs we are creating from the current one
					separatedAPs = {} #The APs we are creating from the current one
					for ssid in discriminatorSet:
						separatedAPs[ssid] = AP([])

					repartition = {ssid:{} for ssid in discriminatorSet} #Keep trace of where we appended detections
			
					addedSome = True
					remainingNetworks = ap.networks
					while addedSome:
						addedSome = False
						for nw in remainingNetworks:
							nextLoopNw = []
							if nw.ssid in separatedAPs:
								nw.ap = weakref.proxy(separatedAPs[nw.ssid])
								separatedAPs[nw.ssid].networks.append(nw)
								for det in nw.detections:
									if det.sourceInd not in repartition[nw.ssid]:
										repartition[nw.ssid][det.sourceInd] = []
									repartition[nw.ssid][det.sourceInd].append(det.scanInd)
								addedSome = True

							else:
								remainingDets = []
								#Copy the detections to the closer separated AP with a 'addAdjacent' threshold Otherwise, keep the for the next loop in 'remainingDets'
								# NOTE : This uses proximity in terms of trace source and scan index. A GPS distance could also work
								for det in nw.detections:
									toRemain = True
									for discSsid in separatedAPs:
										if det.sourceInd in repartition[discSsid] and min([abs(det.scanInd - scIdx) for scIdx in repartition[discSsid][det.sourceInd]]) <= addAdjacents:

											#If we alread have an AP with the appropriate bssid, add to that one, if not, create it from nw
											if nw.bssid in [inN.bssid for inN in separatedAPs[discSsid].networks]:
												idx = [inN.bssid for inN in separatedAPs[discSsid].networks].index(nw.bssid)
												separatedAPs[discSsid].networks[idx].detections.append(det)
												det.nw = weakref.proxy(separatedAPs[discSsid].networks[idx])
											else:
												newNet = nw.duplicate()
												newNet.detections = [det]
												det.nw = weakref.proxy(newNet)
												newNet.ap = weakref.proxy(separatedAPs[discSsid])
												separatedAPs[discSsid].networks.append(newNet)
											toRemain = False
											break

									if toRemain:
										remainingDets.append(det)
									else:
										addedSome = True

								if len(remainingDets) > 0:
									nw.detections = remainingDets
									nextLoopNw.append(nw)
								

						remainingNetworks = nextLoopNw

					newAps += separatedAPs.values()

				elif len(discriminatorSet) == 1:
					newAps.append(ap)


			else:
				newAps.append(ap)

		self.aps = newAps



	def applyAPFilter(self, func):
		self.aps = [ap for ap in self.aps if func(ap)]

	def applyDetectionFilter(self, func):
		for ap in self.aps:
			for nw in ap.networks:
				nw.detections = [det for det in nw.detections if func(det)]

	#TKE REMOVE OR REPLACE WITH VERSION FROM 226
	def countUncoveredPositions(self, apSelection):
		retval = 0

		aWinrarIsAP = set([apId for apId, _ in apSelection])
		for iid in aWinrarIsAP:
			if iid not in [ap.id for ap in self.aps]:
				print str(iid) + " is bup !"


		positions = {} #Build a list of AP ids available at each 
		for ap in self.aps:
			for net in ap.networks:
				for det in net.detections:
					key = (det.sourceInd, det.scanInd)
					if key not in positions:
						positions[key] = []
					positions[key].append(ap.id)

		for pos in positions.values():
			for uuui in pos:
				if uuui not in [ap.id for ap in self.aps]:
					print str(uuui) + " is bulp !"
			if len([ap for ap in pos if ap in aWinrarIsAP]) == 0:
				retval += 1

		print "Uncovered : " + str(retval) + "/" + str(len(positions))
		return retval
