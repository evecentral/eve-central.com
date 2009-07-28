import psycopg2

import cherrypy

from copy import copy


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
	ua = ""
	try:
	    ua = cherrypy.request.headers['User-Agent']
	except:
	    pass

	if ua[0:3] == "EVE":
	    if 'isigb' not in self:
		self['isigb'] = True

	    if cherrypy.request.headers['Eve.trusted'] == 'no' and trust is True:
		cherrypy.response.headers['Eve.trustme'] = 'http://eve-central.com/::Registering requires trusting EVE-Central'

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
    svndb = psycopg2.connect(database='evec', user='evec', host = 'localhost', port = '9999')
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
