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
place_city = XapiQuery(place = u"city")
place_town = XapiQuery(place = u"town")
place_village = XapiQuery(place = u"village")
place_hamlet = XapiQuery(place = u"hamlet")
place_isolated_dwelling = XapiQuery(place = u"isolated_dwelling")
place_farm = XapiQuery(place = u"farm")
place_suburb = XapiQuery(place = u"suburb")
place_neighbourhood = XapiQuery(place = u"neighbourhood")
place_continent = XapiQuery(place = u"continent")
place_country = XapiQuery(place = u"country")
place_county = XapiQuery(place = u"county")
place_island = XapiQuery(place = u"island")
place_locality = XapiQuery(place = u"locality")
place_region = XapiQuery(place = u"region")
place_state = XapiQuery(place = u"state")
