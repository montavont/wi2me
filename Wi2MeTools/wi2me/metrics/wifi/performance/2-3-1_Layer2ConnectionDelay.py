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

from wi2me.model.ConnectionEvent import TYPE_CONNECTION_START, TYPE_ASSOCIATED 

METRIC_CODE = "2.3.1"
METRIC_NAME = "Layer Two connection delay"
OUT_BASE_NAME = "Layer2ConnectionDelay.svg"

DEFAULT_CONFIG={}

SEARCHING_START = 1
SEARCHING_ASSOCIATED = 2

def plot(config):

	ConnectionTimes = []

	for source in config['data']:

		state = SEARCHING_START
		for event in source.getEvents( [TYPE_CONNECTION_START, TYPE_ASSOCIATED]):
			if state == SEARCHING_START:
				if event.state == TYPE_CONNECTION_START:
					startTime = event.timestamp
					state = SEARCHING_ASSOCIATED
			elif state == SEARCHING_ASSOCIATED:
				if event.state == TYPE_ASSOCIATED:
					ConnectionTimes.append(event.timestamp - startTime)
					state = SEARCHING_START
					
				elif event.state == TYPE_CONNECTION_START:
					startTime = event.timestamp
	

	if len(ConnectionTimes) == 0:
		return

	fig = plt.figure()

	ecdf = ECDF(ConnectionTimes)

	plt.step(ecdf.x, ecdf.y)
	plt.ylabel("CDF")
	plt.xlabel("Layer 2 connection times (ms)")

	fig.tight_layout()

	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
	fig.savefig(output_file)

	plt.close(fig)	
