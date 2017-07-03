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

METRIC_CODE = "2.1.1"
METRIC_NAME = "Access point channel distribution"
OUT_BASE_NAME = "APChannelDistro.svg"

DEFAULT_CONFIG = {
	'CHANNELS':range(1,14)
}

def plot(config):
	
	channelCount = {u:[] for u in config['CHANNELS']}

	for source in config["data"]:
		results =  source.getScanResults()
		for res in results:
			for nw in res:
				if nw.channel in channelCount and nw.bssid not in channelCount[nw.channel]:
					channelCount[nw.channel].append(nw.bssid)
			


	totalAps = np.sum([len(u) for u in channelCount.values()])

        if totalAps > 0:
            fig = plt.figure()

	    plt.bar(channelCount.keys(), [float(len(u))/totalAps for u in channelCount.values()], align='center')

            plt.xlabel("Channel Number")
	    plt.ylabel("AP Ratio")
	    plt.xticks(config['CHANNELS'])
	
	    output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME
	    fig.savefig(output_file)

	    plt.close(fig)
	
	    textOut = open(config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + ".txt", 'w')

	    for k in channelCount.keys():
		textOut.write(str(k))
		textOut.write('	')
		textOut.write(str(float(len(channelCount[k]))/totalAps))
		textOut.write('	')
		textOut.write(str(len(channelCount[k])))
		textOut.write('\n')

	    textOut.close()	
