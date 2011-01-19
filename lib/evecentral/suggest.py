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
    cur.execute("SELECT types.typename,types.typeid FROM types WHERE types.published = 1 AND types.marketgroup > 0 AND types.typeid NOT IN (SELECT DISTINCT ON (current_market.typeid) current_market.typeid FROM current_market WHERE current_market.reportedtime < (NOW() - interval '1 days') AND current_market.regionid = %s ) ORDER BY RANDOM() LIMIT 20", [region])
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

    return l
