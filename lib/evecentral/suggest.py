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


import random

def upload_suggest(db, region, rettype = "names"):

    cur = db.cursor()
    cur.execute("SELECT DISTINCT ON(types.typename) types.typename,types.typeid FROM types, current_market WHERE types.typeid = current_market.typeid AND current_market.regionid = %s AND current_market.reportedtime < (NOW() - interval '2 days') AND current_market.reportedtime > (NOW() - interval '12 days') GROUP BY types.typename,types.typeid,current_market.reportedtime LIMIT 200 OFFSET 0", [region])
    l = []

    r = cur.fetchone()
    while r:
        if rettype == "names":
            l.append(r[0])
        elif rettype == "ids":
            l.append(r[1])
        else:
            tup = (r[0], r[1])
            l.append(tup)

        r = cur.fetchone()
    random.shuffle(l)

    return l[0:20]
