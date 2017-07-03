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


TYPE_DISCONNECTED = 0
TYPE_CONNECTION_START = 1
TYPE_ASSOCIATING = 2
TYPE_ASSOCIATED = 3
TYPE_CONNECTED = 4
TYPE_DHCP = 5
TYPE_PASSED_PORTAL = 6
TYPE_TIMEOUT = 7
TYPE_RSSI_CHANGE = 8

class ConnectionEvent:

	def __init__(self, state, timestamp, network = None):
		self.state = state
		self.timestamp = timestamp
		self.network = network
