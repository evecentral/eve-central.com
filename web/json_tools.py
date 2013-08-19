#    EVE-Central.com Codebase
#    Copyright (C) 2006-2012 StackFoundry LLC and Yann Ramin
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.


from hashlib import md5
import string
import os
import Cheetah.Template
import random
try:
    import simplejson
except:
    import json as simplejson

import cherrypy

from evecentral import display
from evecentral import evec_func
from evecentral import stats

import time

from evecentral.evec_func import EVCstate, SorterDict, format_long, format_price, emit_redirect, get_system_name, get_region_name, set_or_get



class JsonTools:


    @cherrypy.expose
    def system_search(self, name):
        cherrypy.response.headers['Content-Type'] = 'text/json'
        db = evec_func.db_con()
        cur = db.cursor();
        cur_r = db.cursor();
        name = name + '%';
        cur.execute("SELECT systemid,systemname,regionname FROM systems,regions WHERE regions.regionid = systems.regionid AND systemname ILIKE %s ORDER BY systemname", [name])
        cur_r.execute("SELECT regionid,regionname FROM regions WHERE regionname ILIKE %s ORDER BY regionname LIMIT 30", [name])

        json_list = []

        r = cur.fetchone()
        while r:
            m = { 'id' : r[0],
                  'name' : r[1],
                  'type' : 'System in ' + r[2]}
            json_list.append(m)
            r = cur.fetchone()

        r = cur_r.fetchone()
        while r:
            m = {'id' : r[0],
                 'name' : r[1] ,
                 'type': 'Region'}
            r = cur_r.fetchone()
            json_list.append(m)
        return simplejson.dumps(json_list)
