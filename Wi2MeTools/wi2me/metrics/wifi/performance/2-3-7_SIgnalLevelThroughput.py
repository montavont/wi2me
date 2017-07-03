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

METRIC_CODE = "2.3.7"
METRIC_NAME = "Wifi Throughput depending on RSSI"
OUT_BASE_NAME = "ThroughputDependingOnRssi.svg"


DEFAULT_CONFIG={
		#"SINGLE":1
		}

def plot(config):
	outdir = config['outdir']

	BytePoints = []

	for source in config['data']:

		DataRows = source.getTransferredData()


		lastBytes = 0
		lastTs = 0

		

		for data in DataRows:
			if data.progress >= lastBytes:
				ts = data.network.detections[0].timestamp
				if ts > lastTs and lastTs > 0:
					BytePoints.append([data.network.detections[0].rssi, float(data.progress - lastBytes) / (ts - lastTs) * 1000])
				lastBytes = data.progress
				lastTs = ts
			else:
				lastBytes = 0
				lastTs = 0
		
		if len(BytePoints) > 0:

			fig = plt.figure()

			plt.plot(np.array(zip(*BytePoints)[0]), zip(*BytePoints)[1], 'ro')
			plt.ylabel("Throughput (Bytes/s)")
			plt.xlabel("Rssi (dBm)")

			output_file = outdir + METRIC_CODE + "_" + OUT_BASE_NAME
			fig.savefig(output_file, bbox_inches='tight')

			plt.close(fig)
