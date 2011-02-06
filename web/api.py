#    EVE-Central.com Codebase
#    Copyright (C) 2006-2009 StackFoundry LLC and Yann Ramin
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

import cherrypy

from evecentral import display
from evecentral import evec_func
from evecentral import stats
from evecentral.evec_func import EVCstate, SorterDict, format_long, format_price, emit_redirect
from evecentral import suggest

import evec

import datainput

from numpy import *

import time


class StatHolder:
    def __init__(self, typeid, prices, buy, sell):
        self.prices = prices
        self.buy = buy
        self.sell = sell
        self.typeid = typeid


class Api:

    @cherrypy.expose
    def webhook(self, type_id = None, region_id = None, source = None, data = None):
        print "WEBHOOK", type_id, region_id, source, data
        di = datainput.DataInput()
        data = "This is a header\n" + data # Fake in a header for the sake of inputdata
        resp = di.inputdata("notype", None, data)
        print resp

        return "Thanks for using webhook, the datainput adapter of choice  " + resp

    @cherrypy.expose
    def upload_suggest(self, region = None):
        cherrypy.response.headers['Content-Type'] = 'text/xml'
        region = int(region)
        db = evec_func.db_con()
        sug = suggest.upload_suggest(db, region, "both")

        t = display.template('upload_suggest_xml.tmpl', None)
        t.suggest = sug

        db.close()
        return t.respond()



    @cherrypy.expose
    def evemon(self, hours = "360"):
        cherrypy.response.headers['Content-Type'] = 'text/xml'
        db = evec_func.db_con()

        hours = int(hours)

        response = ""
        response += "<minerals>\n"

        empireregions = [10000001, 10000002, 10000016, 10000020, 10000028, 10000030, 10000032, 10000033, 10000043, 10000049, 10000037, 10000038, 10000036, 10000052, 10000064, 10000065, 10000067, 10000068, 10000054, 10000042,10000044, 10000048 ]

        for mineral in [34, 35, 36, 37, 38, 39, 40, 11399]:
            prices = stats.item_stat(db, mineral, hours, buysell = False, minQ = 5000,
                                     regionlimit = empireregions)

            typename = evec_func.get_type_name(db, mineral)
            response += " <mineral>\n"
            response += " <name>"+typename+"</name>"
            response += " <price>"+`prices['median']`+"</price>"
            response += " </mineral>\n"

        response += "</minerals>\n"
        return response


    @cherrypy.expose
    def quicklook(self, typeid, sethours = None, regionlimit = None, usesystem = None, setminQ = 0, poffset = 0):

        if regionlimit is None:
            regionlimit = []

        h = evec.Home()
        return h.quicklook(typeid = typeid, sethours = sethours, regionlimit = regionlimit, usesystem = usesystem, setminQ = setminQ, outtype = 'xml', api = 2.0)

    @cherrypy.expose
    def marketstat(self, hours = "360", minQ = 0, typeid = None, regionlimit = None, usesystem = None):

        cherrypy.response.headers['Content-Type'] = 'text/xml'
        db = evec_func.db_con()

        if typeid is None:
            typeid = []
        if regionlimit is None:
            regionlimit = []

        if not isinstance(regionlimit, list):
            regionlimit = [regionlimit]

        regionlimit = map(int, regionlimit)

        if int(hours) > 360:
            hours = "360"
        hours = int(hours)


        typeids = []
        if type(typeid) != list:
            typeids = [typeid]
        else:
            typeids = typeid

        if len(typeids) > 100:
            db.close()
            return "<evec_api><error>No more than 100 results allowed</error></evec_api>"

        sql_system = " "

        if usesystem:
            usesystem = int(usesystem)
            sql_system = " AND current_market.systemid = " + `usesystem` + " "


        statslist = []
        for typeid in typeids:

            typeid = int(typeid)

            useMinQ = minQ

            if useMinQ == 0 and typeid in stats.MINQ_TYPES:
                useMinQ = stats.MINQ_VOL

            prices = stats.item_stat(db, typeid, hours, sql_system, regionlimit = regionlimit, buysell = False, minQ = useMinQ)
            newprices = {}
            # Reformat prices as 2 digit float strings
            for key in prices.keys():
                newprices[key] = "%0.2f" % prices[key]
            (sell,buy) = stats.item_stat(db, typeid, hours, sql_system, regionlimit = regionlimit, buysell = True, minQ = useMinQ)
            newsell = {}
            newbuy = {}
            for key in sell.keys():
                newsell[key] = "%0.2f" % sell[key]
            for key in buy.keys():
                newbuy[key] = "%0.2f" % buy[key]

            statslist.append(StatHolder(typeid, newprices, newbuy, newsell))
        
        t = display.template('marketstat_xml.tmpl', None)
        t.types = statslist

        db.close()
        return t.respond()
