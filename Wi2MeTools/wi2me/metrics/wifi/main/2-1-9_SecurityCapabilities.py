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
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.distributions.empirical_distribution import ECDF
import operator

METRIC_CODE = "2.1.9"
METRIC_NAME = "Security Capabilities"
OUT_BASE_NAME = "SecurityHistogram.svg"


DEFAULT_CONFIG={"BAR_COLOR":"green", "MERGED_PLOT":True, "NON_CAPABILITIES":["", "ESS"]}


def plot(config):

	Capabilities = {}
	CapabilitiesCount = {}
		
	for source in config['data']:
		for res in source.getScanResults():
			for ap in res:
				Capabilities[ap.bssid] = ap.capabilities
	

	totalAPNumber = len(Capabilities)

	for cap in Capabilities.values():
		if len(cap) > 0:
			caps = cap.replace('[', '').split(']')
			caps = caps[:len(caps) - 1]
			if len(caps) > 1:
				for secType in caps:
					if CapabilitiesCount.has_key(secType):
						CapabilitiesCount[secType] += 1
					else:
						CapabilitiesCount[secType] = 1
			elif len(caps) == 1 and caps[0] == u"ESS":
				if CapabilitiesCount.has_key('OPEN'):
					CapabilitiesCount["OPEN"] += 1
				else:
					CapabilitiesCount["OPEN"] = 1

	for nonSup in config['NON_CAPABILITIES']:
		if CapabilitiesCount.has_key(nonSup): #Not a security type
			del(CapabilitiesCount[nonSup])

	if len(CapabilitiesCount) > 0:
		
		for k in CapabilitiesCount.keys():
			CapabilitiesCount[k] /= float(totalAPNumber)

		SortedCapabilitiesCount = sorted(CapabilitiesCount.iteritems(), key=operator.itemgetter(1))

		fig = plt.figure()

		plt.bar(range(len(SortedCapabilitiesCount)), zip(*SortedCapabilitiesCount)[1], color=config['BAR_COLOR'], align="center")
		plt.xticks(range(len(CapabilitiesCount.keys())), zip(*SortedCapabilitiesCount)[0], rotation = 90)
		plt.ylabel("Probability of occurence")
		plt.xlabel("Security Type")

		fig.tight_layout()

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
		fig.savefig(output_file)
		
		plt.close(fig)

		if config['MERGED_PLOT']:
			MergedCaps = {"WPA":0, "WPA2":0, "WPS":0, "WEP":0, "OPEN":0}
			for k in Capabilities.values():
				if "WPA-" in k:
					MergedCaps["WPA"] += 1
				if "WPA2-" in k:
					MergedCaps["WPA2"] += 1
				if k == "[ESS]": #Eveyone has the ESS flag, open networks only have this one
					MergedCaps["OPEN"] += 1
				if "WPS" in k:
					MergedCaps["WPS"] += 1
				if "WEP" in k:
					MergedCaps["WEP"] += 1
		
			for k in MergedCaps.keys():
				MergedCaps[k] /= float(totalAPNumber)
				
			fig = plt.figure()
		
			SortedMergedCaps = sorted(MergedCaps.iteritems(), key=operator.itemgetter(1))

			plt.bar(range(len(SortedMergedCaps)), zip(*SortedMergedCaps)[1], color=config['BAR_COLOR'], align="center")
			plt.xticks(range(len(MergedCaps.keys())), zip(*SortedMergedCaps)[0], rotation = 90)
			plt.ylabel("Probability of occurence")
			plt.xlabel("Security Type")

			fig.tight_layout()

			output_file = config['outdir'] + METRIC_CODE + "_MERGED_" + OUT_BASE_NAME
			fig.savefig(output_file)

			plt.close(fig)
