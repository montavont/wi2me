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
import datetime 

METRIC_NAME = "Total Experimentation Time (s)"
METRIC_CODE = "1.8"

DEFAULT_CONFIG = {"CSV_SEQ":"	"}

def getHMSMS(ts):
	h, rest = divmod(ts, 3600000)
	m, rest = divmod(rest, 60000)
	s, ms = divmod(rest, 1000)
	return (h, m, s, ms)

def plot(config):
	Sources = config['data']
	totalLen = 0

	finalFile = open(config['outdir'] + METRIC_CODE + "_TotalDuration.txt", 'w')

	for source in Sources:
		srcLen = 0
                start = stop = 0
		hreadable = "%i:%i:%i.%i" % getHMSMS(0)
		for i, (start, stop) in  enumerate(source.getSessions()):
			sessLen = stop - start
			hreadable = "%i:%i:%i.%i" % getHMSMS(sessLen)

			finalFile.write(source.name + config['CSV_SEQ'] + "Session_" + str(i) + config['CSV_SEQ']  + str(start) + config['CSV_SEQ'] + str(stop) + config['CSV_SEQ'] + str(sessLen) + config['CSV_SEQ'] + hreadable + "\n")
			srcLen += sessLen

		finalFile.write(source.name + config['CSV_SEQ'] + "Total"  + config['CSV_SEQ']  + str(start) + config['CSV_SEQ'] + str(stop) + config['CSV_SEQ'] + str(srcLen) + config['CSV_SEQ'] + hreadable + "\n")
		totalLen += srcLen

	hreadable = "%i:%i:%i.%i" % getHMSMS(totalLen)
	finalFile.write("Total" + config['CSV_SEQ'] + str(totalLen) + config['CSV_SEQ'] + hreadable + "\n")
	finalFile.close()
