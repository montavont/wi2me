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

from wi2me.model.ConnectionEvent import TYPE_DISCONNECTED, TYPE_ASSOCIATED 

METRIC_CODE = "2.3.23"
METRIC_NAME = "Comparative Traffic"
OUT_BASE_NAME = "ComparativeTraffic"
SVG_EXTEND = ".svg"
TXT_EXTEND = ".txt"

DEFAULT_CONFIG={
    "TRUNCATE":False #Only pick the shortest common duration in all the traces
    }
TAB = "	"

def plot(config):
	allTags = set([tag for source in config['data'] for tag in source.tags])

        Traffic = {}
        Duration = {}
        L2AssociationTimes = {}
        Handovers = {}
        AvgThroughputs = {}

	for Dict in [Traffic, Duration, L2AssociationTimes, Handovers]:
		for source in config['data']:
			if len(source.tags) > 0:
				for tag in source.tags:
					Dict[tag] = 0
			else:
				Dict[source.name] = 0

        if config['TRUNCATE']:
            minDuration = min([source.getEndTime() - source.getStartTime() for source in config['data']])
        else:
            minDuration = -1

	for source in config['data']:
                l2Time = 0
                if config['TRUNCATE']:
                    duration = minDuration / 1000.0 
                else:
                    duration = (source.getEndTime() - source.getStartTime()) / 1000.0

                traffic = source.getTotalTransferredBytes(maxts = minDuration)#0
                handovers = 0

                lastSStart = -1
                lastEvent = TYPE_DISCONNECTED

                for event in source.getEvents(eventTypes = [TYPE_ASSOCIATED, TYPE_DISCONNECTED]):
                    if event.state == TYPE_ASSOCIATED:
                        handovers += 1
                        lastSStart = event.timestamp
                    elif event.state == TYPE_DISCONNECTED and lastEvent == TYPE_ASSOCIATED:
                        l2Time += (event.timestamp - lastSStart) / 1000.0
                    lastEvent = event.state

                if source.getEndTime() > lastSStart and lastEvent == TYPE_ASSOCIATED:
                        l2Time += (source.getEndTime() - lastSStart) / 1000.0

                lastTraf = 0

                buckets = 10 
                tsum = 0

                lastTs = 0
                lastBytes = 0

                step = duration * 1000 / buckets
                bucketSpeeds = {i:[] for i in range(buckets + 1)}
                bucketInterv = {i:[] for i in range(buckets + 1)}
                for dataPoint in source.getTransferredData():
                    ts = dataPoint.network.detections[0].timestamp
                    buk = int(ts// step)
                    bucketSpeeds[buk].append(dataPoint.progress - lastBytes)
                    bucketInterv[buk].append(ts - lastTs)

                    lastBytes = dataPoint.progress
                    lastTs = ts


		if len(source.tags) > 0:
			for tag in source.tags:
				Duration[tag] += duration
				Traffic[tag] += traffic
				L2AssociationTimes[tag] += l2Time
				Handovers[tag] += handovers
		else:
			Duration[source.name] += duration
			Traffic[source.name] += traffic
			L2AssociationTimes[source.name] += l2Time
			Handovers[source.name] += handovers

        print "|_.name|_.Average Throughput (kB/s) |_.Duration (s)|_.Average Throughput while associated (kBs)|_.Association time(s)|Association Ratio|_.Handovers|_.Handovers/s|"
        for k in sorted([u for u in Traffic if Duration[u] > 0] ,key = lambda x : Traffic[x] / float(Duration[x])):
            line = "|" + k + "|"
            if Duration[k] > 0:
                line += str(float(Traffic[k]) / Duration[k] // 1000) + "|" + str(Duration[k])[:8]
                AvgThroughputs[k] = float(Traffic[k]) / Duration[k]
            else:
                line += "N/A|" + str(Duration[k])

            if L2AssociationTimes[k] > 0:
                line += "|" + str(float(Traffic[k])/ L2AssociationTimes[k] // 1000) + "|" + str(L2AssociationTimes[k])[:8] + "|" + str(float(L2AssociationTimes[k])  / Duration[k])[:8] + '|' + str(Handovers[k]) + " |"  + str(Handovers[k]/float(Duration[k]))[:8] + "|"
            else:
                line += "|N/A|" + str(L2AssociationTimes[k]) + "|N/A|" + str(Handovers[k]) + " |N/A|"
            print line


        if len(AvgThroughputs) > 0:

                width=0.25
                Average_Xs = [u - width for  u in range(1, len(AvgThroughputs) + 1)]
                Average_bars = [AvgThroughputs[k] for k in sorted(AvgThroughputs)]

                AssociationAverage_Xs = [u for u in range(1, len(AvgThroughputs) + 1)]
                AssociationAverage_bars = [float(Traffic[k]) / L2AssociationTimes[k] for k in sorted(AvgThroughputs) if L2AssociationTimes[k] > 0]

                AssocRatio_Xs = [u + width for  u in range(1, len(AvgThroughputs) + 1)]
                AssociationRatio_bars = [float(L2AssociationTimes[k]) / Duration[k] for k in sorted(AvgThroughputs)]

                fig, ax1 = plt.subplots()
                ax2 = ax1.twinx()


		ax1.set_ylabel("Average Throughput (Bytes/s)")
		ax2.set_ylabel("Association Ratio")
		plt.xlabel("Sources")

                if len(Average_bars) > 0:
                    legT = ax1.bar(Average_Xs, Average_bars, width,  color = "grey", align = "center", label = "Average Throughput")
                if len(AssociationRatio_bars) > 0:
                    legAR = ax2.bar(AssocRatio_Xs, AssociationRatio_bars, width, color="black", align = "center", label ="Association Ratio")
                if len(AssociationAverage_bars) > 0:
                    legAA = ax1.bar(AssociationAverage_Xs, AssociationAverage_bars, width, color="darkgrey", align = "center", label = "Instantaneous Throughput")
                plt.xticks(range(1, len(AvgThroughputs) + 1), sorted(AvgThroughputs.keys()), rotation = 90)


                lines, labels = ax1.get_legend_handles_labels()
                lines2, labels2 = ax2.get_legend_handles_labels()
                ax2.legend(lines + lines2, labels + labels2, loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=3,)


		output_file = config['outdir'] + METRIC_CODE + "_" + OUT_BASE_NAME + SVG_EXTEND
		fig.savefig(output_file, bbox_inches='tight')

		plt.close(fig)

