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

from numpy import *

import time

from evecentral.evec_func import EVCstate, SorterDict, format_long, format_price, emit_redirect, get_system_name, get_region_name, set_or_get



class Order:
    def __init__(self, id, type, price, system, station, station_name, volremain):
        self.id = id
        self.type = type
        self.price = price
        self.system = system
        self.station = station
        self.station_name = station_name
        self.volremain = volremain

class Comparison:
    def __init__(self, type, min, max, median):
        self.type = type
        self.min = min
        self.max = max
        self.median = median
        self.reverse = True

    def compare(self, other):
        #print self.type, self.min, other.min, self.min-other.min
        return Comparison(self.type, self.min - other.min, self.max - other.max, self.median - other.median)
    def __str__(self):
        return "Type: " + `self.type` +  "Price:" + `self.min`

    def __cmp__(self,other):
        t = 1
        if self.reverse:
            t = -1
        if self.min < other.min:
            return -1*t
        if self.min > other.min:
            return 1*t
        return 0

class TradeTool:

    def build_compare(self, db, system, mode, hours = 360, secfilter = 0.4):
        cur = db.cursor()
        sql_age = str(int(hours)) + " hours"
        filter_col = 'current_market.systemid'
        if mode == 'regions':
            filter_col = 'current_market.regionid'

        cur.execute("""SELECT current_market.typeid,
orderid,
price,
volremain,
types.typename,
stations.stationid,
stations.stationname
 FROM current_market,types,stations,systems

WHERE
systems.security > %s
AND systems.systemid = current_market.systemid
AND stations.stationid = current_market.stationid
AND age(reportedtime) < ' """ + sql_age + """'
AND bid = 0
AND """ + filter_col + """ = %s
AND types.typeid = current_market.typeid""", [secfilter, system])



        order_map = {}
        typename_map = {}

        r = cur.fetchone()
        while r:
            type = r[0]
            typename_map[type] = r[4]

            order =  Order(r[1], r[0], r[2], system, r[5], r[6], r[3])
            if type in order_map:
                order_map[type].append(order)
            else:
                order_map[type] = [order]

            r = cur.fetchone()
        return (order_map, typename_map)


    def combine_maps(self, map1, map2):
        types = set(map1.keys())
        types = types.intersection(map2.keys())
        omap = {}
        for type in types:
            # Build numpy arrays of prices and volumes remaining

            pri1_a = array([x.price for x in map1[type]], dtype = float)
            total1 = array([x.volremain for x in map1[type]], dtype = long)

            pri2_a = array([x.price for x in map2[type]], dtype = float)
            total2 = array([x.volremain for x in map2[type]], dtype = long)

            min1 = pri1_a.min()
            min2 = pri2_a.min()

            # Find a represenitive order now
            repre1 = None
            repre2 = None
            for x in map1[type]:
                if min1 == x.price:
                    repre1 = x
            for x in map2[type]:
                if min2 == x.price:
                    repre2 = x

            omap[type] = ( Comparison(type, pri1_a.min(), pri1_a.max(), median(pri1_a)),
                           Comparison(type, pri2_a.min(), pri2_a.max(), median(pri2_a)),
                           total1.sum(), total2.sum(),
                           repre1, repre2 )
        return omap


    @cherrypy.expose
    def index(self):
        session = EVCstate()
        t = display.template('tradetool.tmpl', session)
        return t.respond()


    @cherrypy.expose
    def compare(self, system1, system2, mode1="systems", mode2="systems", sethours = None, setsecfilter = None, setmaxisk = None, **kwargs):
        db = evec_func.db_con()
        session = EVCstate()

        # Get defaults for sessions
        hours = set_or_get(session, "compare_hours", sethours, 24)
        secfilter = set_or_get(session, "compare_sec", setsecfilter, 0.4)
        maxisk = int(set_or_get(session, "compare_max", setmaxisk, 10000000000))

        system1 = int(system1)
        system2 = int(system2)
        if system1 < 30000000:
            mode1 = "regions"

        if system2 < 30000000:
            mode2 = "regions"


        t = display.template('tradetool_compare.tmpl', session)

        (omap1,t1) = self.build_compare(db, system1, mode1, hours, secfilter = secfilter)
        (omap2,t2) = self.build_compare(db, system2, mode2, hours, secfilter = secfilter)
        t1.update(t2)
        typenames = t1

        omap = self.combine_maps(omap1, omap2)

        # Build a pricelist from the maps so we can display the page in order
        pricelist = []
        for type in omap.iterkeys():
            comp = omap[type][1].compare(omap[type][0])

            # Avoid adding in negative price items (halves the size of the page)

            if comp.min <= 0:
                continue
            if omap[type][0].min >= maxisk:
                continue

            pricelist.append(comp)

        pricelist = sort(pricelist)

        # OMap is the order map - comtaining comparisons between the middle
        # and medium price values, the sum of orders, and a representitive order
        t.omap = omap
        t.omap1 = omap1
        t.omap2 = omap2

        t.pricelist = pricelist
        t.namemap = typenames

        if mode1 == 'regions':
            t.system1name = get_region_name(db, system1)
        else:
            t.system1name = get_system_name(db, system1)

        if mode2 == 'regions':
            t.system2name = get_region_name(db, system2)
        else:
            t.system2name = get_system_name(db, system2)


        t.mode1 = mode1
        t.mode2 = mode2
        t.maxisk = maxisk
        t.system1 = system1
        t.system2 = system2
        t.hours = hours
        t.secfilter = secfilter
        return t.respond()
