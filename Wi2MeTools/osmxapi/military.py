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
military_airfield = XapiQuery(military= u"airfield")
military_bunker = XapiQuery(military= u"bunker")
military_barracks = XapiQuery(military= u"barracks")
military_danger_area = XapiQuery(military= u"danger_area")
military_naval_base = XapiQuery(military= u"naval_base")
military_range = XapiQuery(military= u"range")
military_checkpoint = XapiQuery(military= u"checkpoint")
