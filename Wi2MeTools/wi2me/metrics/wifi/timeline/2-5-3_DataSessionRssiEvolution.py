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

METRIC_CODE = "2.5.3"
METRIC_NAME = "Data Session Rssi Timeline"
OUT_NAME = "transferedBytesTimeline"
OUT_EXT = ".svg"

DEFAULT_CONFIG = {
		}

def plot(config):
	outdir = config['outdir']

	DataSessions = {}
	InterpolatedRssis = {}

	for Dict in [DataSessions, InterpolatedRssis]:
		for source in config['data']:
			if len(source.tags) > 0:
				for tag in source.tags:
					Dict[tag] = []
			else:
				Dict[source.name] = []



	for source in config['data']:
		srcDataSessions = []

		Data = source.getTransferredData()
		lastProg = 0

		if len(Data) > 0:
			Session = []

			for tData in Data:
		
				if tData.progress < lastProg and len(Session) > 0:
					srcDataSessions.append(Session)
					Session = []

				rssi = tData.network.detections[0].rssi
				if rssi != -200:
					Session.append(rssi)
				lastProg = tData.progress

			if len(Session) > 0:
				srcDataSessions.append(Session)


	
		if len(source.tags) > 0:
			for tag in source.tags:
				DataSessions[tag] += srcDataSessions
		else:
			DataSessions[source.name] += srcDataSessions

	
	for k in DataSessions:
                if len(DataSessions[k]) > 0:
			finalLength = max([len(s) for s in DataSessions[k]])

			for _ in range(finalLength):
				InterpolatedRssis[k].append([])	

			for sess in DataSessions[k]:
				if len(sess) > 1:
					i = 0
					step = (finalLength - len(sess)) / (len(sess) - 1)
					rest = finalLength - step * (len(sess) -1) - len(sess)
			
					for vIndex, value in enumerate(sess):
						InterpolatedRssis[k][i].append(value)
						i += 1
									
						interpolated = value
						if vIndex != len(sess) - 1:
							for j in range(step):
								interpolated = value + float(j + 1) / step * (sess[vIndex + 1] - value)
								InterpolatedRssis[k][i + j].append(interpolated)
			
							i += step

						if rest > 0:
							InterpolatedRssis[k][i].append(interpolated)
							i += 1
							rest -= 1
						
		

	for k in DataSessions:	
		fig = plt.figure()

		xLabel = "Observation Points"
		yLabel = "Rssi"


		for sess in DataSessions[k]:
			plt.plot(range(	len(sess)), sess)

		plt.xlabel(xLabel)
		plt.ylabel(yLabel)

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_DataSessionsRSSIS_" + k + OUT_EXT
		fig.savefig(output_file)

		plt.close()


	fig = plt.figure()

	xLabel = "Relative Observation Points"
	yLabel = "Mean Rssi"


	for k in DataSessions:	
		plt.plot([float(y)/len(InterpolatedRssis[k]) for y in range(len(InterpolatedRssis[k]))], [np.mean(o) for o in InterpolatedRssis[k]], label=k)


	plt.legend()
	plt.xlabel(xLabel)
	plt.ylabel(yLabel)

	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME + "_rssi_evo" + OUT_EXT
	fig.savefig(output_file)

	plt.close()
