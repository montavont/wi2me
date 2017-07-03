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
import sys
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import operator
import numpy as np
from math import log10

METRIC_CODE = "2.1.10"
METRIC_NAME = "Single Position Access point occurence in scan results"
OUT_BASE_NAME = "SinglePositionAPOccurence"


#How to handle APs announcing multiple BSSID (Personnal AP, CN AP, SimAuthentification AP)
GROUP_NONE = 0	# Do not group
GROUP_GROUP = 1	# Regroup but plot difference bars
GROUP_MERGE = 2	# COnsider physical APS, if at least one answered

DEFAULT_CONFIG = {"MIN_LEVEL":-100, 'SINGLE':1, 'COMMUNITY_NETWORKS':["FreeWifi", "SFR WiFi FON", "SFR WiFi Public"], 'PLOT_GROUPED':GROUP_NONE, 'POWERFREC':{'mean':False, 'median':True, 'logarithmic mean':False, 'max':False, 'boxes':False}, "DISPLAY_APNAMES":False, 'DPI':300, 'MINID':-1, 'MERGE_THRESHOLD':256, "COMPLETE_COLOR":(0.1, 0.1, 0.1), "PARTIAL_COLOR":(0.7, 0.7, 0.7)}


def plot(config):
	print "WARNING : this metric is only intended to be used with traces from only one position"
	outdir = config['outdir']

	plotFig(config, [], GROUP_NONE)
	plotFig(config, [], GROUP_GROUP)
	plotFig(config, config["COMMUNITY_NETWORKS"], GROUP_NONE)



def makeTitle(Filter, grouping):
	retval = ""
	if len(Filter) > 0:
		for cn in Filter:
			retval += cn + " "
	if grouping == GROUP_GROUP:
		retval += "_GroupedAPs_"
	elif grouping == GROUP_MERGE:
		retval += "_MergeAPs_"

	return retval

#Returns True is ap1 > ap2
def compareMultiBSSAP(ap1, ap2):
	occ1 = []
	occ2 = []
	for k in ap1.keys():
		occ1.append(ap1[k])
	for k in ap2.keys():
		occ2.append(ap2[k])

	return max(occ1) > max(occ2)
		


def insertMultiBSSAP(ap, apList):
	inserted = False
	for i in range(0, len(apList)):
		if compareMultiBSSAP(apList[i], ap):
			apList.insert(i, ap)
			inserted = True
			break

	if not inserted:
		apList.append(ap)

	return apList

def sortMultiBSSAps(aps):
	retval = []
	for ap in aps:
		retval = insertMultiBSSAP(ap, retval)

	return retval



