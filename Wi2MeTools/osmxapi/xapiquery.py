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

# Query management for the Overpass API

boxkeys = set ([u'lonw',u'lats',u'lone',u'latn'])
metakeys = set ([u'newer', u'uid', u'user',u'meta'])

class XapiQueryError(Exception):
    	
    def __init__(self, reason, payload):
        self.reason  = reason
        self.payload = payload
    
    def __str__(self):
        return "{0}: {1}".format(self.reason, self.payload)
#.

class XapiQuery (dict):

    def __init__(self, *args, **kwargs):
        self.update(*args, **kwargs)
    #.

    def __getitem__(self, key):
        try:
            val = dict.__getitem__(self, key)
        except KeyError:
            val = None
        return val

    def __setitem__(self, key, val):
        key = key.lower()
        if key == 'uid':
            if 'user' in self:
                raise XapiQueryError("user already defined",self[u'user'])
            #.
        #.
        if key == 'user':
            if 'uid' in self:
                raise XapiQueryError("uid already defined",self[u'uid'])
            #.
        #.

        dict.__setitem__(self, key, val)
    #.

    def __repr__(self):
        s = u""
        keys = set(self.keys())

        others = keys-boxkeys-metakeys
        for k in others:
            s += u'[{0}={1}]'.format(k, self[k])
        #.

        # if all boxkeys are defined
        # Overpass order is West, South, East, North.
        if keys.intersection(boxkeys) == boxkeys:
            assert self[u'lonw'] <= self[u'lone']
            assert self[u'lats'] <= self[u'latn']
            s += u"[bbox={0},{1},{2},{3}]".format(self[u'lonw'],\
                                                     self[u'lats'],\
                                                     self[u'lone'],\
                                                     self[u'latn'])
        #.

        if 'newer'in keys:
            d = self[u'newer']
            s += u'[@newer={0}]'.format(d.strftime('%FT%TZ'))
        #.

        if 'uid' in keys:
            s += u'[@uid={0}]'.format(self[u'uid'])
        #.

        if 'user' in keys:
            s += u'[@user={0}]'.format(self[u'user'])
        #.

        if 'meta' in keys:
            s += u'[@meta]'
        #.

        return s.encode('utf-8')
    #.

    def update(self, *args, **kwargs):
        #print 'update', args, kwargs
        for k, v in dict(*args, **kwargs).iteritems():
            self[k] = v
        #.
    #.

    def box (self, lonw, lats, lone, latn):
        "Convenience method to set the bbox directly"
        assert lonw <= lone
        assert lats <= latn
        self[u'lonw'] = lonw
        self[u'lats'] = lats
        self[u'lone'] = lone
        self[u'latn'] = latn
    #.

    def bbox (self, bbox):
        "Convenience method to set the bbox using a bbox instance"
        self[u'lonw'] = bbox.lonw
        self[u'lats'] = bbox.lats
        self[u'lone'] = bbox.lone
        self[u'latn'] = bbox.latn
    #.

    # Add two queries
    def __add__(self, other):
        d = {}

        for k in other:
            d[k] = other[k]
        #.

        # self keys take prevalence, uid takes prevalence over user
        for k in self:
            if k == u'uid' and u'user' in d:
                del d['user']
            #.
            if k == u'user' and u'uid' in d:
                continue
            #.

            if k in other:
                d[k] = u'{0}|{1}'.format(self[k],other[k])
            else:
                d[k] = self[k]
            #.
        #.

        keyss = set(self.keys())
        keyso = set(other.keys())

        # check if both have boxes, if so expand the box
        if keyss & keyso & boxkeys == boxkeys:
            d['lats'] = min(self['lats'], other['lats'])
            d['latn'] = max(self['latn'], other['latn'])
            d['lone'] = max(self['lone'], other['lone'])
            d['lonw'] = min(self['lonw'], other['lonw'])
        #.

        return XapiQuery(d)
    #.
#.

if __name__ == '__main__':
    import bbox

    bb_aarhus = bbox.Bbox(latn=56.169842, 
                       lats=56.1391956,
                       lone=10.2131633,
                       lonw=10.1461766)

    bb_holmesyd = bbox.Bbox(lats=56.1012473, 
                         lonw=10.1656151, 
                         latn=56.10881, 
                         lone=10.1810646)

    bb_uniparken = bbox.Bbox (lats=56.1618032,
                           lonw=10.1891327,
                           latn=56.1719343,
                           lone=10.212822)

    bb_mindeparken = bbox.Bbox (lats=56.1249836, 
                             lonw=10.2002907, 
                             latn=56.1339765,
                             lone=10.2150536)

    aarhus = XapiQuery()
    aarhus.bbox(bb_aarhus)

    holmesyd = XapiQuery()
    holmesyd.bbox(bb_holmesyd)

    uniparken = XapiQuery ()
    uniparken.bbox(bb_uniparken)

    mindeparken = XapiQuery (lats=56.1249836, 
                              lonw=10.2002907, 
                              latn=56.1339765,
                              lone=10.2150536)

    uniparken['user'] = 'first'
    mindeparken['user'] = 'second'

    print "uniparken", uniparken
    print "mindeparken", mindeparken

    x = uniparken + mindeparken
    print "uniparken + mindeparken=", x
    y =  mindeparken + uniparken
    print "mindeparken+uniparken=", y

    holmesyd[u'uid'] = 123456
    print "uniparken+holmesyd", uniparken+holmesyd

    print "EXPECTING EXCEPTION!"
    try:
        uniparken[u'uid'] = 123456
    except XapiQueryError:
        print "adding uid when user already define threw exception as expected"
    #.
#.
