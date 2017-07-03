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


METRIC_CODE = "2.1.7"
METRIC_NAME = "Inter Channel Interference"
OUT_BASE_NAME = "InterChannelInterference.svg"


DEFAULT_CONFIG={"OVERLAP_COLOR":"red", 'NONOVERLAP_COLOR':'green', 'CHANNELS':[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13], 'INTERF_THRESHOLD':4}


def plot(config):

	ScanResults = []
	totalDistances = 0
	Distances = []
		
	Interference = {}
	for chan in range(0, max(config['CHANNELS'])):
		Interference[chan] = 0.0




	
	for source in config['data']:
		for res in source.getScanResults():
			for i in range(0, len(res)):
				for j in range(i + 1, len(res)):
					ap1 = res[i].channel
					ap2 = res[j].channel
					if ap1 in config['CHANNELS'] and ap2 in config['CHANNELS']:
						totalDistances  += 1
						d =  abs(ap1 - ap2)
						Interference[d] += 1
						Distances.append(d)


	if totalDistances > 0:	

		for k in Interference.keys():
			Interference[k] /= totalDistances


		fig, ax = plt.subplots()

		#Distribution Plotting
		colors = []
		for i in range(0, config['INTERF_THRESHOLD']):
			colors.append(config['OVERLAP_COLOR'])
		for i in range(config['INTERF_THRESHOLD'], len(Interference.values())):
			colors.append(config['NONOVERLAP_COLOR'])

		ax.bar(Interference.keys(), Interference.values(), color=colors, align='center')
		ax.set_ylabel("Relative Frequency Histogram")
		ax.set_xlabel("Separation between channels")

		#CDF Plotting
		cdf =  ax.twinx()
		cdf.yaxis.set_label_position('right')
		cdf.yaxis.set_ticks_position('right')
		cdf.set_ylabel("CDF")
		cdf.patch.set_visible(False)
		ecdf = ECDF(Distances)
		cdf.step(ecdf.x, ecdf.y)

		plt.xticks(range(0, len(config['CHANNELS'])))
		plt.xlim([-0.5, max(config['CHANNELS'])])

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
		fig.savefig(output_file)
