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

from __future__ import print_function

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.distributions.empirical_distribution import ECDF


METRIC_CODE = "2.1.11"
METRIC_NAME = "Wifi Scans per AP"
OUT_BASE_NAME = "ScansPerAP"
OUT_EXT = ".svg"


DEFAULT_CONFIG={
		}



def plot(config):


	APs = {}

	for i, source in enumerate(config['data']):
		print(str(i) + "	" + str(source.path))
		for res in source.getScanResults():
			for ap in res:
				apId = ap.bssid[3:14]
				if apId not in APs:
					APs[apId] = 0
				APs[apId] += 1

	if len(APs) > 0:

		print(len(APs))

		fig = plt.figure()

		ecdf = ECDF(APs.values())

		#Ordered scan result size
		plt.step(ecdf.x, ecdf.y)

		plt.ylabel("CDF")
		plt.xlabel("Number of AP detections (cropped)")

		plt.tight_layout()
		plt.xlim(xmax=1000)

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_CDF" + OUT_EXT
		fig.savefig(output_file)
		plt.close()


		fig = plt.figure()

		ax = fig.add_subplot(1,1,1)
		ecdf = ECDF(APs.values())

		#Ordered scan result size
		ax.step(ecdf.x, ecdf.y)

		plt.ylabel("CDF")
		plt.xlabel("Number of AP detections ")
		ax.set_xscale('log')

		plt.xlim(xmax=1200)
		plt.tight_layout()
		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_LOGCDF.pdf"
		fig.savefig(output_file)
		plt.close()

		Y = sorted(APs.values())
		X = range(len(APs))


		fig = plt.figure()

		ax = fig.add_subplot(1,1,1)

		line, = ax.plot(X, Y, color='blue', lw=2)

		ax.set_yscale('log')

		plt.ylabel("Observations (Log)")
		plt.xlabel("APs")

		plt.tight_layout()

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_LOG" + OUT_EXT
		fig.savefig(output_file)
		plt.close()

                # Save the eCDF to a text file, useful for pgfplots :)
                n_aps = len(APs)

                # 1000 dots should be enough to have a good detail for papers
                # samples = min(n_aps, 1000)
                # xx = np.linspace(0, n_aps, num=samples)
                xx = np.unique(ecdf.x)

                output_file = config['outdir'] + METRIC_CODE + "_" + \
                        OUT_BASE_NAME + ".dat"
                with open(output_file, 'w') as f:
                    print('x', 'ecdf(x)', file=f)
                    for x, y in zip(xx, ecdf(xx)):
                        print(x, y, file=f)
