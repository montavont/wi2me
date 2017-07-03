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

from wi2me.model.ConnectionEvent import  TYPE_DISCONNECTED,  TYPE_RSSI_CHANGE



METRIC_CODE = "2.3.22"
METRIC_NAME = "Rssi On Data Transfer Comparison"
OUT_BASE_NAME = "CompativeRssiOnData"
SVG_EXTEND = ".svg"
TXT_EXTEND = ".txt"

DEFAULT_CONFIG={
        'BUCKET_COUNT':20
    }

TAB = "	"

def plot(config):

	allTags = set([tag for source in config['data'] for tag in source.tags])
	
	Rssis = {} 
        Sessions = {}
        SessionRssiTrajs = {}
        cmap = plt.get_cmap('jet_r')

	for Dict in [Rssis, Sessions, SessionRssiTrajs]:
		for source in config['data']:
			if len(source.tags) > 0:
				for tag in source.tags:
					Dict[tag] = []
			else:
				Dict[source.name] = []


        #Get Rssis overall
	for source in config['data']:
		srcRssis = [data.network.detections[0].rssi for data in source.getTransferredData() if data.network.detections[0].rssi != -200]
		
		if len(source.tags) > 0:
			for tag in source.tags:
				Rssis[tag] += srcRssis
		else:
			Rssis[source.name] += srcRssis


        #Get rssi sequence in each section
        for source in config['data']:
                Seq = []
                sourceSeqs = []
                normalizedRssis = []
                for event in source.getEvents(eventTypes = [TYPE_RSSI_CHANGE, TYPE_DISCONNECTED]):
                    if event.state == TYPE_RSSI_CHANGE:
                        Seq.append(event)
                    elif event.state == TYPE_DISCONNECTED:
                        if len(Seq) > 0:
                            sourceSeqs.append(Seq)
                            Seq = []


                #All rssi change event gathered, now normalize on the session duration
                for Seq in sourceSeqs:
                    duration = Seq[-1].timestamp - Seq[0].timestamp
                    start = Seq[0].timestamp

                    if duration > 0:
                        for evt in Seq:
                            normTs = float(evt.timestamp - start) / duration
                            normalizedRssis.append((normTs, evt.network.detections[0].rssi))
                    
        	if len(source.tags) > 0:
	        	for tag in source.tags:
		        	Sessions[tag] += normalizedRssis
        	else:
	        	Sessions[source.name] += normalizedRssis

        
        #Bucketize the rssis depending on their normalized position in the session, then average each bucket
        for k in Sessions:
            bucketized = []
            data = sorted(Sessions[k], key = lambda x : x[0])
            if len(data) < config['BUCKET_COUNT']:
                print "Not enough data points to buil " + str(config['BUCKET_COUNT']) + " with " + k
            else:
                bucketSize = len(data) / config['BUCKET_COUNT']
                for i in range(config['BUCKET_COUNT']):
                    bucketized.append(Sessions[k][i * bucketSize: (i + 1) * bucketSize])

            SessionRssiTrajs[k] = [np.average(u) for u in bucketized]

                        
	for xAxis, Dict in [("RSSI (dBm)", Rssis)]:
		fig = plt.figure()

		for kInd, k in enumerate(Dict):
			if len(Dict[k]) > 0:
                                color = cmap(float(kInd)/len(Dict))
    
				ecdf = ECDF(Dict[k])
				plt.step(ecdf.x, ecdf.y, label=k, color=color)


        	plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=3,)

		plt.ylabel("CDF")
		plt.xlabel(xAxis)
		plt.xlim(xmin=-100)
			
		fig.tight_layout()

		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + xAxis + SVG_EXTEND
        	fig.savefig(output_file, bbox_inches='tight')

		plt.close(fig)


	textOut = open(config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + TXT_EXTEND, 'w')
	for label, Dict in [("RSSI", Rssis)]:
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



	fig = plt.figure()

	for kInd, k in enumerate(Dict):
		if len(Dict[k]) > 0:
                        color = cmap(float(kInd)/len(Dict))
			plt.plot([x * 100.0 / config['BUCKET_COUNT']  for x in range(config['BUCKET_COUNT'])], SessionRssiTrajs[k], label=k, color=color)


      	plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=3,)

	plt.ylabel("Rssi (dBm)")
	plt.xlabel('Relative session time (%)')
			
	fig.tight_layout()

	output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + "_RssiTrajectories" + SVG_EXTEND
       	fig.savefig(output_file, bbox_inches='tight')

	plt.close(fig)

        

