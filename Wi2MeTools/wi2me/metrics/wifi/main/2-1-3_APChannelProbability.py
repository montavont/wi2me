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


METRIC_CODE = "2.1.3"
METRIC_NAME = "Channel Access point occurence probability"
OUT_BASE_NAME = "channelAPProbability"

DEFAULT_CONFIG = {
	"CHANNELS":range(1, 13 + 1),
	"MINLEVELS":[-200, -80, -75],
	"COMMUNITY_NETWORKS":["FreeWifi", "SFR WiFi FON", "SFR WiFi Public"]
}

def plot(config):
	for minLevel in config["MINLEVELS"]:
		plotFig(config, minLevel, 0)
		plotFig(config, minLevel, 1)

def plotFig(config, minLevel, CNFiltering):
	channelCount = {}
	totalScans = 0
	Sources = config['data']


	for chan in config['CHANNELS']:
		channelCount[chan] = 0.0
	

	for source in Sources:

		results =  source.getScanResults()
		totalScans += len(results)
		for res in results:

			singleScanChanCount = {}

			for nw in res:
				for det in nw.detections:
					if (CNFiltering == 0 or nw.ssid in config['COMMUNITY_NETWORKS']) and det.rssi > minLevel and nw.channel in config['CHANNELS']:
						singleScanChanCount[nw.channel] = "FOUND YA"

			for chan in singleScanChanCount.keys():
				channelCount[chan] += 1
	
	if totalScans > 0:

		for chan in channelCount.keys():
			channelCount[chan] /= totalScans

		fig = plt.figure()

		plt.bar(channelCount.keys(), channelCount.values(), align='center')

		titleStr = ""
		if CNFiltering > 0:
			titleStr = "CN Access point occurence probability"
		else:
			titleStr = "Access point occurence probability"

		if minLevel > -200:
			titleStr += " with minimal RSSI " + str(minLevel)

		plt.title(titleStr)
		plt.xlabel("Channel Number")
		plt.ylabel("Probability")
		plt.xticks(config['CHANNELS'])
		
		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
		if CNFiltering > 0:
			output_file += "_CN"
		output_file += "_MinLevel=" + str(minLevel) + ".svg"
		fig.savefig(output_file)

		plt.close(fig)

                #text output
		output_file = output_file.replace('svg', 'txt')
                outF = open(output_file, 'w')

                for k in channelCount:
                    outF.write(str(k) + "    " + str(channelCount[k]) + "\n")

                outF.close()
