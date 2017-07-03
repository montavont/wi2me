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

from statsmodels.distributions.empirical_distribution import ECDF

METRIC_CODE = "2.3.20"
METRIC_NAME = "SPDY vs MPTCP diffports web page downloading performance comparison"
OUT_NAME = "SPDY_vs_MPTCP"
OUT_EXT = ".svg"

DEFAULT_CONFIG = {
		"COLORS":
			{
				"SPDY_NDIFF":"cyan",
				"SPDY_VANILLA":'blue',
				"WEB":"orange",
				"NDIFFPORTS":"green"
			},
		"SINGLE":1
		}

TRAJ_KEYS = ["SPDY_NDIFF", "SPDY_VANILLA", "WEB", "NDIFFPORTS"] 

def plot(config):
	outdir = config['outdir']

	Trajs = {}
	loopTrajs = {}
	starts = {}
	for k in TRAJ_KEYS:
		Trajs[k] = []
		loopTrajs[k] = []
		starts[k] = 0

	for source in config['data']:

		startTime = source.getStartTime()

		Data = source.getTransferredData()

		for dataP in Data:
			traffic = ""
			for tr in TRAJ_KEYS:
				if tr in dataP.direction:
					traffic = tr
					break			

			if traffic in loopTrajs:
 				if "FINISH_COMPLETE" in dataP.direction:
					dataP.network.detections[0].timestamp -= starts[traffic]
					#if dataP.ap.timestamp < 10000:
					#	loopTrajs[traffic].append(dataP)
					loopTrajs[traffic].append(dataP)
				elif "START" in dataP.direction:
					if len(loopTrajs[traffic]) > 0:
						Trajs[traffic].append(loopTrajs[traffic])
					loopTrajs[traffic] = []
					starts[traffic] = dataP.network.detections[0].timestamp

		for traffic in loopTrajs:
			if len(loopTrajs[traffic]) > 0:
				Trajs[traffic].append(loopTrajs[traffic])

	
	lengths = [len(t) for traf in Trajs for t in Trajs[traf]]
	if len(lengths) > 0:
		numFiles = max(lengths)

		#Transferred files plot
		fig = plt.figure()

		for traffic in Trajs:
			for traj in Trajs[traffic]:
				X = [p.network.detections[0].timestamp for p in traj]
				Y = [ float(val)/numFiles for val in range(len(traj)) ]
				plt.plot(X, Y, color = config['COLORS'][traffic])

		plt.xlabel("Time (ms)")
		plt.ylabel("Part of downloaded files")

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_DownloadedFiles" + OUT_EXT
		fig.savefig(output_file)

		#Transferred data plot
		fig = plt.figure()
		
		for traffic in Trajs:
			for traj in Trajs[traffic]:
				X = [p.network.detections[0].timestamp for p in traj]
				Y = []
				y = 0
				for p in traj:
					y += p.progress
					Y.append(y)
				plt.plot(X, Y, color = config['COLORS'][traffic])

		plt.xlabel("Time (ms)")
		plt.ylabel("Downloaded Bytes")

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_DownloadedBytes" + OUT_EXT
		fig.savefig(output_file)

		plt.close(fig)

		#Transfer time CDFs
		fig = plt.figure()
		
		xTicks = []
		boxData = []

		for traffic in Trajs:
			trafCount = 0
			transTimes = []
			for traj in Trajs[traffic]:
				#Only consider finished transfers
				if len(traj) == numFiles:	
					trafCount += 1
					transTimes.append(max([p.network.detections[0].timestamp for p in traj]))

			#plt.boxplot(transTimes,0,'')

			ecdf = ECDF(transTimes)
			plt.plot(ecdf.x, ecdf.y, color = config['COLORS'][traffic])

			boxData.append(transTimes)
			xTicks.append(traffic + " (" + str(trafCount) + ")")

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_TransTimeCDFs" + OUT_EXT
		fig.savefig(output_file)
		plt.close(fig)

		#Transfer time boxplots
		fig = plt.figure()
		fig = plt.figure()
		plt.boxplot(boxData)
		plt.xticks([a + 1 for a in range(len(xTicks))], xTicks)

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_TransTimeBoxes" + OUT_EXT
		fig.savefig(output_file)
		plt.close(fig)
