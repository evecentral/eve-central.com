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


import psycopg2

import cherrypy

from copy import copy

def empireregions():
    return [10000001, 10000002, 10000016, 10000020, 10000028, 10000030, 10000032, 10000033, 10000043, 10000049, 10000037, 10000038, 10000036, 10000052, 10000064, 10000065, 10000067, 10000068, 10000054, 10000042,10000044, 10000048 ]

def emit_redirect( page):
    cherrypy.response.status = 302
    cherrypy.response.headers['Location'] =  page


class SorterDict(dict):
    def __init__(self,sortby):
        self.sortby = sortby
        self.reverse = False
        dict.__init__(self)
    def __cmp__(self,other):
        t = 1
        if self.reverse:
            t = -1
        if self[self.sortby] < other[self.sortby]:
            return -1*t
        if self[self.sortby] > other[self.sortby]:
            return 1*t
        return 0



def format_long(price):

    intpart = long(price)
    intpart = list(str(intpart))

    if price >= 1000:
        intpart.insert(len(intpart)-3, ',')
    if price >= 1000000:
        intpart.insert(len(intpart)-7, ',')
    if price >= 1000000000:
        intpart.insert(len(intpart)-11, ',')

    string = ""
    for x in intpart:
        string = string + x

    return string


def format_price(price):
    price = float(price)
    intpart = long(price)
    intpart = list(str(intpart))

    if price >= 1000:
        intpart.insert(len(intpart)-3, ',')
    if price >= 1000000:
        intpart.insert(len(intpart)-7, ',')
    if price >= 1000000000:
        intpart.insert(len(intpart)-11, ',')

    string = ""
    for x in intpart:
        string = string + x

    price = price - long(price)
    price = list(str("%f.2" % price))
    for x in price[1:4]:
        string = string + x

    return string

class EVCstate:
    def __init__(self, trust=False):
        pass

    def __getitem__(self, item):
        return cherrypy.session.get(item)

    def __delitem__(self, item):
        del cherrypy.session[item]

    def __setitem__(self, item, value):

        cherrypy.session[item] = value

    def __contains__(self, item):
        return item in cherrypy.session

    def save(self):
        pass
    def load(self):
        pass


def db_con():
    svndb = psycopg2.connect(database='evec', user='evec', host = 'localhost', port = '5432')
    svndb.autocommit = True
    svndb.set_isolation_level(0)
    return svndb


def simple_error(message):
    mess = ""
    mess += "<html><head><title>Simple Error</title></head><body>"
    mess += "<h3>Error</h3>" + message
    mess += "</body></html>"
    return mess


def format_long(price):

    intpart = long(price)
    intpart = list(str(intpart))

    if price >= 1000:
        intpart.insert(len(intpart)-3, ',')
    if price >= 1000000:
        intpart.insert(len(intpart)-7, ',')
    if price >= 1000000000:
        intpart.insert(len(intpart)-11, ',')

    string = ""
    for x in intpart:
        string = string + x

    return string


def format_price(price):
    price = float(price)
    intpart = long(price)
    intpart = list(str(intpart))

    if price >= 1000:
        intpart.insert(len(intpart)-3, ',')
    if price >= 1000000:
        intpart.insert(len(intpart)-7, ',')
    if price >= 1000000000:
        intpart.insert(len(intpart)-11, ',')

    string = ""
    for x in intpart:
        string = string + x

    price = price - long(price)
    price = list(str("%f.2" % price))
    for x in price[1:4]:
        string = string + x

    return string


def build_regionquery(front,rl_o):

    rl = copy(rl_o)

    if len(rl) < 1:
        return " 1 = 1 "

    r = rl.pop()
    query = "( "+front+".regionid = " + str(r) + "  "
    for r in rl:
        query = query + " OR "+front+".regionid = " + str(r) + ""
    query = query + ")"
    return query


def set_or_get(session, name, set, default):
    val = default
    if set is not None:
        session[name] = set
        return set
    if name in session:
        val = session[name]
    return val

def condense_list(list):
    string = str(list.pop())
    for item in list:
        string = string + ":" + item

    return string

def get_system_name(db, systemid):
    cur = db.cursor()
    cur.execute("SELECT systemname FROM systems WHERE systemid = %s", [systemid])
    row = cur.fetchone()
    return row[0]


def get_region_name(db, regionid):
    cur = db.cursor()
    cur.execute("SELECT regionname FROM regions WHERE regionid = %s", [regionid])
    row = cur.fetchone()
    return row[0]

def get_region_id(db, regionname):
    cur = db.cursor()
    cur.execute("SELECT regionid FROM regions WHERE regionname = %s", [regionname])
    row = cur.fetchone()
    return row[0]
    
def get_type_name(db, typeid):
    cur = db.cursor()
    cur.execute("SELECT typename FROM types WHERE typeid = %s", [typeid])
    row = cur.fetchone()
    return row[0]


def get_type_size(db, typeid):
    cur = db.cursor()
    cur.execute("SELECT size FROM types WHERE typeid = %s", [typeid])
    row = cur.fetchone()
    return row[0]

# Type list
def type_list(db):
    cur = db.cursor()

    cur.execute("SELECT typeid, typename FROM types ORDER BY typename")

    ret = []

    row = cur.fetchone()
    while row:
        ret.append({'typeid':row[0], 'typename':row[1]})
        row = cur.fetchone()

    return ret



# Region list
def region_list(db):
    cur = db.cursor()

    cur.execute("SELECT regionid, regionname FROM regions ORDER BY regionname")

    ret = []

    row = cur.fetchone()
    while row:
        ret.append({'regionid':row[0], 'regionname':row[1]})
        row = cur.fetchone()

    return ret
