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

from datetime import date

METRIC_CODE = "2.5.2"
METRIC_NAME = "Scanresult Percentage Timeline"
OUT_NAME = "ScanresultPercentageTimeline"

DEFAULT_CONFIG = {
			'MIN_LEVELS':[-100, -90, -85, -80],
			"SINGLE":1,
		}

def plot(config):
	print "WARNING - This metric is intended to work with traces from non moving phones."

	for level in config['MIN_LEVELS']:
		plotFig(config, level)



def plotFig(config, minLevel):

	UniquesAP = []
	Timelines = []
	outdir = config['outdir']

	for source in config['data']:
		Timeline = {}

		for res in source.getScanResults():
			for nw in res:
				for det in nw.detections:
					if det.rssi > minLevel:
						if nw.bssid not in UniquesAP:
							UniquesAP.append(nw.bssid)
					
						if not Timeline.has_key(det.timestamp):
							Timeline[det.timestamp] = 1
						else:
							Timeline[det.timestamp] += 1
		

		Timelines.append(Timeline)	

	fig, axes = plt.subplots(len(Timelines), sharex=True, sharey=True)

	titleStr = "Number of detected scan results with minimal rssi " + str(minLevel)
	xLabel = "Time"
	yLabel = "Number of scan results"


	if len(Timelines) == 1:
		axes.set_title(titleStr)
		dataPoints = []
		for ts in sorted(Timelines[0].keys()):
			dataPoints.append([ts, Timelines[0][ts]])

		axes.set_ylabel(yLabel)
		axes.bar(*zip(*dataPoints))
		axes.axhline(y=len(UniquesAP))
		axes.text(0, len(UniquesAP) - 1, "total ap number : " + str(len(UniquesAP)))
		axes.set_xlabel(xLabel)

	else:
		axes[0].set_title(titleStr)

		for i in range(0, len(Timelines)):
			dataPoints = []
			for ts in sorted(Timelines[i].keys()):
				dataPoints.append([ts, Timelines[i][ts]])

			axes[i].set_ylabel(yLabel)
			axes[i].bar(*zip(*dataPoints))
			axes[i].axhline(y=len(UniquesAP))
		
		axes[len(Timelines) -1].set_xlabel(xLabel)
	
	output_file = outdir + METRIC_CODE + "_" + OUT_NAME + "_MIN_LEVEL=" + str(minLevel) + ".svg"
	fig.savefig(output_file)
