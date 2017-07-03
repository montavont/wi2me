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

# Class for dealing conveniently with bounding boxes.
# Some of the methods requires the pyproj module to be installed

class Bbox:

    def __init__(self, **keyw):

        self.name = "bbox"
        if not keyw:
            self.set(0,0,0,0)
            return None
        #.

        assert set(keyw.keys()) == set(['lonw','lone','lats','latn'])
        lonw = keyw['lonw']
        lone = keyw['lone']
        latn = keyw['latn']
        lats = keyw['lats']

        self.set(lonw, lats, lone, latn)
        return None
    #.

    def set (self, lonw, lats, lone, latn):
        assert lonw <= lone
        assert lats <= latn
        self.lonw = lonw
        self.lats = lats
        self.lone = lone
        self.latn = latn
    #.

    def __repr__(self):
        return "{4}: [{0}, {1}, {2}, {3}]".format(self.lonw,
                                                  self.lats,
                                                  self.lone,
                                                  self.latn,
                                                  self.name)
    #.

    def __getitem__(self, k):
        """Return bbox items, order is: West, South, East, North"""
        if k == 0:
            return self.lonw
       #.
        if k == 1:
            return self.lats
       #.
        if k == 2:
            return self.lone
       #.
        if k == 3:
            return self.latn
        #.
        raise IndexError("Bbox index out of range")
   #.

    def __iter__(self):
        raise Exception("Bbox class is not iterable")
   #.

    def center (self, lon, lat, dst):
        """Set the bbox given a center and a size in meter"""
        
        from pyproj import Geod

        g = Geod(ellps='WGS84')

        # go dst/2 east to find lon_max
        lon_max = g.fwd(lon, lat, 90.0, dst/2, radians=False)[0]

        # go dst/2 west to find lon_min
        lon_min = g.fwd(lon, lat, 270., dst/2, radians=False)[0]

        # go dst/2 north to find lat_max
        lat_max = g.fwd(lon, lat, 0., dst/2, radians=False)[1]

        # go dst/2 south to find lat_min
        lat_min = g.fwd(lon, lat, 180., dst/2, radians=False)[1]
        
        self.set (lon_min, lat_min, lon_max, lat_max)
    #.

    def size (self):
        from pyproj import Geod

        g = Geod(ellps='WGS84')
        lon_min = self.lonw
        lon_max = self.lone
        lat_min = self.lats
        lat_max = self.latn
        lon = (lon_min+lon_max)/2
        lat = (lat_min+lat_max)/2

        sn = g.inv (lon, lat_min, lon, lat_max, radians=False)[2]
        we = g.inv (lon_min, lat, lon_max, lat, radians=False)[2]

        return (sn, we) 
#.

if __name__ == '__main__':

    lon = 10.538482
    lat = 55.845467
    bb = Bbox()
    bb.center(lon, lat, 1000)
    print bb
    print "Size of bbox in north-south direction:", bb.size()[0]
    print "Size of bbox in east-west  direction:", bb.size()[1]

    aarhus = Bbox(latn=56.169842, 
                       lats=56.1391956,
                       lone=10.2131633,
                       lonw=10.1461766)
    print aarhus
    print "Size of Aarhus", aarhus.size()

    holmesyd = Bbox(lats=56.1012473, 
                         lonw=10.1656151, 
                         latn=56.10881, 
                         lone=10.1810646)
    print holmesyd
    print "Size of Holme Syd", holmesyd.size()

    uniparken = Bbox (lats=56.1618032,
                           lonw=10.1891327,
                           latn=56.1719343,
                           lone=10.212822)
    print uniparken
    print "Size of AU campus", uniparken.size()

    mindeparken = Bbox (lats=56.1249836, 
                             lonw=10.2002907, 
                             latn=56.1339765,
                             lone=10.2150536)
    print mindeparken
    print "Size of Mindeparken", mindeparken.size()
#.
