#!/usr/bin/env python2
#
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
import os
from optparse import OptionParser
from wi2me.utils import MetricHelper
from wi2me.source import SourceHelper
from settings import * 
from datetime import datetime
import json 

def createDirs(dirs):
	for dir in dirs:
		if not os.path.exists(dir):
			os.makedirs(dir)

def main():
	usage = "usage: %prog [options] DATABASE1.db [DATABASE2.db DATABASE3.db ...]\n\
       %prog -l [PATTERN]"
	parser = OptionParser(usage)
	parser.add_option("-l", "--list-metrics", action="store_true", dest="list", help="list available metrics")
	parser.add_option("-t", "--target", dest="target", metavar="TARGET", help="select metrics to use", default="")
	parser.add_option("--defconfig", action="store_true", dest="defconfig", help="print default config to stdout")
	parser.add_option("-c", "--config", metavar="CONFIGFILE", dest="configFile", help="path to config file")

	(options, args) = parser.parse_args()

	#list available metrics
	if options.list:
		MetricHelper.listMetrics(1)
		sys.exit(0)
	
	#print default config to stdout to generate config file to use with -c
	if options.defconfig:
		defconf = {}
		for met in MetricHelper.listMetrics(0, options.target):
			defconf[met.METRIC_CODE] = met.DEFAULT_CONFIG
		print json.dumps(defconf, sort_keys=True, indent = 4)
		sys.exit(0)
	
	sourcePaths = args
	output_directory = OUT_DIR + datetime.now().strftime("%Y%m%d_%H%M%S") + "_"
	if len (sourcePaths) == 1 :
		output_directory += os.path.basename(SourceHelper.shortName(sourcePaths[0]))
	else:
		output_directory += str(len(sourcePaths)) + "Sources"

	output_directory += os.path.sep

	#Create directories
	createDirs([OUT_DIR, TEMP_DIR, output_directory])

	#Create database controllers
	Sources = [SourceHelper.getSource(pth) for pth in sourcePaths]
	base_config = {'data':Sources, 'outdir':output_directory}


	#Process Metrics
	for met in MetricHelper.listMetrics(0, options.target):
		print met.METRIC_CODE + " : " + met.METRIC_NAME

		config = MetricHelper.getConfig(met, options.configFile)

		for k in base_config:
			config[k] = base_config[k]

		#Skip some metrics who are generally not relevant
		#e.g. requiring the phone to stay still
		if config.has_key('SINGLE') and not met.METRIC_CODE in options.target:
			print "skipping single running metric " + met.METRIC_CODE
			continue

		met.plot(config)
			

if __name__ == '__main__':
	main()
