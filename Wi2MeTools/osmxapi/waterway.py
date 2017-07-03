# -*- coding: utf-8 -*-
#
#  This file is part of the osmxapi Python module.
#
#  osmxapi is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  osmxapi is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with osmxapi.  If not, see <http://www.gnu.org/licenses/>.
#
#  Copyright: © 2012 Morten Kjeldgaard <mok@bioxray.dk>
#  License: GPL-3+
#
from xapiquery import XapiQuery
waterway_boatyard = XapiQuery(waterway = u"boatyard")
waterway_canal = XapiQuery(waterway = u"canal")
waterway_ditch = XapiQuery(waterway = u"ditch")
waterway_dock = XapiQuery(waterway = u"dock")
waterway_drain = XapiQuery(waterway = u"drain")
waterway_river = XapiQuery(waterway = u"river")
waterway_riverbank = XapiQuery(waterway = u"riverbank")
waterway_stream = XapiQuery(waterway = u"stream")
waterway_dam = XapiQuery(waterway = u"dam")
waterway_lock_gate = XapiQuery(waterway = u"lock_gate")
waterway_turning_point = XapiQuery(waterway = u"turning_point")
waterway_weir = XapiQuery(waterway = u"weir")
