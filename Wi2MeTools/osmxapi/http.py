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

import sys
import httplib, time

class XapiHttpError(Exception):
  	
    def __init__(self, status, reason, payload):
        self.status = status
        self.reason  = reason
        self.payload = payload
    #.

    def __str__(self):
        return "[{0}] {1}: {2}".format(self.status, self.reason, self.payload)
    #.
#.

class Http:

    def __init__(self, api, debug=False):
        self._api = api
        self._debug = debug
        self._conn = httplib.HTTPConnection(self._api, 80)
    #.

    def http_request(self, cmd, path, auth, send):

        if self._debug:
            path2 = path
            if len(path2) > 50:
                path2 = path2[:50]+"[...]"
            #.
            #print >>sys.stderr, "%s %s %s"%(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2)
            print >>sys.stderr, "%s %s %s"%(time.strftime("%F %T"),cmd,path2)
        #.

        self._conn.putrequest(cmd, path)
        if send != None:
            self._conn.putheader('Content-Length', len(send))
        #.
        self._conn.endheaders()
        if send:
            self._conn.send(send)
        #.
        response = self._conn.getresponse()
        if response.status != 200:
            payload = response.read().strip()
            if response.status == 410:
                return None
            #.
            raise XapiHttpError(response.status, response.reason, payload)
        #.
        if self._debug:
            #print >>sys.stderr, "%s %s %s done"%(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2)
            print >>sys.stderr, "%s %s %s done"%(time.strftime("%F %T"),cmd,path2)
        #.
        return response.read()
    #.

    def http(self, cmd, path, auth, send):
        i = 0
        while True:
            i += 1
            try:
                return self.http_request(cmd, path, auth, send)
            #.
            except XapiHttpError, e:
                if e.status >= 500:
                    if i == 5: raise
                    if i != 1: time.sleep(5)
                    self._conn = httplib.HTTPConnection(self._api, 80)
                #.
                else: 
                    raise
                #.
            except Exception:
                if i == 5: raise
                if i != 1: time.sleep(5)
                self._conn = httplib.HTTPConnection(self._api, 80)
            #.
        #. 
    #.

    def get(self, path):
        return self.http('GET', path, False, None)
    #.
#.
