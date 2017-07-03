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

METRIC_CODE = "2.5.1"
METRIC_NAME = "Transfered Bytes Timeline"
OUT_NAME = "transferedBytesTimeline.svg"

DEFAULT_CONFIG = {
			"UNOFFSET":True
		}

def plot(config):
	outdir = config['outdir']

	Timelines = []

	for source in config['data']:

		startTime = source.getStartTime()

		DataPoints = []
		Data = source.getTransferredData()

		if len(Data) > 0:
			lastBytes = [Data[0].network.detections[0].timestamp, Data[0].progress]
			if config['UNOFFSET']:
				lastBytes[0] -= startTime
			
			#for row in DataRows:
			for tData in Data[1:]:
				Timestamp = tData.network.detections[0].timestamp #, Bytes = row
		
				if config['UNOFFSET']:
					Timestamp -= startTime	
		
				DataPoints.append(lastBytes)
				if tData.progress <= lastBytes[1]:
					DataPoints.append([lastBytes[0]+1, 0])

				lastBytes = [Timestamp, tData.progress]

			DataPoints.append(lastBytes)

		Timelines.append(DataPoints)


	fig, axes = plt.subplots(len(Timelines), sharex=True, sharey=True)
	titleStr = "Transfered Data Timeline"
	xLabel = "Time"
	yLabel = "Transferred Data (Bytes)"


	if len(Timelines) == 1:

		axes.set_title(titleStr)
		axes.set_ylabel(yLabel)
		if len(Timelines[0]) > 0:
			axes.plot(*zip(*Timelines[0]))
		axes.set_xlabel(xLabel)

	else:
		axes[0].set_title(titleStr)

		for i in range(0, len(Timelines)):
			axes[i].set_ylabel(yLabel)
			if len(Timelines[i]) > 0:
				axes[i].plot(*zip(*Timelines[i]))
		
		axes[len(Timelines) -1].set_xlabel(xLabel)


	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_NAME
	fig.savefig(output_file)

	plt.close()
