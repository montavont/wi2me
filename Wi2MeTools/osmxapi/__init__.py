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
#  Copyright: © 2009-2010 Etienne Chové <chove@crans.org>
#  Copyright: © 2012 Morten Kjeldgaard <mok@bioxray.dk>
#  License: GPL-3+

__version__ = '0.1'

import xml.dom.minidom
import dom, http
import os.path

class OsmXapi:

    def __init__(self, api = "www.overpass-api.de", base="api", debug = False):
        self.debug = debug
        self.base = os.path.join('/', base, 'xapi')
        self.http = http.Http(api, debug)
    #.

    def nodeGet(self, query=None, raw=None):
        """ Returns NodeData for query """
        if not query:
            return None
        #.
        uri = self.base+"?node"+repr(query)
        data = self.http.get(uri)
        if raw: return data
        if not data: return data
        data = xml.dom.minidom.parseString(data)
        data = data.getElementsByTagName("osm")[0].getElementsByTagName("node")
        nodelist = []
        for n in data:
            nodelist.append(dom.parseNode(n))
        #.
        return nodelist
    #.


    def wayGet(self, query=None, raw=None):
        """Returns way data for query"""
        if not query:
            return None
        #.
        uri = self.base+"?way"+repr(query)
        data = self.http.get(uri)
        if raw: return data
        if not data: return data
        data = xml.dom.minidom.parseString(data)
        data = data.getElementsByTagName("osm")[0].getElementsByTagName("way")
        waylist = []
        for w in data:
            waylist.append(dom.parseWay(w))
        #.
        return waylist
    #.


    def relationGet(self, query=None, raw=None):
        """Return relation data for query"""
        uri = self.base+"?relation"+repr(query)
        data = self.http.get(uri)
        if raw: return data
        data = xml.dom.minidom.parseString(data)
        data = data.getElementsByTagName("osm")[0].getElementsByTagName("relation")
        relationlist = []
        for r in data:
            relationlist.append(dom.parseRelation(r))
        #.
        return relationlist
    #.

    def anyGet(self, query=None, raw=None):
        """Return any data for query"""
        uri = self.base+"?*"+repr(query)
        data = self.http.get(uri)
        if raw: return data
        data = xml.dom.minidom.parseString(data)
        anydict = {}
        for e in "node", "way", "relation":
            d = data.getElementsByTagName("osm")[0].getElementsByTagName(e)
            anylist = []
            for a in d:
                if e == "node":
                    anylist.append(dom.parseNode(a))
                #.
                if e == "way":
                    anylist.append(dom.parseWay(a))
                #.
                if e == "relation":
                    anylist.append(dom.parseRelation(a))
                #.
            #.
            anydict[e] = anylist
        #.
        return anydict
    #.

    #.

if __name__ == '__main__':
    from xapiquery import XapiQuery

    xapi = OsmXapi(debug = True)

    uniparken = XapiQuery (lats=56.1618032,
                           lonw=10.1891327,
                           latn=56.1719343,
                           lone=10.212822)

    uniparken[u'amenity'] = u'parking'

    N = xapi.nodeGet(uniparken)
    print N

    W = xapi.wayGet(uniparken)
    print W

    A = xapi.anyGet(uniparken)
    print A
#.
