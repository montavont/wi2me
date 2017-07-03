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
METRIC_NAME = "Total Length (m)"
METRIC_CODE = "1.9"


DEFAULT_CONFIG = {"CSV_SEQ":"	"}

GPSPoints = []

def plot(config):
	totalLen = 0
	
	finalFile = open(config['outdir'] + METRIC_CODE + "_TotalLength.txt", 'w')


	for source in config['data']:
		srcLen = source.getTravelledDistance()
		finalFile.write(source.name + config['CSV_SEQ'] + str(srcLen) + "\n")
		totalLen += srcLen	
		
	finalFile.write("Total" + config['CSV_SEQ'] + str(totalLen) + "\n")
	finalFile.close()
