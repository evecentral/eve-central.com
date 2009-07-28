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


class TradeFinderException(Exception):
    pass

class Order(object):
    def __init__(self, typeid, typename, systemid, systemname, stationid, stationname, price):
        self.typeid = typeid
        self.typename = typename
        self.systemid = systemid
        self.systemname = systemname
        self.stationid = stationid
        self.stationname = stationname
        self.price = price


class TradeFetcher(object):
    def __init__(self, db, querypoint, limits, bid = 0):
        self.qp = querypoint
        self.db = db
        self.bid = bid
        self.limits = limits
        self.orders = []
        self._fetch()

    def _fetch(self):
        cur = db.cursor()

        location = " = %(froml)s"

        if self.qp[0] == "System":
            location = "f.systemid" + location
        elif self.qp[0] == "Region":
            location = "f.regionid" + location
        else:
            location = " 1 = 1 " # Valid SQL, very pointless :)

        cur.execute("""SELECT types.typeid, types.typename, fsys.systemname, fs.systemid, fs.stationid, fs.stationname
                              f.price
                       FROM types, current_market AS f, stations AS fs, systems AS fsys
                       WHERE fs.systemid = fsys.systemid AND f.bid = %(bid)s
                             AND f.stationid = fs.stationid AND f.typeid = types.typeid AND """
                    + location
                    {'bid' : self.bid,
                     'froml' : self.qp[1],
                     })

        r = cur.fetchone()
        while r:
            o = Order(r[0], r[1], r[2], r[3], r[4], r[5], r[6])
            self.orders.append(o)
            r = cur.fetchone()
        return



class TradeFinder(object):

    VALID_LIMITS = ('size', 'security', 'age')

    def __init__(self, fromtype, fromt, totype, tot):
        self.fromt = (fromtype, fromt)
        self.tot = (totype, tot)
        self.limits["size"] = 10000
        self.limits["security"] = 0.4
        self.limits["age"] = 4

    def set_limiter(self, limiter, limit):
        if limiters not in TradeFinder.VALID_LIMITS:
            return TradeFinderException("Not a valid limit")

        self.limits[limiter] = limit
        return self.limits[limiter]

    def get_limiter(self, limiter):
        return self.limits[limiter]


    def fetch_orders(self, db):
        sells = TradeFetcher(db, self.fromt, self.limits, bid = 0).orders
        buys = TradeFetcher(db, self.tot, self.limits, bid = 1).orders