def plotFig(config, CNFilter, apGrouping):
	outdir = config['outdir']
	minLevel = config['MIN_LEVEL']
	
	bssidTranslator = {}
	APOccurence = {}

	numScans = 0
	bestAPLevel = -100

	for source in config['data']:

		results = source.getScanResults()
		numScans += len(results)
		for res in results:
			for nw in res:
				for det in nw.detections:
					if det.rssi > minLevel and (len(CNFilter) == 0 or nw.ssid in CNFilter):
						bssidTranslator[nw.bssid] = nw.ssid

						if APOccurence.has_key(nw.bssid):
							APOccurence[nw.bssid]['count'] += 1
							APOccurence[nw.bssid]['levels'].append(det.rssi)
						else:
							APOccurence[nw.bssid] = {'count':1, 'levels':[det.rssi]}


	font = {
        	'weight' : 'normal',
	        'size'   : 20}

	matplotlib.rc('font', **font)
	fig = plt.figure()

	#Regroup BSS that differ by less than MERGE_THRESHOLD (3) as a single AP
	if apGrouping == GROUP_GROUP:
		newOccurence = []
		singleAPOccurence = {}
		singleAPReversed = {}
		occurence = {}
		lastKey = 0
		for k in sorted(APOccurence.keys()):
			intK = int(k.replace(":", ""), 16)
			if intK - lastKey < config['MERGE_THRESHOLD']:
				occurence[k] = float(APOccurence[k]['count']) /numScans
			else: 
				if len(occurence.keys()) > 1:
					newOccurence.append(occurence)
				#Sort single occurences appart for now, in order to group bssid grouped by first Byte
				elif len(occurence.keys()) == 1:
					singleAPOccurence[occurence.keys()[0]] = occurence.values()[0]
				occurence = {k:float(APOccurence[k]['count']) / numScans}
			lastKey = intK

		if len(occurence.keys()) > 1:
			newOccurence.append(occurence)
		elif len(occurence.keys()) == 1:
			singleAPOccurence[occurence.keys()[0]] = occurence.values()[0]

		#Reverse the single BSSs and compare the to the other ones
		for sBssid in singleAPOccurence.keys():
			reversedBssid = ""
			for b in reversed(sBssid.split(':')):
				reversedBssid += b + ":"
			singleAPReversed[reversedBssid.rstrip(':')] = sBssid
			
		lastKey = 0
		occurence = {}
		for r in sorted(singleAPReversed.keys()):
			intK = int(r.replace(":", ""), 16)
			k = singleAPReversed[r]
			if intK - lastKey < config['MERGE_THRESHOLD']:
				occurence[k] = float(APOccurence[k]['count']) /numScans
			else: 
				if len(occurence.keys()) > 0:
					newOccurence.append(occurence)
				occurence = {k:float(APOccurence[k]['count']) / numScans}

			lastKey = intK

			
		newOccurence = sortMultiBSSAps(newOccurence)

		maxlen = 0
		#concatenate AP names for XTicks
		APTicks = []
		for ap in newOccurence:
			val = ""
			maxlen = max(maxlen, len(ap))
			for bss in ap:
				if len(bssidTranslator[bss]) > 0:
					val += "\n" + bssidTranslator[bss]
				else: 
					val += "\n" + bss  
			APTicks.append(val)

		width = 1.0 / (maxlen + 1)
		for i in range(0, maxlen):
			APS = []
			colors = []
			for ap in newOccurence:
				keys = sorted(ap.keys())
				if len(keys) > i:
					APS.append(ap[keys[i]])
					if ap[keys[i]] == 1.0:
						colors.append(config["COMPLETE_COLOR"])
					else:
						colors.append(config['PARTIAL_COLOR'])
				else:
					APS.append(0)
					colors.append("blue")
			rects = plt.bar(np.array(range(0, len(newOccurence))) + width * i, APS, width, color=colors, edgecolor="none")
		if config['DISPLAY_APNAMES']:
			plt.xticks(range(len(APTicks)), APTicks, rotation=90)
	
		plt.xlabel(str(len(newOccurence)) + " Access Points announcing " + str(len(APOccurence)) + " BSSs")

	#Treat each BSS as a different AP
	elif apGrouping == GROUP_NONE:
		sortedOccurences = []
		for occ in sorted(APOccurence.iteritems(), key=operator.itemgetter(1)):
			sortedOccurences.append([occ[0], occ[1]['count']])
		translatedAPs = []
		translatedAPCounts = []
		colors = []
		for u in sortedOccurences:
			if len(bssidTranslator[u[0]]) > 0:
				translatedAPs.append(bssidTranslator[u[0]])
			else:
				translatedAPs.append(u[0])
			translatedAPCounts.append(float(u[1]) / numScans)

			if u[1] == numScans:
				colors.append(config["COMPLETE_COLOR"])
			else:
				colors.append(config["PARTIAL_COLOR"])
	
		plt.bar(range(len(translatedAPCounts)), translatedAPCounts, align='center', color=colors, edgecolor="none")
		if config['DISPLAY_APNAMES']:
			plt.xticks(range(len(translatedAPs)), translatedAPs, rotation=90)
		plt.xlabel("APs")
		plt.xlim([-1, len(translatedAPs) + 1])



	cnTitle = ""
	if len(CNFilter) > 0:
		cnTitle = " community networks"
	plt.ylabel("AP appearance frequency")

	output_file = outdir + METRIC_CODE + "_" + OUT_BASE_NAME
	output_file += makeTitle(CNFilter, apGrouping)
	output_file += "_MinLevel=" + str(minLevel) + ".svg"
	fig.savefig(output_file, dpi = config['DPI'], bbox_inches='tight')
	plt.close(fig)

	#Plot Power depending on appearance frequency in differente ways
	if apGrouping == GROUP_NONE:
		for Type in config['POWERFREC'].keys():
			if config['POWERFREC'][Type]:

				powerApps = []

				fig = plt.figure()
				plt.ylim([-100, bestAPLevel + 5 ])
				# this requires diffent treatment : 
				if Type =='boxes':
					positions = []
					boxes = []
					for ap in APOccurence.keys():
						boxes.append(APOccurence[ap]['levels'])
						positions.append(APOccurence[ap]['count'])
					plt.boxplot(boxes, positions=positions)
					plt.xticks([])

				else: 
					if Type == 'mean':
						for ap in APOccurence.keys():
							mean = reduce(lambda x, y: x + y, APOccurence[ap]['levels']) / len(APOccurence[ap]['levels'])
							powerApps.append([APOccurence[ap]['count'], mean])

					elif Type == 'median':
						for ap in APOccurence.keys():
							median = sorted(APOccurence[ap]['levels'])[len(APOccurence[ap]['levels'])//2]
							powerApps.append([APOccurence[ap]['count'], median])

					elif Type =='logarithmic mean':
						for ap in APOccurence.keys():
							totalPWatts = 0
							for level in APOccurence[ap]['levels']:
								totalPWatts += 10**((level - 30) / 10.0)
							totalPWatts /= len(APOccurence[ap]['levels'])
							logMean = 10 * log10(totalPWatts) + 30
							powerApps.append([APOccurence[ap]['count'], logMean])

					elif Type =='max':
						for ap in APOccurence.keys():
							powerApps.append([APOccurence[ap]['count'], max(APOccurence[ap]['levels'])])

					powerApps = sorted(powerApps)
					

					plt.plot(np.array(zip(*powerApps)[0]) / float(numScans), zip(*powerApps)[1], "bo")

				#Linear regression
				m, b = np.polyfit(*zip(*powerApps), deg=1)
        			yp = np.polyval([m, b], [min(zip(*powerApps)[0]), max(zip(*powerApps)[0])])
			        plt.plot(yp, color="green")

				plt.xlabel("Frequency")
				plt.ylim([-100, -70])
				plt.ylabel("Median Power (dBm)")
				
				output_file = outdir + METRIC_CODE + "_" + OUT_BASE_NAME
				output_file += makeTitle(CNFilter, apGrouping)
				output_file += "_" + Type + "_power_" + "_MinLevel=" + str(minLevel) + ".svg"
				fig.savefig(output_file, bbox_inches='tight')
				plt.close(fig)

