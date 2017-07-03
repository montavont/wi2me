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
from PIL import Image
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.distributions.empirical_distribution import ECDF

METRIC_NAME = "Time covered and uncovered by a specific service or operator"
METRIC_CODE = "2.2.7"

OUT_EXT = ".svg"
TXT_EXT = ".txt"
DEFAULT_CONFIG = {
			'SSIDS':['FreeWifi', 'orange', 'Bouygues Telecom Wi-Fi', 'IciWifi gratuit', 'eduroam'],
			'SSID_PREFS':['SFR WiFi ', 'SFR_', 'Livebox-', 'NUMERICABLE-', 'Bbox-', 'NEUF_', 'freebox', "DartyBox_"],
			'ADD_INCOMPLETE':False,#Add the incomplete coverage values we might get at the end of a trace. That value should be unusually shorter...
			'BSSID_FILES_FOLDER':""
		}

#Matches all SSIDS (Basically detects empty scan results...no use in most cases)
class allMatcher:
	def __init__(self):
		self.name = "ALL_SSIDS"
		self.bssidFile = ""

	def match(self, network):
		return True

#Matches a specific SSID
class ssidMatcher:
	def __init__(self, ssid):
		self.ssid = ssid
		self.name = "SSID_MATCHER_" + ssid
		self.bssidFile = ""
		

	def match(self, network):
		return network.ssid == self.ssid

#Matches SSIDs with a specific prefix
class ssidPrefixMatcher:
	def __init__(self, prefix):
		self.prefix = prefix.lower()
		self.name = "SSID_PREFIX_" + prefix
		self.bssidFile = ""

	def match(self, network):
		return network.ssid.lower().startswith(self.prefix)


def plot(config):

	matchers = []
	for ssid in config['SSIDS']:
		matchers.append(ssidMatcher(ssid))

	for prefix in config['SSID_PREFS']:
		matchers.append(ssidPrefixMatcher(prefix))		


	#Check if a bssid restriction files folder was passed. If so add it to the matchers
	if os.path.isdir(config['BSSID_FILES_FOLDER']):
		bssidFiles = [ os.path.join(config['BSSID_FILES_FOLDER'],f) for f in os.listdir(config['BSSID_FILES_FOLDER']) if os.path.isfile(os.path.join(config['BSSID_FILES_FOLDER'],f)) ]
		for matcher in matchers:
			if len([f for f in bssidFiles if matcher.name in f]) == 1:
				matcher.bssidFile = [f for f in bssidFiles if matcher.name in f][0]
			else:
				print "Incorrect number of BssidFiles found for matcher " + matcher.name + "	" + str(len([f for f in bssidFiles if matcher.name in f]))

	for matcher in matchers:

		bssidRestrictions = {"ALL":[]}
		if len(matcher.bssidFile) > 0:
			bssidRestrictions["SUBSET"] = [bss.rstrip('\n') for bss in open(matcher.bssidFile, 'r').readlines()]
		
		coveredTimes = {}
		uncoveredTimes = {}

		ignoreIndex = []
		for restKey in bssidRestrictions:
			bssidRestriction = bssidRestrictions[restKey]
			coveredTimes[restKey] = []
			uncoveredTimes[restKey] = []

			for sIndex, source in enumerate(config['data']):
				sourceCovTimes = []
				sourceUncovTimes = []
				if sIndex in ignoreIndex:
					continue

				results = [res for res in source.getScanResults() if len(res) > 0]
				if len(results)  > 0:
					lastTs = results[0][0].detections[0].timestamp
					initCovered = covered = len([det for det in results[0] if matcher.match(det)]) > 0
					if len(bssidRestriction) > 0:
						covered = covered and len([det for det in res if det.bssid in bssidRestriction]) > 0
					coveredTime = 0
					uncoveredTime = 0

					for res in results[1:]:
						match = len([det for det in res if matcher.match(det)]) > 0
						if len(bssidRestriction) > 0:
							match = match and len([det for det in res if det.bssid in bssidRestriction]) > 0

						ts = res[0].detections[0].timestamp
						if match:
							if uncoveredTime > 0 and not covered:
								sourceUncovTimes.append(uncoveredTime + ts - lastTs)
								uncoveredTime = 0
							coveredTime += ts - lastTs
						else:
							if coveredTime > 0 and covered:
								sourceCovTimes.append(coveredTime)
								coveredTime = 0
							uncoveredTime += ts - lastTs
						covered = match
						lastTs = ts

					#[NOT ADVISED] We can also use these incomplete values, although they would just be minimal
					if config['ADD_INCOMPLETE']:
						if coveredTime > 0:
							sourceCovTimes.append(coveredTime)
						if uncoveredTime > 0:
							sourceUncovTimes.append(uncoveredTime)
					#Otherwise, if a source was covered at 0 or 100%, this is not a valid coverage time, ignore it in the following runs
					else:
						sDuration = results[-1][0].detections[0].timestamp - results[0][0].detections[0].timestamp
						#If we had a 0 or 100% coverage and using no restriction, this trace in not usable for the next ones. Add it to the balcklist
						if sDuration in [coveredTime, uncoveredTime] and len(bssidRestriction) == 0:
							ignoreIndex.append(sIndex)

						#We also need to delete the first time we observed on that trace, since wo do not know its beginning for sure
						if initCovered:
							sourceCovTimes = sourceCovTimes[1:]
						else:
							sourceUncovTimes = sourceUncovTimes[1:]

				coveredTimes[restKey] += sourceCovTimes
				uncoveredTimes[restKey] += sourceUncovTimes
					

			for data, name in [(coveredTimes[restKey], "Coverage"), (uncoveredTimes[restKey], "Non-Coverage")]:
				if len(data) > 0:
					if len(bssidRestriction) > 0:
						outPath = config['outdir'] + METRIC_CODE + "_" + name + "Times_" + matcher.name + "_RestrictedTo_" + str(len(bssidRestriction)) + "bssids_" + str(len(data)) + "_points"
					else:
						outPath = config['outdir'] + METRIC_CODE + "_" + name + "Times_" + matcher.name + "_" + str(len(data)) + "_points"
					fig = plt.figure()
					ecdf = ECDF(data)
					cdfFile = open(outPath + TXT_EXT, 'w')
					for x, y in zip(*[ecdf.x, ecdf.y]):
						cdfFile.write(str(x) + "	" + str(y) + "\n")
					cdfFile.close()

					plt.step(ecdf.x, ecdf.y)
					plt.ylabel("CDF")
					plt.xlabel(name + " Times (ms)")

					fig.tight_layout()
					fig.savefig(outPath + OUT_EXT)

					plt.close(fig)
				else:
					print "No data for " + name + "	" + matcher.name
					

		for ddict, name in [(coveredTimes, "Coverage"), (uncoveredTimes, "Non-Coverage")]:

			outPath = config['outdir'] + METRIC_CODE + "_" + name + "Times_" + matcher.name + "_COMBINED_" 

			fig = plt.figure()

			for key in ddict:
				if len(ddict[key]) > 0:
					ecdf = ECDF(ddict[key])
					plt.step(ecdf.x, ecdf.y, label=key)

			plt.ylabel("CDF")
			plt.xlabel(name + " Times (ms)")
			plt.legend()

			fig.tight_layout()
			fig.savefig(outPath + OUT_EXT)
			plt.close(fig)
