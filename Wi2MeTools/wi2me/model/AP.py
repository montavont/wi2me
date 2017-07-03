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


#Object representing a physical AP, that can broadcast different networks
class AP:
	counter = 0
	def __init__(self, networks = []):
		self.networks = networks
		self.id = AP.counter
		AP.counter += 1

#A netwotrk broadcasted by an AP
class Network:
	def __init__(self, bssid = "00:00:00:00:00:00", channel = 0, ssid = "", capabilities = "", detections = []):
		self.bssid = bssid.lower()
		self.channel = channel
		self.ssid = ssid
		self.attrs = {}
		self.capabilities = capabilities
		self.detections = detections
		self.ap = None

	def duplicate(self, withDetections = False):
		dets = []
		if withDetections:
			dets = self.detections

		return Network(self.bssid, self.channel, self.ssid, self.capabilities, dets)


#An observation of one of an AP'networks, located in time and space
class Detection:
	def __init__(self, rssi = -200, uptime = 0, timestamp = 0, source = "", GPS = (0, 0), scanInd = 0):
		self.source = source
		self.uptime = uptime
		self.GPS = GPS
		self.timestamp = timestamp
		self.rssi = rssi
		self.nw = None
		self.scanInd = scanInd
		self.sourceInd = -1
