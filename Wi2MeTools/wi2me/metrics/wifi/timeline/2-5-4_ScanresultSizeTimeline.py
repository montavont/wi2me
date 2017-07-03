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

METRIC_CODE = "2.5.4"
METRIC_NAME = "Scanresult Size Timeline"
OUT_NAME = "ScanresultSizeTimeline"

DEFAULT_CONFIG = {
		}

def plot(config):

	UniquesAP = []
	Timelines = []
	outdir = config['outdir']

	for source in config['data']:
		Timeline = {}

		for res in source.getScanResults():
                        ts = res[0].detections[0].timestamp
			Timeline[ts] = len(res)
		
		Timelines.append(Timeline)	

	fig, axes = plt.subplots(len(Timelines), sharex=True, sharey=True)

	xLabel = "Time"
	yLabel = "Number of response in scan results"


	if len(Timelines) == 1:
		dataPoints = []
		for ts in sorted(Timelines[0].keys()):
			dataPoints.append([ts, Timelines[0][ts]])

		axes.set_ylabel(yLabel)
		axes.bar(*zip(*dataPoints))
		axes.set_xlabel(xLabel)

	else:
		for i in range(0, len(Timelines)):
			dataPoints = []
			for ts in sorted(Timelines[i].keys()):
				dataPoints.append([ts, Timelines[i][ts]])

                        colors = ['black' for u in range(len(dataPoints))]
                        colors[max(range(len(dataPoints)), key = lambda x : dataPoints[x][1])] = "green"
                        colors[min(range(len(dataPoints)), key = lambda x : dataPoints[x][1])] = "red"

			axes[i].set_ylabel(yLabel)
			axes[i].bar(*zip(*dataPoints), edgecolor = colors)
		
		axes[len(Timelines) -1].set_xlabel(xLabel)
	
	output_file = outdir + METRIC_CODE + "_" + OUT_NAME + ".svg"
	fig.savefig(output_file)
