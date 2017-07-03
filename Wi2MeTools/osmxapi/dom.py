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

import xml.dom.minidom

def parseNode(domelement):
    """ Returns NodeData for the node. """
    result = getAttributes(domelement)
    result[u"tag"] = getTag(domelement)
    return result
#.

def parseWay(domelement):
    """ Returns WayData for the way. """
    result = getAttributes(domelement)
    result[u"tag"] = getTag(domelement)
    result[u"nd"]  = getNd(domelement)        
    return result
#.

def parseRelation(domelement):
    """ Returns RelationData for the relation. """
    result = getAttributes(domelement)
    result[u"tag"]    = getTag(domelement)
    result[u"member"] = getMember(domelement)
    return result
#.

def getAttributes(domelement):
    """ Returns a formatted dictionary of attributes of a domelement. """
    result = {}
    for k, v in domelement.attributes.items():
        if k == u"uid"         : v = int(v)
        elif k == u"changeset" : v = int(v)
        elif k == u"version"   : v = int(v)
        elif k == u"id"        : v = int(v)
        elif k == u"lat"       : v = float(v)
        elif k == u"lon"       : v = float(v)
        elif k == u"open"      : v = v=="true"
        elif k == u"visible"   : v = v=="true"
        elif k == u"ref"       : v = int(v)
        result[k] = v
    #.
    return result            
#.

def getTag(domelement):
    """ Returns the dictionary of tags of a domelement. """
    result = {}
    for t in domelement.getElementsByTagName("tag"):
        k = t.attributes["k"].value
        v = t.attributes["v"].value
        result[k] = v
    #.
    return result
#.

def getNd(domelement):
    """ Returns the list of nodes of a domelement. """
    result = []
    for t in domelement.getElementsByTagName("nd"):
        result.append(int(int(t.attributes["ref"].value)))
    return result            
#.

def getMember(domelement):
    """ Returns a list of relation members. """
    result = []
    for m in domelement.getElementsByTagName("member"):
        result.append(getAttributes(m))
    return result
#.
