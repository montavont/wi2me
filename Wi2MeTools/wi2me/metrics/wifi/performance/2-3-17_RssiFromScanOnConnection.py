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
from wi2me.model.ConnectionEvent import TYPE_CONNECTION_START, TYPE_CONNECTED, TYPE_DISCONNECTED

METRIC_CODE = "2.3.17"
METRIC_NAME = "Rssi from scan results on Connection"
OUT_BASE_NAME = "RssiFromScanOnConnection.svg"


DEFAULT_CONFIG={"PLOT_ALL":True, "PLOT_SUCCESS":True, "PLOT_FAILURES": True, "ALL_COLOR":"blue", "SUCCESS_COLOR":"green", "FAILURE_COLOR":"red", 'LEGEND_BOX':(1, 0.25)}

SEARCHING_START = 1
SEARCHING_CONNECTED = 2

#Look avor a list of scan results, and take the level of the one just before the timestamp of given AP
def getLevelFromScan(targetTS, targetBssid, ScanResults):
	return max([det for ress in ScanResults for res in ress for det in res.detections if det.timestamp < targetTS and res.bssid == targetBssid], key = lambda x : x.timestamp).rssi



def plot(config):

	AllLevels = []
	SuccessfullLevels = []
	UnsuccessfullLevels = []
	legends = []
	legendNames = []

	for source in config['data']:

		ScanResults = source.getScanResults()
		Events = source.getEvents([TYPE_CONNECTION_START, TYPE_CONNECTED, TYPE_DISCONNECTED])

		if len(Events) > 0:

			state = SEARCHING_START

			SuccessfullConnectionEvents = []
			UnsuccessfullConnectionEvents = []

			lastTS = Events[0].network.detections[0].timestamp
			lastBssid = Events[0].network.bssid

			for event in Events:
				if state == SEARCHING_START:
					if event.state == TYPE_CONNECTION_START:
						lastTS = event.network.detections[0].timestamp
						lastBssid = event.network.bssid
						state = SEARCHING_CONNECTED
				elif state == SEARCHING_CONNECTED:
					if event.state == TYPE_CONNECTED:
						SuccessfullLevels.append(getLevelFromScan(lastTS, lastBssid, ScanResults))
						state = SEARCHING_START
					elif event.state == TYPE_CONNECTION_START:
						UnsuccessfullLevels.append(getLevelFromScan(lastTS, lastBssid, ScanResults))
						lastTS = event.network.detections[0].timestamp
						lastBssid = event.network.bssid

	AllLevels = SuccessfullLevels + UnsuccessfullLevels

	if len(AllLevels) == 0:
		return

	fig = plt.figure()

	if config['PLOT_ALL']:
		edcf_all = ECDF(AllLevels)
		data, = plt.step(edcf_all.x, edcf_all.y, color=config['ALL_COLOR'])
		legends.append(data)
		legendNames.append("All connection Attempts")

	if config['PLOT_SUCCESS']:
		if len(SuccessfullLevels) == 0:
			print "no successes in trace"
		else:
			edcf_success = ECDF(SuccessfullLevels)
			data, = plt.step(edcf_success.x, edcf_success.y, color=config['SUCCESS_COLOR'])
			legends.append(data)
			legendNames.append("Successfull connection Attempts")
	
	if config['PLOT_FAILURES']:
		if len(UnsuccessfullLevels) == 0:
			print "no failures in trace"
		else:
			edcf_failures = ECDF(UnsuccessfullLevels)
			data, = plt.step(edcf_failures.x, edcf_failures.y, color=config['FAILURE_COLOR'])
			legends.append(data)
			legendNames.append("Failed connection Attempts")



	plt.legend(legends, legendNames, bbox_to_anchor=config['LEGEND_BOX'])

	plt.ylabel("CDF")
	plt.xlabel("Signal level on connection timeout")




	fig.tight_layout()

	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
	fig.savefig(output_file)

	plt.close(fig)
