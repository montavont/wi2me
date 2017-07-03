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
power_cable_distribution_cabinet = XapiQuery(power = u"cable_distribution_cabinet")
power_generator = XapiQuery(power = u"generator")
power_line = XapiQuery(power = u"line")
power_minor_line = XapiQuery(power = u"minor_line")
power_pole = XapiQuery(power = u"pole")
power_station = XapiQuery(power = u"station")
power_sub_station = XapiQuery(power = u"sub_station")
power_tower = XapiQuery(power = u"tower")
