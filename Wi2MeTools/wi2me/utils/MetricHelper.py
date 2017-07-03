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



import os
import imp
import json 

METRIC_DIR = "wi2me" + os.path.sep + "metrics"
FORBIDDEN_CONF_KEYS = ["db", "outdir"]

def listMetrics(verbose, Filter=None):
	retval = []

	printval = []

	for dirname, dirnames, filenames in os.walk(METRIC_DIR):
    		if '.svn' in dirnames:
        		dirnames.remove('.svn')

		for filename in filenames:
			if not filename.endswith(".py"):
				continue 

        		metric_path = os.path.join(dirname, filename)
        		metric_name = os.path.splitext(filename)[0]

			try:
				metric = imp.load_source(metric_name, metric_path)

				if hasattr(metric, "METRIC_CODE") and hasattr(metric, "METRIC_NAME"):
					if verbose > 0:
						printval.append(metric.METRIC_CODE + " " + metric.METRIC_NAME)

					if Filter:
						#prefix case
						if Filter[len(Filter) - 1 ] == '.':
							if metric.METRIC_CODE.startswith(Filter):
								retval.append(metric)
						#single metric targeting 
						elif Filter == metric.METRIC_CODE:
							retval.append(metric)
					else:
						retval.append(metric)
			except ImportError, e:
				print "failed to import metric " + metric_name
				print e
				continue
	
	if verbose > 0:
		for s in sorted(printval):
			print s		


	if Filter and len(retval) < 1:
		print "Did not find any metric for filter " + Filter + ". \nUse -l option to list available metrics."

	return retval

def getConfig(metric, confFile):
	retval = metric.DEFAULT_CONFIG 
	if confFile != None and os.path.exists(confFile):
		confFile = open(confFile, 'r')
		userConf = json.loads(confFile.read())

		if userConf.has_key(metric.METRIC_CODE):
			retval = userConf[metric.METRIC_CODE]

		
	return retval
