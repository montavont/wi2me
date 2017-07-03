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


METRIC_CODE = "2.1.8"
METRIC_NAME = "Intra Channel Interference"
OUT_BASE_NAME = "IntraChannelInterference.svg"


DEFAULT_CONFIG={"BAR_COLOR":"red", 'CHANNELS':[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13]}


def plot(config):

	interfCount = 0
	InterfList = []
	InterfCount = {}
		

	for source in config['data']:

		for rres in source.getScanResults():
			res = [r.channel for r in rres]

			for chan in config['CHANNELS']:
				interf = res.count(chan)
				if interf > 0:
					interfCount += 1
					InterfList.append(interf)
					if InterfCount.has_key(interf):
						InterfCount[interf] += 1
					else:
						InterfCount[interf] = 1
					

	if interfCount > 0:

		for k in InterfCount.keys():
			InterfCount[k] /= float(interfCount)


		fig, ax = plt.subplots()

		ax.bar(InterfCount.keys(), InterfCount.values(), color=config['BAR_COLOR'], align='center')
		ax.set_ylabel("Probability of occurence")
		ax.set_xlabel("Number of APs in the same channel")

		#CDF Plotting
		cdf =  ax.twinx()
		cdf.yaxis.set_label_position('right')
		cdf.yaxis.set_ticks_position('right')
		cdf.set_ylabel("CDF")
		cdf.patch.set_visible(False)
		ecdf = ECDF(InterfList)
		cdf.step(ecdf.x, ecdf.y)

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
		fig.savefig(output_file)
