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

from statsmodels.distributions.empirical_distribution import ECDF
from scipy.stats import ks_2samp


METRIC_CODE = "2.1.4"
METRIC_NAME = "Access point power distribution"
OUT_BASE_NAME = "APPowerDistribution"

DEFAULT_CONFIG = {"COMMUNITY_NETWORKS":["FreeWifi", "SFR WiFi FON", "SFR WiFi Public"], "minLevel":-101}
LINESTYLES = ['-', '-.', ':', '--']

def plot(config):
	plotFig(config, [])
	plotFig(config, config["COMMUNITY_NETWORKS"])
	for cn in config["COMMUNITY_NETWORKS"]:
		plotFig(config, [cn])


def makeTitle(Filter):
	retval = ""
	if len(Filter) > 0:
		for cn in Filter:
			retval += cn + " "
	return retval

def plotFig(config, CNFilter):

        Powers = {}
	MaxPowers = {}
        AveragePowers = {}

	allTags = sorted(set([tag for source in config['data'] for tag in source.tags]))
        if len(allTags) == 0:
            Powers[''] = {}
            for source in config['data']:
                source.tags = ['']
            allTags = ['']
        else:
            for tag in allTags:
                Powers[tag] = {}


	for source in config['data']:
		for nw in source.getApList():
			for det in nw.detections:
				if det.rssi > config['minLevel'] and (len(CNFilter) == 0 or nw.ssid in CNFilter):
                                        for tag in source.tags:
        					if Powers[tag].has_key(nw.bssid):
	        					Powers[tag][nw.bssid].append(int(det.rssi))
		        			else:
			        			Powers[tag][nw.bssid]=[int(det.rssi)]

	if max(len(u) for u in Powers.values()) == 0:
		print "No ap found for filter : " + str(CNFilter)
	else:
                for tag in Powers:
                    MaxPowers[tag] = {}
                    AveragePowers[tag] = {}
                    for bssid in Powers[tag]:
                        MaxPowers[tag][bssid] = max(Powers[tag][bssid])
                        AveragePowers[tag][bssid] = np.average(Powers[tag][bssid])

                for data, xLabel in [(MaxPowers, 'Maximal Signal Level (dBm)'),(AveragePowers, 'Average Signal Level (dBm)')]:
                        minx = 0
                        maxx = -100

			fig, ax = plt.subplots()
			cdf =  ax.twinx()

                        curves = {}

                        for tInd, tag in enumerate(allTags):
		            if len(data[tag]) == 0:
			        print "No ap found for filter : " + str(CNFilter) + " and tag " + tag
                            else:
                                ecdf = ECDF(data[tag].values())

    			        #CDF Plotting
                                cdf.yaxis.set_label_position('right')
			        cdf.yaxis.set_ticks_position('right')
			        cdf.set_ylabel("CDF")
				cdf.set_ylim(0, 1)
				cdf.patch.set_visible(False)
				cdf.step(ecdf.x[::-1], ecdf.y, label = tag, linestyle=LINESTYLES[tInd % len(LINESTYLES)])

                                minx = min(minx, min([x for x in ecdf.x if x not in [-np.inf, np.inf]]))
                                maxx = max(maxx, max([x for x in ecdf.x if x not in [-np.inf, np.inf]]))

                                curves[tag] = ecdf

                            if len(allTags) == 1:

                                numberAPs = len(data[tag])
                                PowersOccurence = dict((k, 0) for k in range(-100, 1))

		                for ap in data[tag].keys():
			            PowersOccurence[int(data[tag][ap])] += 1

				    #NormalizedPowersOccurence = [0] * 101
				    #for i in range(0, len(PowersOccurence.values())):
				    #    NormalizedPowersOccurence[i] = float(PowersOccurence[-i]) / numberAPs

			        ax.bar(PowersOccurence.keys(), PowersOccurence.values(), align='center', label = tag, color = 'gray')
				ax.set_ylabel("Occurences")

			    ax.set_xlabel(xLabel)

                        if len(allTags) > 1:
                            plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=2,)

                        plt.xlim(maxx, minx)

                        output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + xLabel + makeTitle(CNFilter) + ".svg"
		        fig.savefig(output_file, bbox_inches='tight')

                        plt.close()

                        ks_outpfile = open(config['outdir'] + METRIC_CODE + "_"+ OUT_BASE_NAME + "KolmogorovSmirnov" + xLabel[:-6] + makeTitle(CNFilter) + ".txt", 'w')
                        for t1 in allTags:
                            for t2 in allTags:
                                if t1 != t2 and 0 not in(len(data[t1]),len(data[t2])):
                                    ks_outpfile.write(t1)
                                    ks_outpfile.write(" ")
                                    ks_outpfile.write(t2)
                                    ks_outpfile.write(" ")
                                    ks_outpfile.write(str(ks_2samp(data[t1].values(),data[t2].values())))
                                    ks_outpfile.write("\n")

                        ks_outpfile.close()

