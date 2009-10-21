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


import md5
import string
import os
import Cheetah.Template
import random
import Pyro.core

import cherrypy

from evecentral import display
from evecentral import evec_func
from evecentral import stats
from evecentral import cache

from numpy import *

import time

from evecentral.evec_func import EVCstate, SorterDict, format_long, format_price, emit_redirect

from usermanager import Users
from corps import Corps
from datainput import DataInput
from tradetool import TradeTool
from evecentral.suggest import upload_suggest


try:
    from api import Api
except:
    pass



class Home:

    @cherrypy.expose
    def cache_stat(self):
        cherrypy.response.headers['Content-Type'] = 'text/plain'
        r = "Cache hit: " + str(cache.hits()) + "\n"
        r += "Cache miss: " + str(cache.miss()) + "\n"
        r += "%: " + str(float(cache.hits()) / cache.miss()) + "\n"
        r += "Last key stored: " + cache.last_key_s() + "\n"
        r += "Last key fetched: " + cache.last_key_f() 
        return r


    @cherrypy.expose
    def typesearch(self, search):
        session = EVCstate()

        t = display.template('typesearch.tmpl', session)
        t.search = search
        db = evec_func.db_con()
        search = search.lower()

        search = "%" + search + "%"

        notmask = '%Blueprint'

        if search.find('blueprint') != -1:
            notmask = 'this wont ever exist'


        cur = db.cursor()
        cur.execute("SELECT typename,typeid FROM types WHERE typename ILIKE %s AND typename NOT ILIKE %s ORDER BY typename", [search, notmask])
        types = []
        item = cur.fetchone()
        while item:

            types.append({'typeid':item[1], 'typename':item[0]})

            item = cur.fetchone()

        t.types = types


        db.close()

        if len(types) == 1:
            emit_redirect('/home/quicklook.html?typeid='+str(int(types[0]['typeid'])))
            return




        return t.respond()


    typesearch_html = typesearch

    @cherrypy.expose
    def marketstat_xml(self, hours = "360", minQ = 0, typeid = None, dump=None, evemon = None, regionlimit = None):

        cherrypy.response.headers['Content-Type'] = 'text/xml'
        db = evec_func.db_con()

        if regionlimit is None:
            regionlimit = []

        if not isinstance(regionlimit, list):
            regionlimit = [regionlimit]

        regionlimit = map(int, regionlimit)



        if int(hours) > 360:
            hours = "360"
        hours = int(hours)

        response = ""

        response += "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
        response += "<!-- Automatically generated data from EVE-Central.com -->\n"
        response += "<!-- WARNING: THIS API IS NOW DEPRECATED - PLEASE USE THE NEW API -->\n"

        if typeid is not None and dump is None:

            typeid = int(typeid)

            if minQ == 0 and typeid in [34, 35, 36, 37, 38, 39, 40, 11399]:
                minQ = 10000

            prices = stats.item_stat(db, typeid, hours, buysell = False, regionlimit = regionlimit, minQ = minQ)
            (sell,buy) = stats.item_stat(db, typeid, hours, regionlimit = regionlimit)

            response += "<market_stat>\n"
            response += " <typeid>"+`typeid`+"</typeid>\n"
            response += " <avg_price>"+`prices['avg_price']`+"</avg_price>\n"
            response += " <total_volume>"+`prices['total_vol']`[:-1]+"</total_volume>\n"
            response += " <avg_buy_price>"+`buy['avg_price']`+"</avg_buy_price>\n"
            response += " <total_buy_volume>"+`buy['total_vol']`[:-1]+"</total_buy_volume>\n"
            response += " <stddev_buy_price>" + `buy['stddev']`+"</stddev_buy_price>\n"
            response += " <max_buy_price>" + `buy['max']`+"</max_buy_price>\n"
            response += " <min_buy_price>" + `buy['min']`+"</min_buy_price>\n"
            response += "\n"
            response += " <avg_sell_price>"+`sell['avg_price']`+"</avg_sell_price>\n"
            response += " <total_sell_volume>"+`sell['total_vol']`[:-1]+"</total_sell_volume>\n"
            response += " <stddev_sell_price>" + `sell['stddev']`+"</stddev_sell_price>\n"
            response += " <max_sell_price>" + `sell['max']`+"</max_sell_price>\n"
            response += " <min_sell_price>" + `sell['min']`+"</min_sell_price>\n"

            response += " <median>"
            response += "  <sell>" + `sell['median']`+ "</sell>"
            response += "  <buy>" + `buy['median']` + "</buy>"
            response += "  <all>" + `prices['median']` + "</all></median>"

            response += "</market_stat>"
        elif typeid is not None and dump is not None:
            typeid = int(typeid)
            response += "<market_dump>\n"
        elif evemon is not None:

            response += "<minerals>\n"

            for mineral in [34, 35, 36, 37, 38, 39, 40, 11399]:
                prices = stats.item_stat(db, mineral, hours, buysell = False, minQ = 5000)

                typename = evec_func.get_type_name(db, mineral)
                response += " <mineral>\n"
                response += " <name>"+typename+"</name>"
                response += " <price>"+`prices['median']`+"</price>"
                response += " </mineral>\n"

            response += "</minerals>\n"



        db.close()
        return response

    marketstat_xml_html = marketstat_xml


    @cherrypy.expose
    def quicklook(self, typeid, setorder=None, setdir = None, igbover = False, sethours = None, regionlimit = None, usesystem = None, setminQ = 0, poffset = 0, outtype = 'html', api = 1.0):
        session = {}




        if outtype == 'html':
            session = EVCstate()
            cherrypy.response.headers['Content-Type'] = 'text/html'
        elif outtype == 'xml':
            cherrypy.response.headers['Content-Type'] = 'text/xml'


        db = evec_func.db_con()

        if regionlimit is None:
            regionlimit = []

        if type(regionlimit) != list:
            regionlimit = [int(regionlimit)]
        else:
            regionlimit = [int(x) for x in regionlimit]



        if 'regionlimit' in session and len(regionlimit) == 0:
            regionlimit = session['regionlimit']

        randomregion = None



        up_sug = None
        if len(regionlimit) == 0:
            pass
        elif random.randint(1,50) > 10:

            random.shuffle(regionlimit)
            randomregion = regionlimit[0]
            up_sug = upload_suggest(db, randomregion)


        order = 'price'
        orderdir = 'ASC'
        borderdir = 'DESC'
        hours = 15*24
        minQ = 0

        if setminQ:
            minQ = int(setminQ)
            session['minQ'] = minQ

        if 'minQ' in session:
            minQ = session['minQ']

        if setdir:
            if setdir == '1':
                orderdir = "ASC"
                borderdir = 'DESC'
            else:
                orderdir = "DESC"
                borderdir = 'ASC'
            session['orderdir'] = orderdir
            session['borderdir'] = borderdir

        if 'orderdir' in session:
            orderdir = session['orderdir']
        if 'borderdir' in session:
            borderdir = session['borderdir']



        if sethours:
            hours = int(sethours)
            session['orderhours'] = hours

        if 'orderhours' in session:
            hours = session['orderhours']

        if setorder:
            if setorder not in ['volremain', 'stationname', 'regionname', 'price']:
                raise "SetOrder fail"
            order = setorder
            session['order'] = order
        if 'order' in session:
            order = session['order']



        t = None
        if outtype == 'html':
            t = display.template('quicklook.tmpl', session)
        elif outtype == 'xml':
            t = display.template('quicklook_xml.tmpl', session)
            t.api = api

        typename = ""
        try:
            typename = evec_func.get_type_name(db, typeid)
        except:
            return "Can't find that type"

        typesize = evec_func.get_type_size(db, typeid)

        t.typename = typename
        t.typesize = typesize
        t.typeid = typeid
        t.sortorder = order
        reg_block = evec_func.build_regionquery("regions", regionlimit)


        sql_age = `hours`+" hours"

        sql_system = " "
        if usesystem:
            usesystem = int(usesystem)
            sql_system = " AND current_market.systemid = " + `usesystem` + " "

        # statistics for selling


        (sell, buy) = stats.item_stat(db, typeid, hours, sql_system, regionlimit = regionlimit, minQ = minQ)



        t.b_avg_price = format_price(buy['median'])
        t.b_total_vol = format_long(buy['total_vol'])
        t.b_total_movement = format_long(buy['total_movement'])

        t.avg_price = format_price(sell['median'])
        t.total_vol = format_long(sell['total_vol'])
        t.total_movement = format_long(sell['total_movement'])


        # do query here

        cur_buy = db.cursor()
        cur_sell = db.cursor()
        limit = "LIMIT 10000 OFFSET 0"
        try:
            if igbover is False and session['isigb'] == True:
                limit = "LIMIT 25 OFFSET " + str(int(poffset))
        except:
            pass


        cur_trans = db.cursor()
        cur_trans.execute("SELECT wmt.price,wmt.stationname,wmt.transtime,wmt.quantity FROM wallet_market_transactions AS wmt WHERE typeid = %s ORDER BY transtime DESC LIMIT 10", [typeid])

        transactions = []

        r = cur_trans.fetchone()
        while r:
            rec = {}
            rec['price'] = format_price(float(r[0]))
            rec['stationname'] = r[1]
            rec['transtime'] = r[2]
            rec['quantity'] = format_long(long(r[3]))
            r = cur_trans.fetchone()
            transactions.append(rec)

        t.poffset = int(poffset)

        # Fetch from cache or run query

        buys = []
        sells = []


        cache_key = cache.generic_key("evec_quicklook", typeid, regionlimit, usesystem, order, limit, minQ)
        cache_result = cache.get(cache_key)
        

        def run_query():

            cur_buy.execute("SELECT bid,current_market.systemid,current_market.stationid,price,volremain,(issued+duration),range,regionname, (reportedtime),stationname,security,minvolume,regions.regionid,orderid FROM current_market,regions,stations,systems WHERE " + reg_block + " AND stations.systemid = systems.systemid AND typeid = %s AND stations.stationid = current_market.stationid AND current_market.regionid = regions.regionid AND age(reportedtime) < '"+sql_age+"' AND volremain >= %s AND current_market.bid = 1  " + sql_system + " ORDER BY " + order + " " + borderdir + " " + limit, [typeid,minQ])

            cur_sell.execute("SELECT bid,current_market.systemid,current_market.stationid,price,volremain,(issued+duration),range,regionname,(reportedtime),stationname,security,regions.regionid,orderid FROM current_market,regions,stations,systems WHERE " + reg_block + " AND typeid = %s AND stations.systemid = systems.systemid AND stations.stationid = current_market.stationid AND current_market.regionid = regions.regionid AND age(reportedtime) < '"+sql_age+"'	AND volremain >= %s AND current_market.bid = 0 " + sql_system + " ORDER BY " + order + " " + orderdir + " " + limit, [typeid,minQ])


            for (query,lista,isbuy) in [(cur_buy, buys, True), (cur_sell, sells, False)]:
                r = query.fetchone()
                while r:
                    rec = {}
                    rec['systemid'] = r[1]
                    rec['stationid'] = r[2]
                    price = float(r[3])
                    string = format_price(price)
                    
                    rec['price'] = string
                    rec['price_raw'] = price
                    rec['volremain'] = format_long(r[4])
                    rec['volremain_raw'] = r[4]
                    rec['expires'] = str(r[5])[0:10]
                    if r[0] == True:
                        rec['range'] = r[6]
                    else:
                        rec['range'] = -2
                    rec['regionname'] = r[7]
                    
                    rec['reportedtime'] = str(r[8])[5:-7]
                    rec['stationname'] = r[9]
                    rec['security'] = str(r[10])[0:3]
                    # Try to grab regionid from the end of the query
                    if isbuy:
                        if int(r[11]) > 1:
                            rec['minvolume'] = format_long(int(r[11]))
                            rec['minvolume_raw'] = int(r[11])
                        else:
                            rec['minvolume'] = 1
                            rec['minvolume_raw'] = 1
                        rec['regionid'] = r[12]
                        rec['orderid']  = r[13]
                    else:
                        rec['minvolume'] = 1
                        rec['minvolume_raw'] = 1
                        rec['regionid'] = r[11]
                        rec['orderid'] = r[12]

                    lista.append(rec)

                    r = query.fetchone()



        # pass in info here

        if cache_result is None:
            run_query()
            cache.set(cache_key, (buys,sells))
        else:
            buys = cache_result[0]
            sells = cache_result[1]
            

        t.regions = evec_func.region_list(db)
        t.upload_sug = up_sug
        t.upload_reg = randomregion
        t.regionlimit = regionlimit
        t.usesystem = usesystem
        t.hours = hours
        t.minQ = minQ
        t.buys = buys
        t.sells = sells
        t.mtransaction = transactions
        db.close()
        return t.respond()

    

    quicklook_html = quicklook

    @cherrypy.expose
    def tradefind_display(self, qtype, fromt, to, set = None, age = 24, cashonhand = 10000000.00, minprofit = 100000, size = 10000, startat = 0, limit = 10, newsearch = "0", sort = "jprofit", prefer_sec = "0"):


        mapserver = Pyro.core.getProxyForURI("PYROLOC://localhost:7766/mapserver")


        session = EVCstate()
        t = display.template('tradefind_display.tmpl', session)
        db = evec_func.db_con()
        newsearch = int(newsearch)
        if 'trade_results' not in session:
            newsearch = 1

        cur = db.cursor()

        limit = int(limit)
        prefer_sec = int(prefer_sec)
        recalc_route = False


        if sort not in ['jprofit', 'sprofit', 'profit', 'jumps']:
            return

        if set:
            session['tf_age'] = age
            session['tf_minprofit'] = minprofit
            session['tf_size'] = size
            session['tf_limit'] = limit
            session['tf_sort'] = sort
            session['tf_prefer_sec'] = prefer_sec
            newsearch = 1
        else:
            if 'tf_age' in session:
                age = session['tf_age']
            if 'tf_minprofit' in session:
                minprofit = session['tf_minprofit']
            if 'tf_size' in session:
                size = session['tf_size']
            if 'tf_limit' in session:
                limit = session['tf_limit']
            if 'tf_sort' in session:
                sort = session['tf_sort']
            if 'tf_prefer_sec' in session:
                prefer_sec = session['tf_prefer_sec']
            else:
                prefer_sec = 1


        age_t = str(int(age)) + " hours"

        size = str(int(size))
        cashonhand = str(double(cashonhand))

        sql_profit_size = """    (t.price - f.price)* min(""" + size + """, min(t.volremain,f.volremain) * types.size)/types.size """
        sql_profit_jumps = """   '1' """
        #sql_cash = """ f.price <= $(cashonhand)s """

        sql_sec_limit = """ """
        if prefer_sec:
            sql_sec_limit = """ fsys.security > 0.4 AND tsys.security > 0.4 AND """

        cur_f = db.cursor()
        cur_t = db.cursor()


        # Pure suck query
        if qtype == "Systems" and newsearch:

            cur.execute("""SELECT types.typeid,types.typename,fs.systemid,ts.systemid,fs.stationname,ts.stationname,t.price - f.price AS pricediff,
            min(t.volremain,f.volremain),
            (t.price - f.price)* min(t.volremain,f.volremain) AS profit,
            """ + sql_profit_size + """ AS profit_size,
            """ + sql_profit_jumps + """ AS profit_jumps,
            t.price,f.price,
            t.volremain,f.volremain

            FROM
            types, current_market AS f, current_market AS t, stations AS fs, stations AS ts, systems AS fsys, systems AS tsys
            WHERE
            """ + sql_sec_limit + """
            ts.systemid = tsys.systemid AND fs.systemid = fsys.systemid AND
            f.bid = 0 AND t.bid = 1 AND f.systemid = %(fromt)s AND t.systemid = %(to)s AND t.typeid = f.typeid AND t.stationid = ts.stationid AND
            f.stationid = fs.stationid AND
            age(f.reportedtime) < %(age)s AND age(t.reportedtime) < %(age)s AND
            """ + sql_profit_size + """ >=	%(minprofit)s AND

            types.size <= """ + size + """ AND
            t.typeid = types.typeid
            AND f.typeid = types.typeid
            AND f.price < t.price""",

                        {'fromt':fromt, 'to':to, 'age':age_t, 'minprofit':minprofit,})

            cur_f.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemid = %s ORDER BY systemname", [fromt])
            cur_t.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemid = %s ORDER BY systemname", [to])


        elif qtype == "Regions" and newsearch:
            cur.execute("""SELECT types.typeid,types.typename,fs.systemid,ts.systemid,fs.stationname,ts.stationname,t.price - f.price AS pricediff,
            min(t.volremain,f.volremain),
            (t.price - f.price)*min(t.volremain,f.volremain) AS profit,


            """+ sql_profit_size + """ AS profit_size,
            """ + sql_profit_jumps + """ AS profit_jumps,


            t.price,f.price, t.volremain, f.volremain

            FROM types, current_market AS f, current_market AS t, stations AS fs, stations AS ts, systems AS fsys, systems AS tsys
            WHERE
            """ + sql_sec_limit + """
            ts.systemid = tsys.systemid AND fs.systemid = fsys.systemid AND
            f.bid = 0 AND t.bid = 1 AND f.regionid = %(fromt)s AND t.regionid = %(to)s AND t.typeid = f.typeid AND t.stationid = ts.stationid AND
            f.stationid = fs.stationid AND
            age(f.reportedtime) < %(age)s AND age(t.reportedtime) < %(age)s AND
            """ + sql_profit_size + """ >=	%(minprofit)s AND

            types.size <= """ + size + """ AND
            t.typeid = types.typeid
            AND f.typeid = types.typeid
            AND f.price < t.price""",
                        {'fromt':fromt, 'to':to, 'age':age_t, 'minprofit':minprofit, })
            cur_f.execute("SELECT regionid,regionname FROM regions WHERE regionid = %s ORDER BY regionname", [fromt])
            cur_t.execute("SELECT regionid,regionname FROM regions WHERE regionid = %s ORDER BY regionname", [to])



        elif qtype == "SystemToRegion" and newsearch:
            cur.execute("""SELECT types.typeid,types.typename,fs.systemid,ts.systemid,fs.stationname,ts.stationname,t.price - f.price AS pricediff,
            min(t.volremain,f.volremain),
            (t.price - f.price)*min(t.volremain,f.volremain) AS profit,


            """ + sql_profit_size + """ AS profit_size,
            """ + sql_profit_jumps + """ AS profit_jumps,


            t.price,f.price, t.volremain, f.volremain

            FROM types, current_market AS f, current_market AS t, stations AS fs, stations AS ts, systems AS fsys, systems AS tsys
            WHERE
            """ + sql_sec_limit + """
            ts.systemid = tsys.systemid AND fs.systemid = fsys.systemid AND
            f.bid = 0 AND t.bid = 1 AND f.systemid = %(fromt)s AND t.regionid = %(to)s AND t.typeid = f.typeid AND t.stationid = ts.stationid AND
            f.stationid = fs.stationid AND
            age(f.reportedtime) < %(age)s AND age(t.reportedtime) < %(age)s AND
            """ + sql_profit_size + """ >=	%(minprofit)s AND


            types.size <= """ + size + """ AND
            t.typeid = types.typeid
            AND f.typeid = types.typeid
            AND f.price < t.price""",

                        {'fromt':fromt, 'to':to, 'age':age_t, 'minprofit':minprofit, })
            cur_f.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemid = %s ORDER BY systemname", [fromt])
            cur_t.execute("SELECT regionid,regionname FROM regions WHERE regionid = %s ORDER BY regionname", [to])

        elif qtype == "Global" and newsearch:
            cur.execute("""SELECT types.typeid,types.typename,fs.systemid,ts.systemid,fs.stationname,ts.stationname,t.price - f.price AS pricediff,
            min(t.volremain,f.volremain),
            (t.price - f.price)*min(t.volremain,f.volremain) AS profit,


            """+ sql_profit_size + """ AS profit_size,
            """ + sql_profit_jumps + """ AS profit_jumps,


            t.price,f.price

            FROM types, current_market AS f, current_market AS t, stations AS fs, stations AS ts, systems AS fsys, systems AS tsys
            WHERE
            """ + sql_sec_limit + """
            ts.systemid = tsys.systemid AND fs.systemid = fsys.systemid AND
            f.bid = 0 AND t.bid = 1 AND t.typeid = f.typeid AND t.stationid = ts.stationid AND
            f.stationid = fs.stationid AND
            age(f.reportedtime) < %(age)s AND age(t.reportedtime) < %(age)s AND
            """ + sql_profit_size + """ >=	%(minprofit)s AND

            types.size <= """ + size + """ AND
            t.typeid = types.typeid
            AND f.typeid = types.typeid
            AND f.price < t.price""",
    #    ORDER BY profit_size DESC LIMIT %(limit)s OFFSET %(startat)s""",
                        {'age':age_t, 'minprofit':minprofit})
            cur_f.execute("SELECT regionid,regionname FROM regions WHERE regionid = %s ORDER BY regionname", [fromt])
            cur_t.execute("SELECT regionid,regionname FROM regions WHERE regionid = %s ORDER BY regionname", [to])



        trades = []
        time_net = 0.0
        import __builtin__
        from_set = __builtin__.set()
        from_map = {}
        from_to_map = {}
        error_log = []

        fromcache = 0
        if newsearch:

            r = cur.fetchone()
            while r:
                row = SorterDict('sortby')
                row['typeid'] = r[0]
                row['typename'] = r[1]
                fr = int(r[2])
                to2 = int(r[3])
                row['fr'] = int(r[2])
                row['to2'] = int(r[3])
                row['fromstation'] = r[4]
                row['tostation'] = r[5]
                row['pricediff'] = r[6]
                row['tradeable'] = format_long(long(r[7]))
                row['profit'] = format_price(float(r[8]))
                row['profit_size'] = float(r[9])
                from_set.add(fr)






                row['tprice'] = format_price(float(r[11]))
                row['fprice'] = format_price(float(r[12]))
                row['tvol'] = format_long(long(r[13]))
                row['fvol'] = format_long(long(r[14]))
                trades.append(row)

                # Add to from_to_map - map source
                if fr in from_to_map:
                    from_to_map[fr].add(row['to2'])
                else:
                    from_to_map[fr] = __builtin__.set()
                    from_to_map[fr].add(row['to2'])

                if fr in from_map:
                    from_map[fr].append(row)
                else:
                    from_map[fr] = []
                    from_map[fr].append(row)

                r = cur.fetchone()


            # Now we try to compute distance



            for compsys in from_set:

                distance_map = {}
                distance = 0


                time_net_ = time.time()

                distance_map = mapserver.route_comp(compsys, list(from_to_map[compsys]))

                time_net += time.time() - time_net_

                for row in from_map[compsys]:
                    if row['fr'] == compsys:

                        if compsys == row['to2']:
                            distance = 0
                        else:
                            distance = distance_map[row['to2']]

                        ps = row['profit_size']


                        row['profit_size'] = format_price(ps)

                        if sort == "jprofit":
                            row['sortby'] = ps/(max(0,distance)+1)
                            t.sort_nice = "Profit per jump"
                        elif sort == "sprofit":
                            row['sortby'] = ps
                            t.sort_nice = "Profit per trip"
                        elif sort == "profit":
                            row['sortby'] = row['profit']
                            t.sort_nice = "Profit"
                        elif sort == "jumps":
                            t.sort_nice = "Distance"
                            row['sortby'] = distance
                            row.reverse= True

                        row['profit_jumps'] = format_price(float(ps/(1+max(0,distance))))
                        row['distance'] = distance


            time_sort = time.time()
            trades.sort(reverse=True)
            time_sort = time.time() - time_sort

            #session['trade_results'] = trades

        else:
            trades = session['trade_results']
            fromcache = 1

        trade_num = len(trades)
        trades = trades[int(startat):int(startat)+int(limit)]

        t.fromcache = fromcache
        t.num_trades = trade_num
        t.trades = trades
        t.fromt = fromt
        t.to = to
        t.qtype = qtype
        t.age = age
        t.minprofit = minprofit
        t.size = size
        t.startat = startat
        t.limit = limit
        t.sort = sort
        t.prefer_sec = prefer_sec
        t.fromname = cur_f.fetchone()[1]
        t.toname = cur_t.fetchone()[1]

        t.page = int((int(startat) + 1) / limit) + 1
        t.next = int(startat) +limit

        t.prev = max(0,int(startat) - limit)

        db.close()

        session.save()

        return t.respond() +"Pathfinder time: %0.3fs, Sort time: %0.3fs, recompute set was %d elements long<br>" % (time_net, time_sort, len(from_set))

    tradefind_display_html = tradefind_display


    @cherrypy.expose
    def leaders(self):

        session = EVCstate()
        t = display.template('leaders.tmpl', session)
        db = evec_func.db_con()

        cur = db.cursor()

        cur.execute("SELECT username,uploads,userid FROM users WHERE userid != 0 AND uploads > 0 ORDER BY uploads DESC LIMIT 100")
        r = cur.fetchall()

        r_d = []

        t.results = r

        db.close()
        return t.respond()


    leaders_html = leaders
    @cherrypy.expose
    def tradefind_search(self, qtype, fromt, to):

        session = EVCstate()
        t = display.template('tradefind_search.tmpl', session)
        db = evec_func.db_con()

        cur_f = db.cursor()
        cur_t = db.cursor()

        t.qtype = qtype
        t.fromt = fromt
        t.to = to

        fromt = "%" + fromt + "%"
        to = "%" + to + "%"

        if qtype == "Systems":
            cur_f.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemname ILIKE %s ORDER BY systemname", [fromt])
            cur_t.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemname ILIKE %s ORDER BY systemname", [to])
        elif qtype == "Regions":
            cur_f.execute("SELECT regionid,regionname FROM regions WHERE regionname ILIKE %s ORDER BY regionname", [fromt])
            cur_t.execute("SELECT regionid,regionname FROM regions WHERE regionname ILIKE %s ORDER BY regionname", [to])
        elif qtype == "SystemToRegion":
            cur_f.execute("SELECT systemid,(systemname || ' / ')  || regionname FROM systems,regions WHERE systems.regionid = regions.regionid AND systemname ILIKE %s ORDER BY systemname", [fromt])
            cur_t.execute("SELECT regionid,regionname FROM regions WHERE regionname ILIKE %s ORDER BY regionname", [to])


        from_list = []
        to_list = []

        for (q,list) in [(cur_f,from_list), (cur_t,to_list)]:
            r = q.fetchone()
            while r:
                d = {}
                d['id'] = r[0]
                d['name'] = r[1]
                list.append(d)
                r = q.fetchone()

        t.from_list = from_list
        t.to_list = to_list


        db.close()

        session.save()

        return t.respond()

    tradefind_search_html = tradefind_search

    @cherrypy.expose
    def tradefind(self):
        session = EVCstate()


        t = display.template('tradefind.tmpl', session)
        return t.respond()

    tradefind_html = tradefind

    @cherrypy.expose
    def market(self, regionlimit=None, pickregion = None, empire = ""):
        session = EVCstate()

        db = evec_func.db_con()

        t = display.template('market.tmpl', session)


        if regionlimit is None:
            regionlimit = []

        if not isinstance(regionlimit, list):
            regionlimit = [regionlimit]

        regionlimit = map(int, regionlimit)

        empireregions = [10000001, 10000002, 10000016, 10000020, 10000028, 10000030, 10000032, 10000033, 10000043, 10000049, 10000037, 10000038, 10000036, 10000052, 10000064, 10000065, 10000067, 10000068, 10000054, 10000042,10000044, 10000048 ]


        if pickregion is not None:
            if empire:
                regionlimit = empireregions
            session['regionlimit'] = regionlimit
        else:
            if 'regionlimit' in session:
                regionlimit = session['regionlimit']
            else:
                regionlimit = []
                session['regionlimit'] = []

        t.regionlimit = regionlimit



        t.regions = evec_func.region_list(db)
        #t.types = evec_func.type_list(db)
        t.types = []


        db.close()
        return t.respond()

    market_html = market


    @cherrypy.expose
    def setigb(self,isigb="0"):
        session = EVCstate()

        if isigb == "1":
            session['isigb'] = True
        else:
            session['isigb'] = False
        session.save()


        emit_redirect('/home/')

    setigb_html = setigb

    @cherrypy.expose
    def software(self):
        session = EVCstate()

        t = display.template('software.tmpl', session)
        return t.respond()


    software_html = software

    @cherrypy.expose
    def develop_old(self):
        session = EVCstate()

        t = display.template('develop_old.tmpl', session)
        return t.respond()

    develop_old_html = develop_old


    @cherrypy.expose
    def develop(self):
        session = EVCstate()

        t = display.template('develop.tmpl', session)
        return t.respond()

    develop_html = develop

    @cherrypy.expose
    def reports(self):
        session = EVCstate()

        t = display.template('reports.tmpl', session)

        return t.respond()

    reports_html = reports

    @cherrypy.expose
    def index(self):
        session = EVCstate()

        t = display.template('index.tmpl', session)

        a = t.respond()
        return a

    @cherrypy.expose
    def test(self):
        return "Test is tested"


    index_html = index
