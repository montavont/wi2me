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
emergency_ambulance_station = XapiQuery(emergency= u"ambulance_station")
emergency_fire_extinguisher = XapiQuery(emergency= u"fire_extinguisher")
emergency_fire_flapper = XapiQuery(emergency= u"fire_flapper")
emergency_fire_hose = XapiQuery(emergency= u"fire_hose")
emergency_fire_hydrant = XapiQuery(emergency= u"fire_hydrant")
emergency_phone = XapiQuery(emergency= u"phone")
emergency_ses_station = XapiQuery(emergency= u"ses_station")
emergency_siren = XapiQuery(emergency= u"siren")
