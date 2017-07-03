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
import numpy as np
from wi2me.utils import MapHelper


METRIC_CODE = "2.1.5"
METRIC_NAME = "Access point coverage distribution"
OUT_BASE_NAME = "APCoverageDistribution"

DEFAULT_CONFIG = {
	"COMMUNITY_NETWORKS":["FreeWifi", "SFR WiFi FON", "SFR WiFi Public"],
	"GRANULARITY" : 200, #in meters
	"AP_MIN_LEVEL" : -100,
	"TEXT_OUTPUT" : False
}

		

def makeTitle(Filter, cns):
	retval = ""
	if len(Filter) == len(cns) :
		retval = "Undifferencied CN "
	elif len(Filter) > 0:
		for cn in Filter:
			retval += cn + " "
	return retval


def plot(config):
	outdir = config['outdir']
	APPositions = {}
	APTraces = {}
	APTrajectories = {}
	APLocations = {}
	APDistances = {}

	CNFilter = []

	for source in config['data']:
		for nw in source.getApList():
			for det in nw.detections:
				latitude, longitude = det.GPS
				if det.rssi > config["AP_MIN_LEVEL"] and latitude * longitude != 0:
			
					detData = [det.rssi, latitude, longitude]

			
					if APLocations.has_key(nw.bssid):
						added = 0
						#Bssids are reused, compare to the previously found values and aggregate them depending on a max distance
						for i in range(0, len(APLocations[nw.bssid])):
							distance = MapHelper.meterDist(APLocations[nw.bssid][i]["center"], detData[1:])
							if distance < config['GRANULARITY']:
								added = 1
								APLocations[nw.bssid][i]["range"] = max(distance, APLocations[nw.bssid][i]["range"])
								APLocations[nw.bssid][i]["data"].append(detData)
								APLocations[nw.bssid][i]["center"] = MapHelper.locateCenter(APLocations[nw.bssid][i]["data"])
								break

						if added < 1:
							APLocations[nw.bssid].append({"data":[detData], "center":detData[1:], "range":0})
					else:
						APLocations[nw.bssid] = [{"data":[detData], "center":detData[1:], "range":0}]

	for bss in APLocations.keys():
		for loc in APLocations[bss]:
			dist = loc["range"]
			if dist > 0:
				if APDistances.has_key(dist // 1):
					APDistances[dist // 1] += 1
				else:
					APDistances[dist // 1] = 1


	numberAPs = sum(APDistances.values())

	if numberAPs == 0:
		print "No traceable ap found for filter : " + str(CNFilter)
	else:

		NormalizedDistanceOccurence = {}
		for k in APDistances.keys():
			NormalizedDistanceOccurence[k] = float(APDistances[k]) / numberAPs

		fig, ax = plt.subplots()

		#CDF Plotting
		cdf =  ax.twinx()
		cdf.yaxis.set_label_position('right')
		cdf.yaxis.set_ticks_position('right')
		cdf.set_ylabel("CDF")
		cdf.patch.set_visible(False)
		cdf.plot(sorted(NormalizedDistanceOccurence.keys()), np.cumsum(NormalizedDistanceOccurence.values()), 'red')

		#Distribution Plotting
		ax.bar(APDistances.keys(), APDistances.values(), align='center')
		ax.set_ylabel("Occurences")
		ax.set_xlabel('Coverage distance (m)')

		plt.title("Coverage distribution for " + makeTitle(CNFilter, config["COMMUNITY_NETWORKS"]) + "access points")
		plt.xlim(xmin = 0)

		output_file = outdir + METRIC_CODE + "_" + OUT_BASE_NAME + makeTitle(CNFilter, config["COMMUNITY_NETWORKS"]) + ".svg"
		fig.savefig(output_file)

		if config['TEXT_OUTPUT']:
			f = open(outdir + METRIC_CODE + "_" + OUT_BASE_NAME + makeTitle(CNFilter, config["COMMUNITY_NETWORKS"]) + ".txt", 'w')
			for k in APDistances.keys():
				f.write(str(k) + "	" + str(APDistances[k]) + "\n")
			f.close()


