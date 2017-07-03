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

import dmesgSource
import sqliteSource
import ns3Source
import csvSource
import ApiTestingEmptySource

ORIGIN_ANDROID = "android"
ORIGIN_DMESG = "dmesg"
ORIGIN_NS3 = "ns3"
ORIGIN_CSV = "csv"
TAG_SEP = '#'


def getSource(path):
	source =  None
	sourceLocation = None
	tags = []
	if os.path.exists(path):
		sourceLocation = path
	elif TAG_SEP in path:
		splitted = path.split('#')
		
		if os.path.exists(splitted[0]):
			sourceLocation = splitted[0]
			tags = splitted[1:]

	if sourceLocation is not None:
		if sourceLocation.endswith('.db'):
			source = sqliteSource.sqliteSource(sourceLocation)
		elif sourceLocation.endswith('.ns3_log'):
			source = ns3Source.ns3Source(sourceLocation)
		elif sourceLocation.endswith('ApiTestingEmptySource'):
                        source = ApiTestingEmptySource.ApiTestingEmptySource(sourceLocation)
		elif sourceLocation.endswith('csv'):
                        source = csvSource.csvSource(sourceLocation)
		elif os.path.isdir(sourceLocation):
			source = dmesgSource.dmesgSource(sourceLocation)
                if source is not None:
         		source.tags = tags
	else:
		print "Ignoring incorrect path " + path

	return source


def shortName(path):
	retval = os.path.basename(path).rstrip('\n')
	spltPath = path.split(os.sep)
	while len(retval) == 0:
		if len(spltPath) > 0 :
			retval = spltPath.pop()
		else:
			break
	return retval
