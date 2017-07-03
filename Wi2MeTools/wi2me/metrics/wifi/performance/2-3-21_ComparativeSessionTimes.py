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

METRIC_CODE = "2.3.21"
METRIC_NAME = "Comparative Session Times"
OUT_BASE_NAME = "ComparativeSessionTimes"
SVG_EXTEND = ".svg"
TXT_EXTEND = ".txt"

DEFAULT_CONFIG={}
TAB = "	"

LINESTYLES = ['-', '-.', ':', '--']

def plot(config):
	allTags = set([tag for source in config['data'] for tag in source.tags])

	sessionTimes = {} 
	sessionRssis = {} 
	interSessionTimes = {} #Times between sessions

        cmap = plt.get_cmap('jet_r')

	for Dict in [sessionTimes, interSessionTimes, sessionRssis]:
		for source in config['data']:
			if len(source.tags) > 0:
				for tag in source.tags:
					Dict[tag] = []
			else:
				Dict[source.name] = []

	for source in config['data']:
		srcSessionTimes = []
		srcSessionRssis = []
		srcInterSessionTimes = []

		LastBytes = -1
		duration = 0
		data = source.getTransferredData()
                lastRssi = -200

		if len(data) > 0:
			start = data[0].network.detections[0].timestamp
			end = max(u.network.detections[0].timestamp for u in data)
			iStart = start
		
			for dataTrans in data:
				now = dataTrans.network.detections[0].timestamp

                                rssi = dataTrans.network.detections[0].rssi
                                if rssi != lastRssi:
        				srcSessionRssis.append(rssi) 
                                        lastRssi = rssi

				if dataTrans.progress >= LastBytes:
					duration = float(now - start) / 1000
					iStart = now
				else:
					srcInterSessionTimes.append(float(now - iStart) / 1000)
					srcSessionTimes.append(duration)
					duration = 0
					start = now
				LastBytes = dataTrans.progress

			srcSessionTimes.append(duration) 

		if len(source.tags) > 0:
			for tag in source.tags:
				sessionTimes[tag] += srcSessionTimes
				interSessionTimes[tag] += srcInterSessionTimes
				sessionRssis[tag] += srcSessionRssis
		else:
			sessionTimes[source.name] += srcSessionTimes
			interSessionTimes[source.name] += srcInterSessionTimes
			sessionRssis[source.name] += srcSessionRssis


	#for xAxis, Dict, xRange in [("AP Availability", sessionTimes, [0, 150]), ("AP Switching Times", interSessionTimes, [0, 10])]:
	for xAxis, Dict, xRange in [("AP Availability (s)", sessionTimes, [0, 150]), ("AP Switching Times (s)", interSessionTimes, [0, 10]), ("RSSI (dBm)", sessionRssis, [-100, 0])]:
		xmin, xmax = xRange
		fig = plt.figure()
            
		styleInd = 0
		colorInd = 0

	        cdfTextOut = open(config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_" + xAxis + "_cdf_" + TXT_EXTEND, 'w')

		for kInd, k in enumerate(sorted(Dict.keys())):
			if len(Dict[k]) > 0:
                                #color = cmap(float(kInd)/len(allTags))

				ecdf = ECDF(Dict[k])
				plt.step(ecdf.x, ecdf.y, label=k, linestyle=LINESTYLES[kInd % len(LINESTYLES)])

                                cdfTextOut.write(k  + ' x   ' + str([ x for x in ecdf.x]) + '\n')
                                cdfTextOut.write(k  + ' y   ' + str([ y for y in ecdf.y]) + '\n')
                
                cdfTextOut.close()
                            
		plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=2,)

		plt.ylabel("CDF")
		plt.xlabel(xAxis)

		#fig.tight_layout()

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + xAxis + SVG_EXTEND
		fig.savefig(output_file, bbox_inches='tight')

		plt.close(fig)


	textOut = open(config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + TXT_EXTEND, 'w')
	for label, Dict in [("AP Availability", sessionTimes), ("AP Switching Times", interSessionTimes)]:
		textOut.write(label + "\n")
		for k in Dict:
			textOut.write(TAB)
			textOut.write(k)
			textOut.write('\n')
			if len(Dict[k]) > 0:
				for func in [np.mean, np.median, np.std]:
					textOut.write(TAB)
					textOut.write(TAB)
					textOut.write(func.__name__)
					textOut.write(TAB)
					textOut.write(str(func(Dict[k])))
					textOut.write('\n')
		textOut.write('\n')
	
	textOut.close()	
