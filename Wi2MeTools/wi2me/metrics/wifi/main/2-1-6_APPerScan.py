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
from scipy.stats import ks_2samp


METRIC_CODE = "2.1.6"
METRIC_NAME = "Wifi Scanresult Sizes"
OUT_BASE_NAME = "APsPerScan"
OUT_EXT = ".svg"


DEFAULT_CONFIG={'PLOT_MAX':True,
		'PLOT_MIN':True,
		'PLOT_MEAN':True,
		'PLOT_TOTAL':False,
		}

LINESTYLES = ['-', '-.', ':', '--']


def plot(config):

	ScanResultLengths = {}
	TotalBssids = {}
        cmap = plt.get_cmap('jet_r')


	allTags = sorted(set([tag for source in config['data'] for tag in source.tags]))
        if len(allTags) == 0:
            ScanResultLengths[''] = []
            TotalBssids[''] = []
            for source in config['data']:
                source.tags = ['']
            allTags = ['']
        else:
            for tag in allTags:
                ScanResultLengths[tag] = []
                TotalBssids[tag] = []



	for source in config['data']:
		for res in source.getScanResults():
                        for tag in source.tags:
        			TotalBssids[tag] += [ap.bssid for ap in res]
	        		ScanResultLengths[tag].append(len(res))

	if len(ScanResultLengths) > 0:

		fig = plt.figure()

                for tInd, tag in enumerate(allTags):
                        color = cmap(float(tInd)/len(allTags))

			ecdf = ECDF(ScanResultLengths[tag])

			#Ordered scan result size
			plt.step( ecdf.x, ecdf.y, label = tag, linestyle=LINESTYLES[tInd % len(LINESTYLES)])
    
                if len(allTags) > 1:
                        plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=2,)

		plt.ylabel("CDF")
		plt.xlabel("Scan Result Size")
			
		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_CDF" + OUT_EXT
		fig.savefig(output_file, bbox_inches='tight')
			
		plt.close(fig)

		fig = plt.figure()

                for tag in allTags:

		        meanResSize = reduce(lambda x, y: x + y, ScanResultLengths[tag]) / float(len(ScanResultLengths[tag]))
        		maxResSize = max(ScanResultLengths[tag])
	        	minResSize = min(ScanResultLengths[tag])


			plt.ylabel("Scan result size")
			plt.xlabel("Scan Attempts")

			plt.step( range(len(ScanResultLengths[tag])), sorted(ScanResultLengths[tag]), label = tag)

			if config['PLOT_MIN']:	
				plt.axhline(y=minResSize)
                                text = "minimal result size : " + str(minResSize)
                                if len(tag) > 0:
                                    text += "   (" + tag + ')'
				plt.text(0, minResSize + 1, text)

			if config['PLOT_MAX']:	
				plt.axhline(y=maxResSize)
                                text = "maximal result size : " + str(maxResSize)
                                if len(tag) > 0:
                                    text += "   (" + tag + ')'
				plt.text(0, maxResSize + 1, text)

			if config['PLOT_MEAN']:	
				plt.axhline(y=meanResSize)
                                text = "mean result size : " + str(meanResSize)
                                if len(tag) > 0:
                                    text += "   (" + tag + ')'
				plt.text(0, meanResSize + 1, text)
				
			if config['PLOT_TOTAL']:	
				totalAPNumber = len(set(TotalBssids))
				plt.axhline(y=totalAPNumber)
                                text = "Number of different BSS seen : " + str(totalAPNumber)
                                if len(tag) > 0:
                                    text += "   (" + tag + ')'
				plt.text(0, totalAPNumber - 2, text)

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_SIZE" + OUT_EXT
		fig.savefig(output_file)
			
		plt.close(fig)



                ks_outpfile = open(config['outdir'] + METRIC_CODE + "_"+ OUT_BASE_NAME + "KolmogorovSmirnov.txt", 'w')
                for t1 in allTags:
                    for t2 in allTags:
                        if t1 != t2 and 0 not in(len(ScanResultLengths[t1]),len(ScanResultLengths[t2])):
                            ks_outpfile.write(t1)
                            ks_outpfile.write(" ")
                            ks_outpfile.write(t2)
                            ks_outpfile.write(" ")
                            ks_outpfile.write(str(ks_2samp(ScanResultLengths[t1],ScanResultLengths[t2])))
                            ks_outpfile.write("\n")

                ks_outpfile.close()

