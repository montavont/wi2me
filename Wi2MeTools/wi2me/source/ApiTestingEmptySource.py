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


# This source modules returns empty values and is only used for test purpose

import sqlite3
from wi2me.model.AP import AP, Network, Detection
from wi2me.model.ConnectionEvent import *
from wi2me.model.TransferredData import *
import SourceHelper
import inspect

class ApiTestingEmptySource:
	def __init__(self, path):
		self.path = path
		self.origin = "TEST"
		self.name = SourceHelper.shortName(path)
		self.tags = []

	############################### Time related functions

	#timestamp for the beginning of the mesurements
	def getStartTime(self):
		return 0

	#timestamp for the end of the mesurements
	def getEndTime(self):
		return 0


	#If the database contains multiple occurence where the app was started and stopped
	def getSessions(self):
		retval = []
		return retval



	############################### Geolocalisation related functions

	#minimal and maximal latitude ant longitude in order to delimitate the place the mesurements took place in
	#The expected order is [lat_min, lat_max, long_min, long_max]
	def getExtremeCoordinates(self):
		retval = [0, 0, 0, 0]
		return retval 

	#Get the total travelled distance in meters
	def getTravelledDistance(self):
		retval = 0
		return retval

	#Get the GPS coordinates of points contained in a certain timerange (or all of them)
	def getPoints(self, timeRange=None):
		Points = [] 
		return Points

	############################### Access point discovery related functions

	# get a list of list of scan results (one list for each attempt)
	# This function does not assume grouping by physical APs and will return a list of Networks
	def getScanResults(self, gpsRange=None):
		retval = []
		return retval


	#Retrieve list of all APs seen in the trace
	# This Ill-named function does not group by physical APs and will return a list of Networks with detections corresponding to their bssid
	def getApList(self):
		retval = {}
		return retval.values()


	#Return the number of scan attempts
	def countScans(self):
		retval = 0
		return retval


	############################### Device Event related functions


	#Get device connectivity events
	def getEvents(self, eventTypes):
		return []

	#Get device community network events
	def getCNEvents(self, eventTypes):
		return []


	############################### Transmitted Data related functions

	#Get a list of the data transfer events, with the associated AP
	def getTransferredData(self):
		retval = []
		return retval
