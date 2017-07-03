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

from wi2me.model.ConnectionEvent import TYPE_CONNECTION_START, TYPE_CONNECTED, TYPE_TIMEOUT

METRIC_CODE = "2.3.16"
METRIC_NAME = "Rssi on Connection Timeout"
OUT_BASE_NAME = "RssiOnConnectionTimeout.svg"


DEFAULT_CONFIG={}

SEARCHING_START = 1
SEARCHING_TIMEOUT = 2

def plot(config):

	ConnectionLevels = []

	for source in config['data']:
		Events = source.getEvents([TYPE_TIMEOUT])
		ConnectionLevels += [event.network.detections[0].rssi for event in Events]


	if len(ConnectionLevels) == 0:
		return

	fig = plt.figure()

	ecdf = ECDF(ConnectionLevels)

	plt.step(ecdf.x, ecdf.y)
	plt.ylabel("CDF")
	plt.xlabel("Signal level on connection timeout")

	fig.tight_layout()

	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
	fig.savefig(output_file)
	
	plt.close(fig)
